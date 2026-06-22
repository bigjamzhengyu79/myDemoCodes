package com.example.goal.entity;

import com.example.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 目标评论。
 * 支持两种类型：
 * - PUBLIC（公开）：该目标所有参与者可见
 * - PRIVATE_TO_STUDENT（私密）：仅老师 + 特定学生可见
 * 作者可以是老师（创建自己的目标）或学生（被分配的目标）。
 */
@Data
@Entity
@Table(name = "goal_comments")
public class GoalComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_id", nullable = false)
    private Goal goal;

    /**
     * 评论所属学生（仅学生发表的评论非空，老师评论时此字段为空）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private User student;

    /**
     * 评论作者（老师或学生）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    /**
     * 作者角色：TEACHER / STUDENT
     */
    @Column(nullable = false, length = 10)
    private String authorRole;

    /**
     * 可见性：PUBLIC（公开）/ PRIVATE_TO_STUDENT（仅指定学生可见）
     */
    @Column(nullable = false, length = 20)
    private String visibility = "PUBLIC";

    /**
     * 私密评论的目标学生 ID（仅 PRIVATE_TO_STUDENT 时非空）
     */
    @Column(name = "target_student_id")
    private Long targetStudentId;

    /** 评论内容，支持 LaTeX 富文本 */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /** 图片 URL 列表，JSON 数组格式，如 ["/uploads/img1.jpg","/uploads/img2.jpg"] */
    @Column(columnDefinition = "TEXT")
    private String imageUrls;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}