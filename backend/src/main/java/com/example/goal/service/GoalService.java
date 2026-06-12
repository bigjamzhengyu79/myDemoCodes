package com.example.goal.service;

import com.example.entity.User;
import com.example.goal.dto.GoalDto.GoalRequest;
import com.example.goal.dto.GoalDto.GoalResponse;
import com.example.goal.dto.GoalDto.GoalStatsResponse;
import com.example.goal.entity.Goal;
import com.example.goal.entity.GoalAssignee;
import com.example.goal.entity.GoalStatus;
import com.example.goal.repository.GoalAssigneeRepository;
import com.example.goal.repository.GoalRepository;
import com.example.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GoalService {

    private final GoalRepository goalRepository;
    private final GoalAssigneeRepository goalAssigneeRepository;
    private final UserRepository userRepository;

    public List<GoalResponse> listParentGoals(String status, String keyword) {
        List<Goal> goals;
        if (keyword != null && !keyword.isBlank()) {
            goals = goalRepository.searchParents(keyword.trim());
        } else if (status != null && !status.isBlank()) {
            goals = goalRepository.findByParentIsNullAndStatusOrderByCreatedAtDesc(
                    GoalStatus.valueOf(status.toUpperCase()));
        } else {
            goals = goalRepository.findByParentIsNullOrderByCreatedAtDesc();
        }
        return goals.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * 按需加载子目标，支持指定深度
     */
    public List<GoalResponse> loadSubGoals(Long parentId, Integer depth) {
        if (depth == null || depth <= 1) {
            List<Goal> subs = goalRepository.findByParentIdOrderByPlannedStartAsc(parentId);
            return subs.stream().map(this::toResponse).collect(Collectors.toList());
        } else {
            List<Goal> descendants = goalRepository.findDescendantsWithinDepth(parentId, depth);
            return descendants.stream().map(this::toResponse).collect(Collectors.toList());
        }
    }

    public GoalResponse getGoal(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public GoalResponse createGoal(GoalRequest req) {
        Goal goal = new Goal();
        applyRequest(goal, req);
        if (goal.getParent() != null) {
            goal.setDepth(goal.getParent().getDepth() + 1);
        } else {
            goal.setDepth(1);
        }
        Goal saved = goalRepository.save(goal);
        // 处理多对多学生分配
        if (req.getAssigneeIds() != null) {
            replaceAssignees(saved, req.getAssigneeIds());
        }
        return toResponse(saved);
    }

    @Transactional
    public GoalResponse updateGoal(Long id, GoalRequest req) {
        Goal goal = findOrThrow(id);
        applyRequest(goal, req);
        if (goal.getParent() != null) {
            goal.setDepth(goal.getParent().getDepth() + 1);
        } else {
            goal.setDepth(1);
        }
        if (!goal.getSubGoals().isEmpty()) {
            recalcFromSubs(goal);
        }
        Goal saved = goalRepository.save(goal);
        // 处理多对多学生分配(仅当请求中显式给了 assigneeIds 字段)
        if (req.getAssigneeIds() != null) {
            replaceAssignees(saved, req.getAssigneeIds());
        }
        return toResponse(saved);
    }

    @Transactional
    public void deleteGoal(Long id) {
        goalRepository.delete(findOrThrow(id));
    }

    public GoalStatsResponse getStats() {
        List<Goal> parents = goalRepository.findByParentIsNullOrderByCreatedAtDesc();
        GoalStatsResponse stats = new GoalStatsResponse();
        stats.setTotalParent(parents.size());
        stats.setTotalSub(parents.stream().mapToLong(g -> g.getSubGoals().size()).sum());
        stats.setDone(parents.stream().filter(g -> calcStatus(g) == GoalStatus.DONE).count());
        stats.setLate(parents.stream().filter(g -> calcStatus(g) == GoalStatus.LATE).count());
        stats.setAvgProgress(parents.isEmpty() ? 0 :
                (int) Math.round(parents.stream().mapToInt(this::calcProgress).average().orElse(0)));
        return stats;
    }

    // ====== 多对多学生分配 ======

    /**
     * 用传入的学生 ID 集合完整替换目标的分配关系。
     * null/空表示清空(若 null 则保留原状;调用方控制)。
     */
    private void replaceAssignees(Goal goal, List<Long> studentIds) {
        // 先清空旧的
        List<GoalAssignee> existing = goalAssigneeRepository.findByGoal(goal);
        if (!existing.isEmpty()) {
            goalAssigneeRepository.deleteAll(existing);
            goalAssigneeRepository.flush();
        }
        if (studentIds == null || studentIds.isEmpty()) {
            goal.getAssignees().clear();
            return;
        }
        // 去重,避免重复插入
        Set<Long> dedup = new HashSet<>(studentIds);
        for (Long sid : dedup) {
            User student = userRepository.findById(sid)
                    .orElseThrow(() -> new RuntimeException("学生不存在: " + sid));
            GoalAssignee ga = new GoalAssignee();
            ga.setGoal(goal);
            ga.setStudent(student);
            goalAssigneeRepository.save(ga);
            goal.getAssignees().add(ga);
        }
    }

    /**
     * 单个追加分配(供 controller / 后续扩展使用)。
     */
    @Transactional
    public void assignStudent(Long goalId, Long studentId) {
        Goal goal = findOrThrow(goalId);
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("学生不存在: " + studentId));
        if (goalAssigneeRepository.existsByGoalAndStudent(goal, student)) {
            return;
        }
        GoalAssignee ga = new GoalAssignee();
        ga.setGoal(goal);
        ga.setStudent(student);
        goalAssigneeRepository.save(ga);
        goal.getAssignees().add(ga);
    }

    /**
     * 取消单个分配。
     */
    @Transactional
    public void unassignStudent(Long goalId, Long studentId) {
        Goal goal = findOrThrow(goalId);
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("学生不存在: " + studentId));
        goalAssigneeRepository.deleteByGoalAndStudent(goal, student);
        goal.getAssignees().removeIf(a -> a.getStudent().getId().equals(studentId));
    }

    public List<Long> listAssignedStudentIds(Long goalId) {
        Goal goal = findOrThrow(goalId);
        return goalAssigneeRepository.findByGoal(goal).stream()
                .map(a -> a.getStudent().getId())
                .collect(Collectors.toList());
    }

    // ====== 内部 ======

    private void applyRequest(Goal goal, GoalRequest req) {
        goal.setTitle(req.getTitle());
        goal.setDescription(req.getDescription());
        goal.setStatus(req.getStatus() != null ? req.getStatus() : GoalStatus.TODO);
        goal.setPlannedStart(req.getPlannedStart());
        goal.setPlannedEnd(req.getPlannedEnd());
        goal.setActualStart(req.getActualStart());
        goal.setActualEnd(req.getActualEnd());
        goal.setProgress(req.getProgress() != null ? req.getProgress() : 0);
        goal.setOwners(req.getOwners());
        if (req.getParentId() != null) {
            goal.setParent(findOrThrow(req.getParentId()));
        } else {
            goal.setParent(null);
        }
        // 单值 assignee(向后兼容)
        if (req.getAssigneeId() != null) {
            User assignee = userRepository.findById(req.getAssigneeId())
                    .orElseThrow(() -> new RuntimeException("用户不存在: " + req.getAssigneeId()));
            goal.setAssignee(assignee);
        }
    }

    private void recalcFromSubs(Goal parent) {
        List<Goal> subs = parent.getSubGoals();
        if (subs.isEmpty()) return;

        double[] weights = {1.0, 0.8, 0.6, 0.4};
        int maxDepth = 4;
        double total = 0;
        double weightSum = 0;
        for (Goal sub : subs) {
            int d = Math.min(sub.getDepth() - parent.getDepth(), maxDepth - 1);
            double w = weights[Math.max(0, d)];
            total += calcProgressRecursive(sub, 1, weights);
            weightSum += w;
        }
        int weightedProgress = weightSum > 0 ? (int) Math.round(total / weightSum) : 0;
        parent.setProgress(weightedProgress);

        GoalStatus derived;
        if (subs.stream().allMatch(s -> s.getStatus() == GoalStatus.DONE)) {
            derived = GoalStatus.DONE;
        } else if (subs.stream().anyMatch(s -> s.getStatus() == GoalStatus.LATE)) {
            derived = GoalStatus.LATE;
        } else if (subs.stream().anyMatch(s ->
                s.getStatus() == GoalStatus.IN_PROGRESS || s.getStatus() == GoalStatus.DONE)) {
            derived = GoalStatus.IN_PROGRESS;
        } else {
            derived = GoalStatus.TODO;
        }
        parent.setStatus(derived);
    }

    private double calcProgressRecursive(Goal goal, int depthLevel, double[] weights) {
        if (goal.getSubGoals().isEmpty() || depthLevel > weights.length) {
            return goal.getProgress() * weights[Math.min(depthLevel - 1, weights.length - 1)];
        }
        double total = 0;
        double weightSum = 0;
        for (Goal sub : goal.getSubGoals()) {
            total += calcProgressRecursive(sub, depthLevel + 1, weights);
            weightSum += weights[Math.min(depthLevel, weights.length - 1)];
        }
        return weightSum > 0 ? total / weightSum : 0;
    }

    private int calcProgress(Goal g) {
        double[] weights = {1.0, 0.8, 0.6, 0.4};
        return (int) Math.round(calcProgressRecursive(g, 1, weights));
    }

    private GoalStatus calcStatus(Goal g) {
        if (g.getSubGoals().isEmpty()) return g.getStatus();
        List<Goal> subs = g.getSubGoals();
        if (subs.stream().allMatch(s -> s.getStatus() == GoalStatus.DONE)) return GoalStatus.DONE;
        if (subs.stream().anyMatch(s -> s.getStatus() == GoalStatus.LATE)) return GoalStatus.LATE;
        if (subs.stream().anyMatch(s ->
                s.getStatus() == GoalStatus.IN_PROGRESS || s.getStatus() == GoalStatus.DONE))
            return GoalStatus.IN_PROGRESS;
        return GoalStatus.TODO;
    }

    private GoalResponse toResponse(Goal g) {
        GoalResponse r = new GoalResponse();
        r.setId(g.getId());
        r.setTitle(g.getTitle());
        r.setDescription(g.getDescription());
        r.setStatus(calcStatus(g));
        r.setPlannedStart(g.getPlannedStart());
        r.setPlannedEnd(g.getPlannedEnd());
        r.setActualStart(g.getActualStart());
        r.setActualEnd(g.getActualEnd());
        r.setProgress(calcProgress(g));
        r.setOwners(g.getOwners());
        r.setParentId(g.getParent() != null ? g.getParent().getId() : null);
        r.setCreatedAt(g.getCreatedAt());
        r.setUpdatedAt(g.getUpdatedAt());
        List<Goal> subGoals = g.getSubGoals();
        if (subGoals == null || subGoals.isEmpty()) {
            r.setSubGoals(Collections.emptyList());
        } else {
            r.setSubGoals(subGoals.stream().map(this::toResponse).collect(Collectors.toList()));
        }
        if (g.getManager() != null) {
            r.setManagerId(g.getManager().getId());
            r.setManagerName(g.getManager().getRealName());
        }
        if (g.getAssignee() != null) {
            r.setAssigneeId(g.getAssignee().getId());
            r.setAssigneeName(g.getAssignee().getRealName());
        }
        // 多对多学生分配
        List<GoalAssignee> assignees = goalAssigneeRepository.findByGoal(g);
        if (assignees != null && !assignees.isEmpty()) {
            r.setAssigneeIds(new ArrayList<>());
            r.setAssigneeNames(new ArrayList<>());
            for (GoalAssignee ga : assignees) {
                if (ga.getStudent() != null) {
                    r.getAssigneeIds().add(ga.getStudent().getId());
                    r.getAssigneeNames().add(ga.getStudent().getRealName());
                }
            }
        } else {
            r.setAssigneeIds(Collections.emptyList());
            r.setAssigneeNames(Collections.emptyList());
        }
        return r;
    }

    private Goal findOrThrow(Long id) {
        return goalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Goal not found: " + id));
    }
}
