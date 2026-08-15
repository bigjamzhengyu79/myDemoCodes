package com.example.homework.entity;

import com.example.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 错题本条目：学生手动收藏的题目 + 自己的订正笔记。
 *
 * 【重要】本表【完全由学生手动勾选】产生，系统不会自动收录任何题目。
 * 需求原话："说是错题，实际上是做过习题的保存，只要学生勾选，就能把目标习题移入错题本"。
 * 也就是说做对的题、蒙对的题同样可以收藏 —— 与"错题本"这个名字的直觉含义相反，
 * 改动前请先读这段注释，不要"顺手"加上按 score 自动收录的逻辑。
 *
 * 学生的作答情况（得分、错误类型、教师反馈）不在这里存，
 * 查询时从 student_answers 实时关联 —— 教师重新批改会改分，快照会立刻与成绩单不一致。
 *
 * 唯一键是 (student_id, question_id) 而非 answer_id：
 * 同一道题可能出现在多份作业里，但学生对它的收藏意图只有一份。
 * 侧表模式参照 goal 模块的 StudentGoalProgress。
 */
@Data
@Entity
@Table(name = "mistake_notes",
       uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "question_id"}))
public class MistakeNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    /**
     * 从哪份作业收藏进来的，仅用于列表显示来源。
     * 刻意用普通 Long 而非 @ManyToOne：收藏本只需要作业标题，
     * 加关联对象只会多一个 LAZY 代理。同 GoalAssignment.assignmentId 的处理。
     */
    @Column(name = "source_assignment_id")
    private Long sourceAssignmentId;

    /** 学生自己写的订正/反思，支持 LaTeX（与答题内容同一套渲染） */
    @Column(name = "note_content", columnDefinition = "TEXT")
    private String noteContent;

    /** 订正配图，base64 data URL 的 JSON 数组，与 StudentAnswer.imageUrlsJson 同构 */
    @Column(name = "image_urls_json", columnDefinition = "MEDIUMTEXT")
    private String imageUrlsJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Mastery mastery = Mastery.UNREVIEWED;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /** 掌握状态，由学生自己标记 */
    public enum Mastery { UNREVIEWED, REVIEWING, MASTERED }
}
