package com.example.goal.repository;

import com.example.goal.entity.Goal;
import com.example.goal.entity.GoalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GoalRepository extends JpaRepository<Goal, Long> {

    List<Goal> findByParentIsNullOrderByCreatedAtDesc();

    List<Goal> findByParentIsNullAndManagerIdOrderByCreatedAtDesc(Long managerId);

    List<Goal> findByParentIdOrderByPlannedStartAsc(Long parentId);

    List<Goal> findByParentIsNullAndStatusOrderByCreatedAtDesc(GoalStatus status);

    @Query("SELECT g FROM Goal g WHERE g.parent IS NULL AND " +
           "(g.title LIKE %:kw% OR g.owners LIKE %:kw%)")
    List<Goal> searchParents(@Param("kw") String keyword);

    long countByParentIsNull();

    long countByParentIsNotNull();

    long countByParentIsNullAndStatus(GoalStatus status);

    // 查询某父目标下指定深度的子孙目标
    @Query("SELECT g FROM Goal g WHERE g.parent.id = :parentId AND g.depth <= :maxDepth")
    List<Goal> findDescendantsWithinDepth(@Param("parentId") Long parentId, @Param("maxDepth") int maxDepth);
}
