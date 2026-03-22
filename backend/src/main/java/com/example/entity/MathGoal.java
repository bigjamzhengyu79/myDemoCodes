package com.example.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "math_goals")
public class MathGoal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(nullable = false)
    private Integer progress = 0;

    @Column(nullable = false, length = 20)
    private String status = "进行中"; // 进行中, 已完成, 已放弃

    @ElementCollection
    @CollectionTable(name = "math_goal_subgoals", joinColumns = @JoinColumn(name = "math_goal_id"))
    @Column(name = "subgoal", length = 500)
    private java.util.List<String> subGoals = new java.util.ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
