package com.example.homework.repository;

import com.example.homework.entity.Question;
import com.example.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByCreatedBy(User user);

    @Query("SELECT q FROM Question q JOIN q.knowledgeTags t WHERE t.id = :tagId")
    List<Question> findByKnowledgeTagId(@Param("tagId") Long tagId);

    @Query("SELECT q FROM Question q WHERE q.difficulty = :d")
    List<Question> findByDifficulty(@Param("d") Integer difficulty);
}
