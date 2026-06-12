package com.example.goal.controller;

import com.example.goal.dto.GoalDto.GoalRequest;
import com.example.goal.dto.GoalDto.GoalResponse;
import com.example.goal.dto.GoalDto.GoalStatsResponse;
import com.example.goal.service.GoalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GoalController {

    private final GoalService goalService;

    @GetMapping
    public ResponseEntity<List<GoalResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(goalService.listParentGoals(status, keyword));
    }

    /**
     * 按需加载子目标
     */
    @GetMapping("/sub-goals")
    public ResponseEntity<List<GoalResponse>> loadSubGoals(
            @RequestParam Long parentId,
            @RequestParam(required = false) Integer depth) {
        return ResponseEntity.ok(goalService.loadSubGoals(parentId, depth));
    }

    @GetMapping("/stats")
    public ResponseEntity<GoalStatsResponse> stats() {
        return ResponseEntity.ok(goalService.getStats());
    }

    @GetMapping("/{id}")
    public ResponseEntity<GoalResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(goalService.getGoal(id));
    }

    @PostMapping
    public ResponseEntity<GoalResponse> create(@RequestBody GoalRequest req) {
        return ResponseEntity.ok(goalService.createGoal(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GoalResponse> update(
            @PathVariable Long id, @RequestBody GoalRequest req) {
        return ResponseEntity.ok(goalService.updateGoal(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        goalService.deleteGoal(id);
        return ResponseEntity.noContent().build();
    }

    // ====== 多对多学生分配 ======

    /**
     * 获取目标已分配的学生 ID 列表
     */
    @GetMapping("/{id}/assignees")
    public ResponseEntity<List<Long>> getAssignees(@PathVariable Long id) {
        return ResponseEntity.ok(goalService.listAssignedStudentIds(id));
    }

    /**
     * 单个追加分配:body: { studentId: 12 }
     */
    @PostMapping("/{id}/assignees")
    public ResponseEntity<Void> addAssignee(
            @PathVariable Long id,
            @RequestBody Map<String, Long> body) {
        Long studentId = body.get("studentId");
        if (studentId == null) {
            return ResponseEntity.badRequest().build();
        }
        goalService.assignStudent(id, studentId);
        return ResponseEntity.ok().build();
    }

    /**
     * 取消单个学生分配
     */
    @DeleteMapping("/{id}/assignees/{studentId}")
    public ResponseEntity<Void> removeAssignee(
            @PathVariable Long id,
            @PathVariable Long studentId) {
        goalService.unassignStudent(id, studentId);
        return ResponseEntity.noContent().build();
    }
}
