package com.example.homework.repository;

import com.example.homework.entity.Assignment;
import com.example.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByTeacher(User teacher);
    List<Assignment> findByClassGroupId(Long classGroupId);
    List<Assignment> findByClassGroupIdIn(List<Long> classGroupIds);

    /**
     * 目标关联作业的候选列表（分页）。
     *
     * 老师的作业会随学期不断累积，原先「一次性返回全部、前端渲染成复选框长列表」
     * 在作业变多后无法使用，故改为服务端分页 + 筛选（与题库选择器 QuestionRepository
     * .findIdsForSummary 同一套做法）。
     *
     * 参数为 null 时该条件不参与过滤。
     * onlyOngoing=true 时只保留未截止的作业（dueTime 为空视为长期有效，予以保留）。
     * JOIN FETCH classGroup：列表要显示班级名，避免逐行懒加载。
     */
    @Query(value = """
            SELECT a FROM Assignment a LEFT JOIN FETCH a.classGroup
            WHERE a.teacher = :teacher
              AND (:status IS NULL OR a.status = :status)
              AND (:keyword IS NULL OR LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:classGroupId IS NULL OR a.classGroup.id = :classGroupId)
              AND (:now IS NULL OR a.dueTime IS NULL OR a.dueTime >= :now)
            ORDER BY a.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(a) FROM Assignment a
            WHERE a.teacher = :teacher
              AND (:status IS NULL OR a.status = :status)
              AND (:keyword IS NULL OR LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:classGroupId IS NULL OR a.classGroup.id = :classGroupId)
              AND (:now IS NULL OR a.dueTime IS NULL OR a.dueTime >= :now)
            """)
    Page<Assignment> findForPicker(@Param("teacher") User teacher,
                                   @Param("status") Assignment.Status status,
                                   @Param("keyword") String keyword,
                                   @Param("classGroupId") Long classGroupId,
                                   @Param("now") LocalDateTime now,
                                   Pageable pageable);
}
