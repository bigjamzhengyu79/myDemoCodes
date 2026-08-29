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

    /**
     * 所有被标记为「可复制」的父目标，不限创建者。
     *
     * 【注意：不要改回按 managerId 过滤】
     * 「可复制」的语义就是跨老师共享模板 —— 老师 A 标记后，老师 B 建目标时
     * 应该能选到它。历史实现复用了上面的 findByParentIsNullAndManagerId，
     * 先按创建者过滤再筛 copyable，导致别人的目标永远出不来（本方法即为修复该 bug）。
     *
     * 与之呼应的两处设计：toggleCopyable 限定只有创建者/管理员能改这个标记，
     * copyGoalTree 复制时会把新目标的 manager 换成复制者 —— 两者都只在跨老师
     * 共享的前提下才有意义。
     *
     * JOIN FETCH manager：下拉列表要显示原作者名（区分不同老师的同名目标），
     * 不预取的话每行都会多一次懒加载查询。
     */
    @Query("SELECT g FROM Goal g LEFT JOIN FETCH g.manager " +
           "WHERE g.parent IS NULL AND g.copyable = true ORDER BY g.createdAt DESC")
    List<Goal> findCopyableParents();

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
