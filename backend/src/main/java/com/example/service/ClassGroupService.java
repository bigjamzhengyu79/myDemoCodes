package com.example.service;

import com.example.entity.ClassGroup;
import com.example.entity.User;
import com.example.repository.ClassGroupRepository;
import com.example.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClassGroupService {

    private final ClassGroupRepository classGroupRepository;
    private final UserRepository userRepository;

    public List<ClassGroup> getAllClassGroups() {
        return classGroupRepository.findAll();
    }

    public ClassGroup getClassGroupById(Long id) {
        return classGroupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("班级不存在: " + id));
    }

    @Transactional
    public ClassGroup createClassGroup(ClassGroup classGroup) {
        if (classGroupRepository.existsByName(classGroup.getName())) {
            throw new RuntimeException("班级名称已存在");
        }
        return classGroupRepository.save(classGroup);
    }

    @Transactional
    public ClassGroup updateClassGroup(Long id, ClassGroup classGroup) {
        ClassGroup existing = getClassGroupById(id);
        existing.setName(classGroup.getName());
        existing.setDescription(classGroup.getDescription());
        return classGroupRepository.save(existing);
    }

    @Transactional
    public void deleteClassGroup(Long id) {
        if (!classGroupRepository.existsById(id)) {
            throw new RuntimeException("班级不存在: " + id);
        }
        classGroupRepository.deleteById(id);
    }

    // ====== 老师管理 ======

    /**
     * 设置班级的负责老师
     */
    @Transactional
    public ClassGroup setTeacher(Long classGroupId, Long teacherId) {
        ClassGroup cg = getClassGroupById(classGroupId);
        if (teacherId == null) {
            cg.setTeacher(null);
        } else {
            User teacher = userRepository.findById(teacherId)
                    .orElseThrow(() -> new RuntimeException("用户不存在: " + teacherId));
            if (teacher.getRole() != User.Role.TEACHER && teacher.getRole() != User.Role.ADMIN) {
                throw new RuntimeException("指定的用户不是老师或管理员");
            }
            cg.setTeacher(teacher);
        }
        return classGroupRepository.save(cg);
    }

    /**
     * 获取指定老师负责的所有班级
     */
    public List<ClassGroup> getClassGroupsByTeacher(Long teacherId) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + teacherId));
        return classGroupRepository.findByTeacher(teacher);
    }

    // ====== 学生管理 ======

    /**
     * 向班级添加学生
     */
    @Transactional
    public ClassGroup addStudent(Long classGroupId, Long studentId) {
        ClassGroup cg = getClassGroupById(classGroupId);
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("学生不存在: " + studentId));
        if (student.getRole() != User.Role.STUDENT) {
            throw new RuntimeException("指定的用户不是学生");
        }
        if (cg.getStudents().stream().anyMatch(s -> s.getId().equals(studentId))) {
            throw new RuntimeException("该学生已在本班级中");
        }
        cg.getStudents().add(student);
        return classGroupRepository.save(cg);
    }

    /**
     * 从班级移除学生
     */
    @Transactional
    public ClassGroup removeStudent(Long classGroupId, Long studentId) {
        ClassGroup cg = getClassGroupById(classGroupId);
        boolean removed = cg.getStudents().removeIf(s -> s.getId().equals(studentId));
        if (!removed) {
            throw new RuntimeException("该学生不在此班级中");
        }
        return classGroupRepository.save(cg);
    }

    /**
     * 获取班级中的所有学生
     */
    public List<User> getStudents(Long classGroupId) {
        getClassGroupById(classGroupId); // 验证班级存在
        return classGroupRepository.findStudentsByClassGroupId(classGroupId);
    }

    /**
     * 获取学生加入的所有班级
     */
    public List<ClassGroup> getClassGroupsByStudent(Long studentId) {
        return classGroupRepository.findByStudentId(studentId);
    }
}