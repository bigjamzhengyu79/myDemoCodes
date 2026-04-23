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
