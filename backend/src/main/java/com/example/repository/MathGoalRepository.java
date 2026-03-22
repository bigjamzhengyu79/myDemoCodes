package com.example.repository;

import com.example.entity.MathGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MathGoalRepository extends JpaRepository<MathGoal, Long> {
    List<MathGoal> findByStatus(String status);
    List<MathGoal> findByCategory(String category);
}
