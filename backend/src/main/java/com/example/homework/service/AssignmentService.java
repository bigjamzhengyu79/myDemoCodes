package com.example.homework.service;

import com.example.entity.User;
import com.example.homework.dto.AssignmentDto;
import com.example.homework.entity.*;
import com.example.homework.repository.*;
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

    public List<AssignmentDto.Response> listByTeacher(String username) {
        User teacher = userRepository.findByUsername(username).orElseThrow();
        return assignmentRepository.findByTeacher(teacher).stream()
                .map(AssignmentDto.Response::from)
                .collect(Collectors.toList());
    }

    public List<AssignmentDto.Response> listForStudent(String username) {
        User student = userRepository.findByUsername(username).orElseThrow();
        String studentClassName = student.getClassName();

        // 获取所有已发布的作业，再根据班级进行过滤
        return assignmentRepository.findAll().stream()
                .filter(a -> a.getStatus() == Assignment.Status.PUBLISHED)
                .filter(a -> {
                    // 如果作业班级为空，则所有学生都能看到
                    if (a.getClassName() == null || a.getClassName().isEmpty()) {
                        return true;
                    }
                    // 如果作业班级不为空，只有班级匹配的学生能看到
                    return a.getClassName().equals(studentClassName);
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
        a.setClassName(req.getClassName());
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
