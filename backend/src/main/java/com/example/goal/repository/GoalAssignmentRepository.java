package com.example.goal.repository;

import com.example.goal.entity.Goal;
import com.example.goal.entity.GoalAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GoalAssignmentRepository extends JpaRepository<GoalAssignment, Long> {

    List<GoalAssignment> findByGoal(Goal goal);

    void deleteByGoal(Goal goal);
}