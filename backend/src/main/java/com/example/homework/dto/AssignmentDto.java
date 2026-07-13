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
        private Long classGroupId;
        private LocalDateTime dueTime;
        private List<Long> questionIds;
    }

    @Data
    public static class Response {
        private Long id;
        private String title;
        private String description;
        private Long classGroupId;
        private String classGroupName;
        private String teacherName;
        private LocalDateTime dueTime;
        private String status;
        private int questionCount;
        private LocalDateTime createdAt;
        /** 学生个人已答题数（仅学生视角返回） */
        private int answeredCount;
        /** 作业总题数（与学生视角的 answeredCount 对应） */
        private int totalCount;

        public static Response from(Assignment a) {
            Response r = new Response();
            r.id = a.getId();
            r.title = a.getTitle();
            r.description = a.getDescription();
            if (a.getClassGroup() != null) {
                r.classGroupId = a.getClassGroup().getId();
                r.classGroupName = a.getClassGroup().getName();
            }
            r.teacherName = a.getTeacher() != null ? a.getTeacher().getRealName() : null;
            r.dueTime = a.getDueTime();
            r.status = a.getStatus().name();
            r.questionCount = a.getQuestions().size();
            r.totalCount = a.getQuestions().size();
            r.createdAt = a.getCreatedAt();
            return r;
        }
    }
}