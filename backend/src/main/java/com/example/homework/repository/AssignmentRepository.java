package com.example.homework.repository;

import com.example.homework.entity.Assignment;
import com.example.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByTeacher(User teacher);
    List<Assignment> findByClassGroupId(Long classGroupId);
    List<Assignment> findByClassGroupIdIn(List<Long> classGroupIds);
}
