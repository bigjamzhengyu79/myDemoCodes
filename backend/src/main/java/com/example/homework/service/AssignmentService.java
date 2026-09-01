package com.example.homework.service;

import com.example.entity.ClassGroup;
import com.example.entity.User;
import com.example.homework.dto.AssignmentDto;
import com.example.homework.entity.*;
import com.example.homework.repository.*;
import com.example.repository.ClassGroupRepository;
import com.example.repository.UserRepository;

import com.example.homework.dto.QuestionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.homework.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final ClassGroupRepository classGroupRepository;
    private final StudentAnswerRepository studentAnswerRepository;

    public List<AssignmentDto.Response> listByTeacher(String username) {
        User teacher = userRepository.findByUsername(username).orElseThrow();
        return assignmentRepository.findByTeacher(teacher).stream()
                .map(AssignmentDto.Response::from)
                .collect(Collectors.toList());
    }

    /**
     * 目标关联作业的候选列表（分页 + 筛选）。
     *
     * 供 GoalModal 的作业选择器使用。原先前端一次性拿全量再自己过滤 PUBLISHED，
     * 既把草稿也传了过去，作业变多后列表也无法操作 —— 状态过滤因此下沉到这里。
     *
     * status 为 null 时不过滤状态；onlyOngoing=true 时只保留未截止的作业。
     */
    public PageResponse<AssignmentDto.Response> listForPicker(
            String username, String keyword, Long classGroupId,
            Assignment.Status status, boolean onlyOngoing, Pageable pageable) {

        User teacher = userRepository.findByUsername(username).orElseThrow();
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        // now 传 null 表示不做截止时间过滤（查询里以 :now IS NULL 短路）
        LocalDateTime now = onlyOngoing ? LocalDateTime.now() : null;

        Page<Assignment> page = assignmentRepository.findForPicker(
                teacher, status, kw, classGroupId, now, pageable);

        List<AssignmentDto.Response> content = page.getContent().stream()
                .map(AssignmentDto.Response::from)
                .collect(Collectors.toList());
        return PageResponse.of(content, page);
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
                    .map(a -> {
                        AssignmentDto.Response r = AssignmentDto.Response.from(a);
                        long count = studentAnswerRepository.countByAssignmentAndStudent(a.getId(), student.getId());
                        r.setAnsweredCount((int) count);
                        return r;
                    })
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
                .map(a -> {
                    AssignmentDto.Response r = AssignmentDto.Response.from(a);
                    // 查询该学生在此作业上的已答题数
                    long count = studentAnswerRepository.countByAssignmentAndStudent(a.getId(), student.getId());
                    r.setAnsweredCount((int) count);
                    return r;
                })
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
    @Transactional(readOnly = true)
    public Assignment getWithQuestions(Long id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("作业不存在"));
    }

    /**
     * 获取作业详情（含完整的题目列表），在事务内完成所有懒加载访问。
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAssignmentDetail(Long id) {
        Assignment a = getWithQuestions(id);
        Map<String, Object> resp = new HashMap<>();
        resp.put("id", a.getId());
        resp.put("title", a.getTitle());
        resp.put("description", a.getDescription() != null ? a.getDescription() : "");
        resp.put("classGroupId", a.getClassGroup() != null ? a.getClassGroup().getId() : null);
        resp.put("classGroupName", a.getClassGroup() != null ? a.getClassGroup().getName() : "");
        resp.put("dueTime", a.getDueTime() != null ? a.getDueTime().toString() : "");
        resp.put("status", a.getStatus().name());
        resp.put("questionCount", a.getQuestions().size());
        resp.put("questions", a.getQuestions().stream()
                .map(QuestionDto.Response::from)
                .collect(Collectors.toList()));
        return resp;
    }

}
