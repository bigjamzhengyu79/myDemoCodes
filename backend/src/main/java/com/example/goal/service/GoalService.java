package com.example.goal.service;

import com.example.goal.dto.GoalDto.GoalRequest;
import com.example.goal.dto.GoalDto.GoalResponse;
import com.example.goal.dto.GoalDto.GoalStatsResponse;
import com.example.goal.entity.Goal;
import com.example.goal.entity.GoalStatus;
import com.example.goal.repository.GoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GoalService {

    private final GoalRepository goalRepository;

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
            // 只加载直接子目标
            List<Goal> subs = goalRepository.findByParentIdOrderByPlannedStartAsc(parentId);
            return subs.stream().map(this::toResponse).collect(Collectors.toList());
        } else {
            // 加载指定深度的子孙目标
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
        // 设置层级深度
        if (goal.getParent() != null) {
            goal.setDepth(goal.getParent().getDepth() + 1);
        } else {
            goal.setDepth(1);
        }
        goalRepository.save(goal);
        return toResponse(goal);
    }

    @Transactional
    public GoalResponse updateGoal(Long id, GoalRequest req) {
        Goal goal = findOrThrow(id);
        applyRequest(goal, req);
        // 更新层级深度
        if (goal.getParent() != null) {
            goal.setDepth(goal.getParent().getDepth() + 1);
        } else {
            goal.setDepth(1);
        }
        if (!goal.getSubGoals().isEmpty()) {
            recalcFromSubs(goal);
        }
        goalRepository.save(goal);
        return toResponse(goal);
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
    }

    /**
     * 递归加权计算进度和状态，权重随层级递减（如1.0, 0.8, 0.6, 0.4）。
     */
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

    /**
     * 递归加权进度计算，depthLevel从1开始。
     */
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
        // 递归加权进度
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
        r.setSubGoals(g.getSubGoals().stream().map(this::toResponse).collect(Collectors.toList()));
        return r;
    }

    private Goal findOrThrow(Long id) {
        return goalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Goal not found: " + id));
    }
}
