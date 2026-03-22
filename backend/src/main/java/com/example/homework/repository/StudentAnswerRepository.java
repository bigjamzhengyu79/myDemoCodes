package com.example.homework.repository;

import com.example.homework.entity.StudentAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
}
