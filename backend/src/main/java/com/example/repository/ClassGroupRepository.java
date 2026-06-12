package com.example.repository;

import com.example.entity.ClassGroup;
import com.example.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClassGroupRepository extends JpaRepository<ClassGroup, Long> {
    boolean existsByName(String name);

    /**
     * 查找指定老师负责的所有班级
     */
    List<ClassGroup> findByTeacher(User teacher);

    /**
     * 查找指定学生加入的所有班级
     */
    @Query("SELECT c FROM ClassGroup c JOIN c.students s WHERE s.id = :studentId")
    List<ClassGroup> findByStudentId(@Param("studentId") Long studentId);

    /**
     * 查找指定学生加入的所有班级ID
     */
    @Query("SELECT c.id FROM ClassGroup c JOIN c.students s WHERE s.id = :studentId")
    List<Long> findClassGroupIdsByStudentId(@Param("studentId") Long studentId);

    /**
     * 查找指定班级中的所有学生
     */
    @Query("SELECT s FROM ClassGroup c JOIN c.students s WHERE c.id = :classGroupId")
    List<User> findStudentsByClassGroupId(@Param("classGroupId") Long classGroupId);

    /**
     * 批量查询多个学生加入的所有班级名称
     * 返回 [studentId, className] 数组列表
     */
    @Query("SELECT s.id, c.name FROM ClassGroup c JOIN c.students s WHERE s.id IN :studentIds")
    List<Object[]> findStudentClassNames(@Param("studentIds") List<Long> studentIds);

    /**
     * 批量查询多个老师负责的所有班级名称
     * 返回 [teacherId, className] 数组列表
     */
    @Query("SELECT c.teacher.id, c.name FROM ClassGroup c WHERE c.teacher.id IN :teacherIds")
    List<Object[]> findTeacherClassNames(@Param("teacherIds") List<Long> teacherIds);
}
