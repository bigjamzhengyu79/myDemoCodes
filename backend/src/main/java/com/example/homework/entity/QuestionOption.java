package com.example.homework.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "question_options")
public class QuestionOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "option_label", nullable = false, length = 1)
    private String optionLabel;

    @Column(name = "content_latex", nullable = false, columnDefinition = "TEXT")
    private String contentLatex;

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect = false;
}
