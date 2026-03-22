package com.example.service;

import com.example.entity.MathGoal;
import com.example.repository.MathGoalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class MathGoalService {
    @Autowired
    private MathGoalRepository mathGoalRepository;

    public List<MathGoal> getAllGoals() {
        return mathGoalRepository.findAll();
    }

    public Optional<MathGoal> getGoalById(Long id) {
        return mathGoalRepository.findById(id);
    }

    public MathGoal createGoal(MathGoal goal) {
        return mathGoalRepository.save(goal);
    }

    public MathGoal updateGoal(Long id, MathGoal goal) {
        return mathGoalRepository.findById(id).map(existingGoal -> {
            existingGoal.setTitle(goal.getTitle());
            existingGoal.setDescription(goal.getDescription());
            existingGoal.setCategory(goal.getCategory());
            existingGoal.setTargetDate(goal.getTargetDate());
            existingGoal.setProgress(goal.getProgress());
            existingGoal.setStatus(goal.getStatus());
            existingGoal.setSubGoals(goal.getSubGoals() != null ? goal.getSubGoals() : new java.util.ArrayList<>());
            return mathGoalRepository.save(existingGoal);
        }).orElseThrow(() -> new RuntimeException("Goal not found"));
    }

    public void deleteGoal(Long id) {
        mathGoalRepository.deleteById(id);
    }

    public List<MathGoal> getGoalsByStatus(String status) {
        return mathGoalRepository.findByStatus(status);
    }

    public List<MathGoal> getGoalsByCategory(String category) {
        return mathGoalRepository.findByCategory(category);
    }
}
