package com.example.controller;

import com.example.entity.MathGoal;
import com.example.service.MathGoalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/math-goals")
public class MathGoalController {

    @Autowired
    private MathGoalService mathGoalService;

    @GetMapping
    public ResponseEntity<List<MathGoal>> getAllGoals() {
        return ResponseEntity.ok(mathGoalService.getAllGoals());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MathGoal> getGoalById(@PathVariable Long id) {
        return mathGoalService.getGoalById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<MathGoal> createGoal(@RequestBody MathGoal goal) {
        MathGoal createdGoal = mathGoalService.createGoal(goal);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdGoal);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MathGoal> updateGoal(@PathVariable Long id, @RequestBody MathGoal goal) {
        try {
            MathGoal updatedGoal = mathGoalService.updateGoal(id, goal);
            return ResponseEntity.ok(updatedGoal);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteGoal(@PathVariable Long id) {
        mathGoalService.deleteGoal(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Goal deleted successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<MathGoal>> getGoalsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(mathGoalService.getGoalsByStatus(status));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<MathGoal>> getGoalsByCategory(@PathVariable String category) {
        return ResponseEntity.ok(mathGoalService.getGoalsByCategory(category));
    }
}
