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
        /** 是否可被复制为模板 */
        private Boolean copyable;
        /**
         * 分配的学生 ID 列表(多对多)。
         * null 表示不修改现有分配;空集合表示清空;非空表示替换为该集合。
         */
        private List<Long> assigneeIds;
        /** 目标关联的班级 ID */
        private Long classGroupId;
        /** 关联的作业 ID 列表 */
        private List<Long> assignmentIds;
        /**
         * 从已有目标复制时，指定源目标 ID。
         * 后端会递归复制源目标的子目标及其关联数据（学生分配、作业关联）。
         */
        private Long sourceGoalId;
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
        /** 目标关联的班级 ID */
        private Long classGroupId;
        /** 目标关联的班级名称 */
        private String classGroupName;
        /** 关联的作业 ID 列表 */
        private List<Long> assignmentIds;
        /** 关联的作业标题列表（展示用） */
        private List<String> assignmentTitles;
        /** 当前登录学生对此目标的个人进度（仅学生视角返回） */
        private Integer studentProgress;
        /** 当前登录学生对此目标的个人状态（仅学生视角返回） */
        private String studentStatus;
        /** 当前登录学生对此目标的个人实际开始时间（仅学生视角返回） */
        private LocalDate myActualStart;
        /** 当前登录学生对此目标的个人实际结束时间（仅学生视角返回） */
        private LocalDate myActualEnd;
        /** 当前用户对此目标的评论列表 */
        private List<CommentResponse> comments;
        /** 当前用户是否有权评论（分配给该目标的学生） */
        private boolean canComment;
        /** 层级深度，根目标为1，子目标依次+1 */
        private Integer depth;
        /** 是否可被复制为模板 */
        private boolean copyable;
    }

    @Data
    public static class StudentProgressUpdateRequest {
        private Integer progress;
        private String status;
        private LocalDate actualStart;
        private LocalDate actualEnd;
    }

    @Data
    public static class GoalStatsResponse {
        private long totalParent;
        private long totalSub;
        private long done;
        private long late;
        private int avgProgress;
    }

    // ====== 评论 DTO ======

    @Data
    public static class CommentRequest {
        private String content;
        /** 附件 URL 列表（从前端上传后获取的 URL），向后兼容纯字符串 URL */
        private List<String> imageUrls;
        /** 附件原始文件名列表（与 imageUrls 顺序对应） */
        private List<String> attachmentNames;
        /** 可见性：PUBLIC（公开）/ PRIVATE_TO_STUDENT（私密），默认 PUBLIC */
        private String visibility = "PUBLIC";
        /** 私密评论的目标学生 ID（PRIVATE_TO_STUDENT 时必填） */
        private Long targetStudentId;
    }

    @Data
    public static class CommentResponse {
        private Long id;
        private Long goalId;
        private Long studentId;
        private String studentName;
        /** 评论作者 ID */
        private Long authorId;
        /** 评论作者姓名 */
        private String authorName;
        /** 作者角色：TEACHER / STUDENT */
        private String authorRole;
        /** 可见性：PUBLIC / PRIVATE_TO_STUDENT */
        private String visibility;
        /** 私密评论的目标学生 ID */
        private Long targetStudentId;
        private String content;
        /** 附件列表，每个元素格式 "url::name" 或纯 url */
        private List<String> imageUrls;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        /** 是否是当前用户的评论（前端判断可删除/编辑） */
        private boolean isOwn;
    }

    // ====== 学生执行概览 DTO（老师视角） ======

    @Data
    public static class StudentProgressResponse {
        private Long studentId;
        private String studentName;
        private Integer progress;
        private String status;
        private LocalDate actualStart;
        private LocalDate actualEnd;
    }

    @Data
    public static class StudentCommentGroupResponse {
        private Long studentId;
        private String studentName;
        private List<CommentResponse> comments;
    }

    @Data
    public static class GoalStudentOverviewResponse {
        private Long goalId;
        private String goalTitle;
        /** 子目标概览（递归嵌套，老师可逐层展开） */
        private List<GoalStudentOverviewResponse> subGoals;
        /** 该目标下所有学生的进度 */
        private List<StudentProgressResponse> studentProgresses;
        /** 该目标下所有学生的评论（按学生分组） */
        private List<StudentCommentGroupResponse> studentComments;
    }
}