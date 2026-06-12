package com.example.goal.dto;

import com.example.goal.entity.GoalStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class GoalDto {

    @Data
    public static class GoalRequest {
        private String title;
        private String description;
        private GoalStatus status;
        private LocalDate plannedStart;
        private LocalDate plannedEnd;
        private LocalDate actualStart;
        private LocalDate actualEnd;
        private Integer progress;
        private String owners;
        private Long parentId;
        private Long assigneeId;
        /**
         * 分配的学生 ID 列表(多对多)。
         * null 表示不修改现有分配;空集合表示清空;非空表示替换为该集合。
         */
        private List<Long> assigneeIds;
    }

    @Data
    public static class GoalResponse {
        private Long id;
        private String title;
        private String description;
        private GoalStatus status;
        private LocalDate plannedStart;
        private LocalDate plannedEnd;
        private LocalDate actualStart;
        private LocalDate actualEnd;
        private Integer progress;
        private String owners;
        private Long parentId;
        private List<GoalResponse> subGoals;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private Long managerId;
        private String managerName;
        private Long assigneeId;
        private String assigneeName;
        /** 被分配的学生 ID 列表(多对多)。 */
        private List<Long> assigneeIds;
        /** 被分配的学生姓名列表(展示用)。 */
        private List<String> assigneeNames;
    }

    @Data
    public static class GoalStatsResponse {
        private long totalParent;
        private long totalSub;
        private long done;
        private long late;
        private int avgProgress;
    }
}
