package com.example.homework.service;

import com.example.entity.User;
import com.example.homework.dto.AnswerDto;
import com.example.homework.entity.*;
import com.example.homework.repository.*;
import com.example.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnswerService {

    private final StudentAnswerRepository answerRepository;
    private final AssignmentRepository assignmentRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;

    @Transactional
    public AnswerDto.Response submit(Long assignmentId, AnswerDto.SubmitRequest req, String username) {
        User student = userRepository.findByUsername(username).orElseThrow();
        Assignment assignment = assignmentRepository.findById(assignmentId).orElseThrow();
        Question question = questionRepository.findById(req.getQuestionId()).orElseThrow();
        StudentAnswer answer = answerRepository.findByAssignmentIdAndQuestionIdAndStudentId(assignmentId, req.getQuestionId(), student.getId()).orElse(new StudentAnswer());
        answer.setAssignment(assignment);
        answer.setQuestion(question);
        answer.setStudent(student);
        answer.setAnswerContent(req.getAnswerContent());
        answer.setImageUrl(req.getImageUrl());
        answer.setSubmittedAt(LocalDateTime.now());
        autoGrade(answer, question, req.getAnswerContent());
        return AnswerDto.Response.from(answerRepository.save(answer));
    }

    private void autoGrade(StudentAnswer answer, Question question, String content) {
        if (content == null) return;

        if (question.getQuestionType() == Question.QuestionType.SINGLE_CHOICE) {
            boolean correct = content.trim().equalsIgnoreCase(question.getAnswerKey());
            answer.setAutoScore(correct ? question.getTotalScore() : 0);
            answer.setScore(answer.getAutoScore());
            answer.setStatus(StudentAnswer.Status.AUTO_GRADED);
            answer.setFeedback(correct ? "✓ 回答正确" : "✗ 回答错误，正确答案为：" + question.getAnswerKey());

        } else if (question.getQuestionType() == Question.QuestionType.FILL_BLANK) {
            String std = question.getAnswerKey() != null ? question.getAnswerKey().trim() : "";
            boolean correct = content.trim().equals(std);
            answer.setAutoScore(correct ? question.getTotalScore() : 0);
            answer.setScore(answer.getAutoScore());
            answer.setStatus(StudentAnswer.Status.AUTO_GRADED);
            answer.setFeedback(correct ? "✓ 回答正确" : "✗ 回答错误，参考答案：" + std);

        } else {
            // 解答题：AI建议分（简单规则：有内容给一半分，待教师复核）
            int suggested = content.length() > 20 ? question.getTotalScore() / 2 : 0;
            answer.setAutoScore(suggested);
            answer.setStatus(StudentAnswer.Status.SUBMITTED);
        }
    }

 @Transactional
    public AnswerDto.Response grade(AnswerDto.GradeRequest req, String reviewerUsername) {
        StudentAnswer answer = answerRepository.findById(req.getAnswerId())
                .orElseThrow(() -> new RuntimeException("答题记录不存在"));
        User reviewer = userRepository.findByUsername(reviewerUsername).orElseThrow();

        answer.setScore(req.getScore());
        answer.setFeedback(req.getFeedback());
        if (req.getErrorType() != null) {
            answer.setErrorType(StudentAnswer.ErrorType.valueOf(req.getErrorType()));
        }
        answer.setStatus(StudentAnswer.Status.REVIEWED);
        answer.setReviewedAt(LocalDateTime.now());
        answer.setReviewer(reviewer);

        return AnswerDto.Response.from(answerRepository.save(answer));
    }

    public List<AnswerDto.Response> getByAssignment(Long assignmentId) {
        return answerRepository.findByAssignmentId(assignmentId).stream()
                .map(AnswerDto.Response::from)
                .collect(Collectors.toList());
    }

    public List<AnswerDto.Response> getByAssignmentAndStudent(Long assignmentId, String username) {
        User student = userRepository.findByUsername(username).orElseThrow();
        return answerRepository.findByAssignmentIdAndStudentId(assignmentId, student.getId()).stream()
                .map(AnswerDto.Response::from)
                .collect(Collectors.toList());
    }

    public List<AnswerDto.Response> getPendingReview() {
        return answerRepository.findPendingReview().stream()
                .map(AnswerDto.Response::from)
                .collect(Collectors.toList());
    }

     // 统计：某学生在某作业的得分汇总
    public java.util.Map<String, Object> getStudentStats(Long assignmentId, String username) {
        User student = userRepository.findByUsername(username).orElseThrow();
        List<StudentAnswer> answers = answerRepository.findByAssignmentIdAndStudentId(assignmentId, student.getId());
        int total = answers.stream().mapToInt(a -> a.getQuestion().getTotalScore()).sum();
        int scored = answers.stream().filter(a -> a.getScore() != null)
                .mapToInt(StudentAnswer::getScore).sum();
        long graded = answers.stream().filter(a -> a.getScore() != null).count();
        return java.util.Map.of(
            "total", total, "scored", scored, "graded", graded,
            "count", answers.size()
        );
    }
}
