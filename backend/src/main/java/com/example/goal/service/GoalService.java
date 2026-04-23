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

    public GoalResponse getGoal(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public GoalResponse createGoal(GoalRequest req) {
        Goal goal = new Goal();
        applyRequest(goal, req);
        goalRepository.save(goal);
        return toResponse(goal);
    }

    @Transactional
    public GoalResponse updateGoal(Long id, GoalRequest req) {
        Goal goal = findOrThrow(id);
        applyRequest(goal, req);
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

    private void recalcFromSubs(Goal parent) {
        List<Goal> subs = parent.getSubGoals();
        if (subs.isEmpty()) return;

        int avgProgress = (int) Math.round(
                subs.stream().mapToInt(Goal::getProgress).average().orElse(0));
        parent.setProgress(avgProgress);

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

    private int calcProgress(Goal g) {
        if (g.getSubGoals().isEmpty()) return g.getProgress();
        return (int) Math.round(
                g.getSubGoals().stream().mapToInt(Goal::getProgress).average().orElse(0));
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
