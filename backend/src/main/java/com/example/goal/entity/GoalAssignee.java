package com.example.goal.entity;

import com.example.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 目标-学生多对多关联。
 * 区分于 GoalAssignment(目标-作业),本表记录"目标分给哪些学生"。
 */
@Data
@Entity
@Table(name = "goal_assignees", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"goal_id", "student_id"})
})
public class GoalAssignee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_id", nullable = false)
    private Goal goal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "assigned_at", updatable = false)
    private LocalDateTime assignedAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (assignedAt == null) assignedAt = LocalDateTime.now();
    }
}
