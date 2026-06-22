package com.example.goal.repository;

import com.example.goal.entity.Goal;
import com.example.goal.entity.StudentGoalProgress;
import com.example.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentGoalProgressRepository extends JpaRepository<StudentGoalProgress, Long> {

    Optional<StudentGoalProgress> findByGoalAndStudent(Goal goal, User student);

    List<StudentGoalProgress> findByStudent(User student);

    List<StudentGoalProgress> findByGoal(Goal goal);

    boolean existsByGoalAndStudent(Goal goal, User student);
}