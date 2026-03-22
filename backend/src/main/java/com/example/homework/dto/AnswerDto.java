package com.example.homework.dto;

import com.example.homework.entity.StudentAnswer;
import lombok.Data;
import java.time.LocalDateTime;

public class AnswerDto {

    @Data
    public static class SubmitRequest {
        private Long questionId;
        private String answerContent;
        private String imageUrl;
    }

    @Data
    public static class GradeRequest {
        private Long answerId;
        private Integer score;
        private String feedback;
        private String errorType;
    }

    @Data
    public static class Response {
        private Long id;
        private Long questionId;
        private String questionTitle;
        private String questionType;
        private Long studentId;
        private String studentName;
        private String answerContent;
        private String imageUrl;
        private Integer score;
        private Integer autoScore;
        private Integer totalScore;
        private String feedback;
        private String errorType;
        private String status;
        private LocalDateTime submittedAt;

        public static Response from(StudentAnswer a) {
            Response r = new Response();
            r.id = a.getId();
            r.questionId = a.getQuestion().getId();
            r.questionTitle = a.getQuestion().getTitle();
            r.questionType = a.getQuestion().getQuestionType().name();
            r.studentId = a.getStudent().getId();
            r.studentName = a.getStudent().getRealName();
            r.answerContent = a.getAnswerContent();
            r.imageUrl = a.getImageUrl();
            r.score = a.getScore();
            r.autoScore = a.getAutoScore();
            r.totalScore = a.getQuestion().getTotalScore();
            r.feedback = a.getFeedback();
            r.errorType = a.getErrorType() != null ? a.getErrorType().name() : null;
            r.status = a.getStatus().name();
            r.submittedAt = a.getSubmittedAt();
            return r;
        }
    }
}
