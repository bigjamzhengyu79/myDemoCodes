package com.example.homework.repository;

import com.example.homework.entity.StudentAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StudentAnswerRepository extends JpaRepository<StudentAnswer, Long> {
    List<StudentAnswer> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);
    List<StudentAnswer> findByAssignmentId(Long assignmentId);

    Optional<StudentAnswer> findByAssignmentIdAndQuestionIdAndStudentId(
            Long assignmentId, Long questionId, Long studentId);

    @Query("SELECT sa FROM StudentAnswer sa WHERE sa.status = 'SUBMITTED' ORDER BY sa.submittedAt ASC")
    List<StudentAnswer> findPendingReview();

    @Query("SELECT COUNT(sa) FROM StudentAnswer sa WHERE sa.assignment.id = :aId AND sa.student.id = :sId")
    long countByAssignmentAndStudent(@Param("aId") Long assignmentId, @Param("sId") Long studentId);

    List<StudentAnswer> findByStudentIdAndScoreIsNotNull(Long studentId);

    /**
     * 错题本用：批量取该学生对这批题目的作答记录。
     *
     * 同一道题可能在多份作业里都答过，会返回多条；已按 submittedAt 倒序，
     * Service 层按 questionId 分组取第一条（即最新的一条）。
     *
     * 不 fetch question/assignment：调用方已经持有题目对象，
     * 作业标题另行按 MistakeNote.sourceAssignmentId 批量解析。
     */
    @Query("""
            SELECT sa FROM StudentAnswer sa
            WHERE sa.student.id = :studentId AND sa.question.id IN :questionIds
            ORDER BY sa.submittedAt DESC
            """)
    List<StudentAnswer> findByStudentIdAndQuestionIdIn(@Param("studentId") Long studentId,
                                                       @Param("questionIds") Collection<Long> questionIds);
}
