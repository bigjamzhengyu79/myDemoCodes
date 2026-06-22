package com.example.goal.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 目标-作业关联。
 * 每个目标节点可以关联一个或多个已发布的作业。
 */
@Data
@Entity
@Table(name = "goal_assignments",
       uniqueConstraints = @UniqueConstraint(columnNames = {"goal_id", "assignment_id"}))
public class GoalAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_id", nullable = false)
    private Goal goal;

    @Column(name = "assignment_id", nullable = false)
    private Long assignmentId;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}