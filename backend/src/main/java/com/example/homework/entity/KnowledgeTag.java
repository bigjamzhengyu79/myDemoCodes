package com.example.homework.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "knowledge_tags")
public class KnowledgeTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 64)
    private String chapter;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;
}
