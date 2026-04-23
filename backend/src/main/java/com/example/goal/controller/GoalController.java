package com.example.goal.controller;

import com.example.goal.dto.GoalDto.GoalRequest;
import com.example.goal.dto.GoalDto.GoalResponse;
import com.example.goal.dto.GoalDto.GoalStatsResponse;
import com.example.goal.service.GoalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}
