package com.example.homework.dto;

import com.example.homework.entity.Assignment;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

public class AssignmentDto {

    @Data
    public static class Request {
        private String title;
        private String description;
        private String className;
        private LocalDateTime dueTime;
        private List<Long> questionIds;
    }

    @Data
    public static class Response {
        private Long id;
        private String title;
        private String description;
        private String className;
        private String teacherName;
        private LocalDateTime dueTime;
        private String status;
        private int questionCount;
        private LocalDateTime createdAt;

        public static Response from(Assignment a) {
            Response r = new Response();
            r.id = a.getId();
            r.title = a.getTitle();
            r.description = a.getDescription();
            r.className = a.getClassName();
            r.teacherName = a.getTeacher() != null ? a.getTeacher().getRealName() : null;
            r.dueTime = a.getDueTime();
            r.status = a.getStatus().name();
            r.questionCount = a.getQuestions().size();
            r.createdAt = a.getCreatedAt();
            return r;
        }
    }
}
