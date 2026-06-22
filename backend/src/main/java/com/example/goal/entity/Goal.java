package com.example.goal.entity;

import com.example.entity.ClassGroup;
import com.example.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "goals")
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GoalStatus status = GoalStatus.TODO;

    private LocalDate plannedStart;
    private LocalDate plannedEnd;
    private LocalDate actualStart;
    private LocalDate actualEnd;

    @Column(nullable = false)
    private Integer progress = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Goal parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("plannedStart ASC")
    private List<Goal> subGoals = new ArrayList<>();

    /**
     * 被分配的学生(多对多)。
     * 区分于 assignee 字段(单一"负责人"语义),本字段表示"分给哪些学生"。
     */
    @OneToMany(mappedBy = "goal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GoalAssignee> assignees = new ArrayList<>();

    /**
     * 目标关联的作业（多对多）。
     */
    @OneToMany(mappedBy = "goal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GoalAssignment> goalAssignments = new ArrayList<>();

    /**
     * 目标评论（一对多）。
     */
    @OneToMany(mappedBy = "goal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GoalComment> goalComments = new ArrayList<>();

    /**
     * 目标关联的班级。设置后若未指定具体学生，则默认分配给班内所有学生。
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_group_id")
    private ClassGroup classGroup;

    @Column(length = 500)
    private String owners;

    /**
     * 目标设定者（老师）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;

    /**
     * 目标负责人（学生）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    /**
     * 层级深度，根目标为1，子目标依次+1，最大支持4层。
     */
    @Column(nullable = false)
    private Integer depth = 1;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}