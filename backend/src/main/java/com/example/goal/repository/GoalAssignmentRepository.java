package com.example.goal.repository;

import com.example.goal.entity.Goal;
import com.example.goal.entity.GoalAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface GoalAssignmentRepository extends JpaRepository<GoalAssignment, Long> {

    List<GoalAssignment> findByGoal(Goal goal);

    /** 批量版 findByGoal，理由同 GoalAssigneeRepository.findByGoalIdIn */
    @Query("SELECT ga FROM GoalAssignment ga WHERE ga.goal.id IN :goalIds")
    List<GoalAssignment> findByGoalIdIn(@Param("goalIds") Collection<Long> goalIds);

    void deleteByGoal(Goal goal);
}