package com.example.homework.service;

import com.example.entity.ClassGroup;
import com.example.entity.User;
import com.example.homework.dto.AssignmentDto;
import com.example.homework.entity.*;
import com.example.homework.repository.*;
import com.example.repository.ClassGroupRepository;
import com.example.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final ClassGroupRepository classGroupRepository;

    public List<AssignmentDto.Response> listByTeacher(String username) {
        User teacher = userRepository.findByUsername(username).orElseThrow();
        return assignmentRepository.findByTeacher(teacher).stream()
                .map(AssignmentDto.Response::from)
                .collect(Collectors.toList());
    }

    public List<AssignmentDto.Response> listForStudent(String username) {
        User student = userRepository.findByUsername(username).orElseThrow();

        // 通过关联表查出学生所在的所有班级 ID
        List<Long> classGroupIds = classGroupRepository.findClassGroupIdsByStudentId(student.getId());

        if (classGroupIds.isEmpty()) {
            // 学生未加入任何班级时，只显示未指定班级的作业
            return assignmentRepository.findAll().stream()
                    .filter(a -> a.getStatus() == Assignment.Status.PUBLISHED)
                    .filter(a -> a.getClassGroup() == null)
                    .map(AssignmentDto.Response::from)
                    .collect(Collectors.toList());
        }

        // 获取学生所在班级的已发布作业
        return assignmentRepository.findAll().stream()
                .filter(a -> a.getStatus() == Assignment.Status.PUBLISHED)
                .filter(a -> {
                    // 未指定班级的作业对所有学生可见
                    if (a.getClassGroup() == null) return true;
                    // 指定了班级的作业只对该班学生可见
                    return classGroupIds.contains(a.getClassGroup().getId());
                })
                .map(AssignmentDto.Response::from)
                .collect(Collectors.toList());
    }

    public AssignmentDto.Response getById(Long id) {
        return AssignmentDto.Response.from(
                assignmentRepository.findById(id).orElseThrow(() -> new RuntimeException("作业不存在")));
    }

    @Transactional
    public AssignmentDto.Response create(AssignmentDto.Request req, String username) {
        User teacher = userRepository.findByUsername(username).orElseThrow();
        Assignment a = new Assignment();
        a.setTitle(req.getTitle());
        a.setDescription(req.getDescription());
        // 通过 classGroupId 关联班级（如果提供的话）
        if (req.getClassGroupId() != null) {
            ClassGroup cg = classGroupRepository.findById(req.getClassGroupId())
                    .orElseThrow(() -> new RuntimeException("班级不存在: " + req.getClassGroupId()));
            a.setClassGroup(cg);
        }
        a.setDueTime(req.getDueTime());
        a.setTeacher(teacher);
        a.setStatus(Assignment.Status.DRAFT);
        if (req.getQuestionIds() != null) {
            List<Question> qs = req.getQuestionIds().stream()
                    .map(qid -> questionRepository.findById(qid).orElseThrow())
                    .collect(Collectors.toList());
            a.setQuestions(qs);
        }
        return AssignmentDto.Response.from(assignmentRepository.save(a));
    }

    @Transactional
    public AssignmentDto.Response publish(Long id) {
        Assignment a = assignmentRepository.findById(id).orElseThrow();
        a.setStatus(Assignment.Status.PUBLISHED);
        return AssignmentDto.Response.from(assignmentRepository.save(a));
    }

    // 获取作业完整题目列表
    public Assignment getWithQuestions(Long id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("作业不存在"));
    }

}
