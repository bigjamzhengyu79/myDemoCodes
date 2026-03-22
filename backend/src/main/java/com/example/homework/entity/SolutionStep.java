package com.example.homework.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "solution_steps")
public class SolutionStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(name = "content_latex", nullable = false, columnDefinition = "TEXT")
    private String contentLatex;

    @Column(name = "step_score", nullable = false)
    private Integer stepScore = 0;

    @Column(name = "common_errors", columnDefinition = "TEXT")
    private String commonErrors;
}
