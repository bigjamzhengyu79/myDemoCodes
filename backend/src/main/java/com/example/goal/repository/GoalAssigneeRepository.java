package com.example.goal.repository;

import com.example.goal.entity.Goal;
import com.example.goal.entity.GoalAssignee;
import com.example.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GoalAssigneeRepository extends JpaRepository<GoalAssignee, Long> {

    List<GoalAssignee> findByGoal(Goal goal);

    /**
     * 批量版 findByGoal，供 GoalService.toResponse 的树形转换使用。
     *
     * 逐节点调用 findByGoal 会产生 N+1（目标树有多少节点就发多少条 SQL），
     * 是目标列表接口耗时 10～80 秒的主因之一。
     * JOIN FETCH student 一并把学生取回来，避免后续读 getStudent() 时再逐条懒加载。
     */
    @Query("SELECT ga FROM GoalAssignee ga JOIN FETCH ga.student WHERE ga.goal.id IN :goalIds")
    List<GoalAssignee> findByGoalIdIn(@Param("goalIds") Collection<Long> goalIds);

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
