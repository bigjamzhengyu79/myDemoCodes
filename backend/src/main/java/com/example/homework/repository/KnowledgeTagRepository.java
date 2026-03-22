package com.example.homework.repository;

import com.example.homework.entity.KnowledgeTag;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface KnowledgeTagRepository extends JpaRepository<KnowledgeTag, Long> {
    List<KnowledgeTag> findByParentIdIsNull();
    List<KnowledgeTag> findByParentId(Long parentId);
}
