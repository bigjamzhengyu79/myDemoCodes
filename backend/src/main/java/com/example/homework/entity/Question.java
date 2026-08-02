package com.example.homework.entity;

import com.example.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255)
    private String title;

    @Column(name = "content_latex", nullable = false, columnDefinition = "TEXT")
    private String contentLatex;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false)
    private QuestionType questionType;

    @Column(nullable = false)
    private Integer difficulty = 3;

    @Column(name = "total_score", nullable = false)
    private Integer totalScore = 5;

    @Column(name = "answer_key", columnDefinition = "TEXT")
    private String answerKey;

    @Column(length = 128)
    private String source;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "image_urls_json", columnDefinition = "MEDIUMTEXT")
    private String imageUrlsJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    /**
     * 可见性：PUBLIC 全体教师可用 / SHARED 作成者 + sharedWith 列出的教师 / PRIVATE 仅作成者。
     * 注意：只约束「题库列表、选择器发现、打开详情」这三处，
     *      已写入 assignment_questions 的题目不受后续取消共享影响 —— 作业照常显示、打印、批改。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 16)
    private Visibility visibility = Visibility.PUBLIC;

    /** visibility=SHARED 时被授权使用（只读，不可编辑删除）的教师集合 */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "question_shares",
        joinColumns = @JoinColumn(name = "question_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> sharedWith = new HashSet<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("stepOrder ASC")
    private List<SolutionStep> solutionSteps = new ArrayList<>();

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("optionLabel ASC")
    private List<QuestionOption> options = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "question_knowledge_tags",
        joinColumns = @JoinColumn(name = "question_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<KnowledgeTag> knowledgeTags = new HashSet<>();

    public enum QuestionType { SINGLE_CHOICE, FILL_BLANK, OPEN_ENDED }

    public enum Visibility { PUBLIC, SHARED, PRIVATE }
}
