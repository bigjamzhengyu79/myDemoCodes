package com.example.goal.repository;

import com.example.goal.entity.Goal;
import com.example.goal.entity.GoalComment;
import com.example.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface GoalCommentRepository extends JpaRepository<GoalComment, Long> {

    /** 获取某个目标的所有公开评论（按时间升序） */
    List<GoalComment> findByGoalAndVisibilityOrderByCreatedAtAsc(Goal goal, String visibility);

    /**
     * 批量版：一次取回多个目标的公开评论，理由同 GoalAssigneeRepository.findByGoalIdIn。
     * JOIN FETCH author/student 是因为 toCommentResponse 会读它们的 realName，
     * 否则每条评论都会再触发一次懒加载查询。
     */
    @Query("SELECT c FROM GoalComment c " +
           "JOIN FETCH c.author LEFT JOIN FETCH c.student " +
           "WHERE c.goal.id IN :goalIds AND c.visibility = :visibility " +
           "ORDER BY c.createdAt ASC")
    List<GoalComment> findByGoalIdInAndVisibility(@Param("goalIds") Collection<Long> goalIds,
                                                  @Param("visibility") String visibility);

    /** 获取某个学生对自己某个目标的所有公开评论 */
    List<GoalComment> findByGoalAndStudentAndVisibilityOrderByCreatedAtAsc(Goal goal, User student, String visibility);

    /** 获取某个目标下，指定学生可见的私密评论（包括老师发给该学生的 + 该学生自己回复的私密评论） */
    @Query("SELECT c FROM GoalComment c WHERE c.goal = :goal AND c.visibility = 'PRIVATE_TO_STUDENT' " +
           "AND (c.targetStudentId = :studentId OR c.author.id = :studentId)")
    List<GoalComment> findPrivateCommentsByGoalAndStudent(@Param("goal") Goal goal, @Param("studentId") Long studentId);

    /** 获取某个目标的所有私密评论（老师查看自己发出的私密评论） */
    @Query("SELECT c FROM GoalComment c WHERE c.goal = :goal AND c.visibility = 'PRIVATE_TO_STUDENT' " +
           "AND (c.author.id = :teacherId OR c.targetStudentId IN :studentIds)")
    List<GoalComment> findPrivateCommentsByGoalAndTeacher(@Param("goal") Goal goal, 
                                                           @Param("teacherId") Long teacherId,
                                                           @Param("studentIds") List<Long> studentIds);

    /** 获取某个目标的所有评论（老师用，全部可见范围） */
    List<GoalComment> findByGoalOrderByCreatedAtAsc(Goal goal);

    /** 删除评论（校验评论作者） */
    void deleteByIdAndAuthorId(Long id, Long authorId);
}
