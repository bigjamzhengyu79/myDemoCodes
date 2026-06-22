package com.example.goal.entity;

import com.example.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学生个人目标进度。
 * 每个学生对自己被分配的目标维护独立的进度、状态和实际时间，
 * 互不影响。
 */
@Data
@Entity
@Table(name = "student_goal_progress",
       uniqueConstraints = @UniqueConstraint(columnNames = {"goal_id", "student_id"}))
public class StudentGoalProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_id", nullable = false)
    private Goal goal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    /** 学生个人进度 0-100 */
    @Column(nullable = false)
    private Integer progress = 0;

    /** 学生个人状态 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GoalStatus status = GoalStatus.TODO;

    /** 学生个人实际开始时间 */
    private LocalDate actualStart;

    /** 学生个人实际结束时间 */
    private LocalDate actualEnd;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}