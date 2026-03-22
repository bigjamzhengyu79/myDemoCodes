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

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("stepOrder ASC")
    private List<SolutionStep> solutionSteps = new ArrayList<>();

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
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
}
