package com.example.goal.repository;

import com.example.goal.entity.Goal;
import com.example.goal.entity.GoalAssignee;
import com.example.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface GoalAssigneeRepository extends JpaRepository<GoalAssignee, Long> {

    List<GoalAssignee> findByGoal(Goal goal);

    List<GoalAssignee> findByStudent(User student);

    Optional<GoalAssignee> findByGoalAndStudent(Goal goal, User student);

    boolean existsByGoalAndStudent(Goal goal, User student);

    @Modifying
    @Transactional
    @Query("DELETE FROM GoalAssignee ga WHERE ga.goal = :goal")
    void deleteByGoal(@Param("goal") Goal goal);

    @Modifying
    @Transactional
    @Query("DELETE FROM GoalAssignee ga WHERE ga.goal = :goal AND ga.student = :student")
    void deleteByGoalAndStudent(@Param("goal") Goal goal, @Param("student") User student);
}
