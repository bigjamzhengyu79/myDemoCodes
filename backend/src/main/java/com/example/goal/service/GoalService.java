package com.example.goal.service;

import com.example.entity.ClassGroup;
import com.example.entity.User;
import com.example.goal.dto.GoalDto.*;
import com.example.goal.entity.*;
import com.example.goal.repository.*;
import com.example.repository.ClassGroupRepository;
import com.example.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GoalService {

    private final GoalRepository goalRepository;
    private final GoalAssigneeRepository goalAssigneeRepository;
    private final GoalCommentRepository goalCommentRepository;
    private final GoalAssignmentRepository goalAssignmentRepository;
    private final StudentGoalProgressRepository studentGoalProgressRepository;
    private final UserRepository userRepository;
    private final ClassGroupRepository classGroupRepository;

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    /**
     * 目标列表：
     * - 管理员：看到全部目标
     * - 老师：只看到自己创建的目标
     * - 学生：只看到分配给我的目标
     * - 未登录/旧 token（userId/role 为 null）：看到全部目标（向后兼容）
     */
    public List<GoalResponse> listParentGoals(String status, String keyword, Long currentUserId, String currentRole) {
        List<Goal> goals;
        // currentUserId 为 null 表示旧 token，降级为查看全部（向后兼容）
        if (currentUserId == null || currentRole == null || "ADMIN".equals(currentRole)) {
            // 未登录/旧 token 或管理员：查看全部
            goals = goalRepository.findByParentIsNullOrderByCreatedAtDesc();
        } else if ("TEACHER".equals(currentRole)) {
            // 老师：只查看自己创建的目标
            goals = goalRepository.findByParentIsNullAndManagerIdOrderByCreatedAtDesc(currentUserId);
        } else {
            // 学生：只查看分配给我的目标
            User student = findUserOrThrow(currentUserId);
            List<GoalAssignee> myAssignments = goalAssigneeRepository.findByStudent(student);
            Set<Long> goalIds = myAssignments.stream()
                    .map(a -> a.getGoal().getId())
                    .collect(Collectors.toSet());
            goals = goalRepository.findByParentIsNullOrderByCreatedAtDesc().stream()
                    .filter(g -> goalIds.contains(g.getId()) || hasAssignedDescendant(g, goalIds))
                    .collect(Collectors.toList());
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            goals = goals.stream()
                    .filter(g -> g.getTitle().contains(kw) || (g.getOwners() != null && g.getOwners().contains(kw)))
                    .collect(Collectors.toList());
        } else if (status != null && !status.isBlank()) {
            GoalStatus st = GoalStatus.valueOf(status.toUpperCase());
            goals = goals.stream()
                    .filter(g -> calcStatus(g) == st)
                    .collect(Collectors.toList());
        }
        return goals.stream().map(g -> toResponse(g, currentUserId)).collect(Collectors.toList());
    }

    /**
     * 检查目标的子孙中是否有被分配给当前用户的
     */
    private boolean hasAssignedDescendant(Goal parent, Set<Long> assignedGoalIds) {
        if (parent.getSubGoals() == null || parent.getSubGoals().isEmpty()) return false;
        for (Goal sub : parent.getSubGoals()) {
            if (assignedGoalIds.contains(sub.getId()) || hasAssignedDescendant(sub, assignedGoalIds)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 按需加载子目标，支持指定深度
     */
    public List<GoalResponse> loadSubGoals(Long parentId, Integer depth, Long currentUserId) {
        if (depth == null || depth <= 1) {
            List<Goal> subs = goalRepository.findByParentIdOrderByPlannedStartAsc(parentId);
            return subs.stream().map(g -> toResponse(g, currentUserId)).collect(Collectors.toList());
        } else {
            List<Goal> descendants = goalRepository.findDescendantsWithinDepth(parentId, depth);
            return descendants.stream().map(g -> toResponse(g, currentUserId)).collect(Collectors.toList());
        }
    }

    public GoalResponse getGoal(Long id, Long currentUserId) {
        return toResponse(findOrThrow(id), currentUserId);
    }

    @Transactional
    public GoalResponse createGoal(GoalRequest req, Long teacherId) {
        Goal goal = new Goal();
        applyRequest(goal, req);
        // 设置目标创建者(老师)，teacherId 为 null 时兼容旧 token
        if (teacherId != null) {
            User manager = findUserOrThrow(teacherId);
            goal.setManager(manager);
        }
        if (goal.getParent() != null) {
            goal.setDepth(goal.getParent().getDepth() + 1);
            // 子目标未指定班级时，自动继承父目标的班级
            if (req.getClassGroupId() == null && goal.getParent().getClassGroup() != null) {
                goal.setClassGroup(goal.getParent().getClassGroup());
            }
        } else {
            goal.setDepth(1);
        }
        Goal saved = goalRepository.save(goal);
        // 处理多对多学生分配
        if (req.getAssigneeIds() != null) {
            replaceAssignees(saved, req.getAssigneeIds());
        } else if (req.getClassGroupId() != null) {
            autoFillStudentsFromClass(saved, req.getClassGroupId());
        } else if (saved.getClassGroup() != null && req.getAssigneeIds() == null) {
            autoFillStudentsFromClass(saved, saved.getClassGroup().getId());
        }
        // 处理关联作业
        if (req.getAssignmentIds() != null) {
            replaceAssignments(saved, req.getAssignmentIds());
        }
        // 如果指定了 sourceGoalId，递归复制源目标的子目标及其关联数据
        if (req.getSourceGoalId() != null && teacherId != null) {
            Goal source = findOrThrow(req.getSourceGoalId());
            User manager = findUserOrThrow(teacherId);
            copySubGoalsRecursive(source, saved, manager);
        }
        return toResponse(saved, teacherId);
    }

    @Transactional
    public GoalResponse updateGoal(Long id, GoalRequest req, Long currentUserId, String currentRole) {
        Goal goal = findOrThrow(id);
        // 权限校验：未登录/旧 token 跳过校验（向后兼容）
        if (currentUserId != null && currentRole != null) {
            if (!"ADMIN".equals(currentRole) && (goal.getManager() == null || !goal.getManager().getId().equals(currentUserId))) {
                throw new RuntimeException("无权编辑此目标");
            }
        }
        applyRequest(goal, req);
        if (goal.getParent() != null) {
            goal.setDepth(goal.getParent().getDepth() + 1);
        } else {
            goal.setDepth(1);
        }
        if (!goal.getSubGoals().isEmpty()) {
            recalcFromSubs(goal);
        }
        Goal saved = goalRepository.save(goal);
        if (req.getAssigneeIds() != null) {
            replaceAssignees(saved, req.getAssigneeIds());
        } else if (req.getClassGroupId() != null) {
            autoFillStudentsFromClass(saved, req.getClassGroupId());
        }
        // 处理关联作业
        if (req.getAssignmentIds() != null) {
            replaceAssignments(saved, req.getAssignmentIds());
        }
        return toResponse(saved, currentUserId);
    }

    @Transactional
    public void deleteGoal(Long id, Long currentUserId, String currentRole) {
        Goal goal = findOrThrow(id);
        // 权限校验：未登录/旧 token 跳过校验（向后兼容）
        if (currentUserId != null && currentRole != null) {
            if (!"ADMIN".equals(currentRole) && (goal.getManager() == null || !goal.getManager().getId().equals(currentUserId))) {
                throw new RuntimeException("无权删除此目标");
            }
        }
        // 手动清理 StudentGoalProgress（Goal 实体没有 @OneToMany 映射此表，无法级联删除）
        List<Goal> allGoalsInTree = new ArrayList<>();
        collectGoalTree(goal, allGoalsInTree);
        for (Goal g : allGoalsInTree) {
            studentGoalProgressRepository.deleteAll(studentGoalProgressRepository.findByGoal(g));
        }
        // 手动清理 GoalComment（避免懒加载集合未初始化导致的问题）
        List<GoalComment> comments = goalCommentRepository.findByGoalOrderByCreatedAtAsc(goal);
        if (!comments.isEmpty()) {
            goalCommentRepository.deleteAll(comments);
        }
        // 删除目标（级联删除 assignees、assignments 等子关联）
        goalRepository.delete(goal);
    }

    /**
     * 递归收集目标树中所有目标节点
     */
    private void collectGoalTree(Goal parent, List<Goal> result) {
        result.add(parent);
        if (parent.getSubGoals() != null) {
            for (Goal sub : parent.getSubGoals()) {
                collectGoalTree(sub, result);
            }
        }
    }

    public GoalStatsResponse getStats(Long currentUserId, String currentRole) {
        List<Goal> parents;
        // currentUserId 为 null 表示旧 token，降级为查看全部（向后兼容）
        if (currentUserId == null || currentRole == null || "ADMIN".equals(currentRole)) {
            parents = goalRepository.findByParentIsNullOrderByCreatedAtDesc();
        } else if ("TEACHER".equals(currentRole)) {
            parents = goalRepository.findByParentIsNullAndManagerIdOrderByCreatedAtDesc(currentUserId);
        } else {
            User student = findUserOrThrow(currentUserId);
            List<GoalAssignee> myAssignments = goalAssigneeRepository.findByStudent(student);
            Set<Long> goalIds = myAssignments.stream()
                    .map(a -> a.getGoal().getId())
                    .collect(Collectors.toSet());
            parents = goalRepository.findByParentIsNullOrderByCreatedAtDesc().stream()
                    .filter(g -> goalIds.contains(g.getId()) || hasAssignedDescendant(g, goalIds))
                    .collect(Collectors.toList());
        }
        GoalStatsResponse stats = new GoalStatsResponse();
        stats.setTotalParent(parents.size());
        stats.setTotalSub(parents.stream().mapToLong(g -> g.getSubGoals().size()).sum());
        stats.setDone(parents.stream().filter(g -> calcStatus(g) == GoalStatus.DONE).count());
        stats.setLate(parents.stream().filter(g -> calcStatus(g) == GoalStatus.LATE).count());
        stats.setAvgProgress(parents.isEmpty() ? 0 :
                (int) Math.round(parents.stream().mapToInt(this::calcProgress).average().orElse(0)));
        return stats;
    }

    // ====== 学生个人进度 ======

    /**
     * 学生更新自己的目标进度
     */
    @Transactional
    public GoalResponse updateMyProgress(Long goalId, Long studentId, StudentProgressUpdateRequest req) {
        Goal goal = findOrThrow(goalId);
        User student = findUserOrThrow(studentId);
        StudentGoalProgress progress = studentGoalProgressRepository
                .findByGoalAndStudent(goal, student)
                .orElseThrow(() -> new RuntimeException("未找到该目标的个人进度记录"));
        if (req.getProgress() != null) {
            progress.setProgress(req.getProgress());
        }
        if (req.getStatus() != null) {
            progress.setStatus(GoalStatus.valueOf(req.getStatus().toUpperCase()));
        } else if (req.getProgress() != null) {
            // 根据进度自动推断状态
            if (req.getProgress() >= 100) {
                progress.setStatus(GoalStatus.DONE);
            } else if (req.getProgress() > 0) {
                progress.setStatus(GoalStatus.IN_PROGRESS);
            } else {
                progress.setStatus(GoalStatus.TODO);
            }
        }
        if (req.getActualStart() != null) {
            progress.setActualStart(req.getActualStart());
        }
        if (req.getActualEnd() != null) {
            progress.setActualEnd(req.getActualEnd());
        }
        studentGoalProgressRepository.save(progress);
        return toResponse(goal, studentId);
    }

    /**
     * 学生获取自己被分配的所有目标（个人简版）
     */
    public List<GoalResponse> listMyGoals(Long studentId) {
        User student = findUserOrThrow(studentId);
        List<GoalAssignee> myAssignments = goalAssigneeRepository.findByStudent(student);
        Set<Long> goalIds = myAssignments.stream()
                .map(a -> a.getGoal().getId())
                .collect(Collectors.toSet());
        List<Goal> parents = goalRepository.findByParentIsNullOrderByCreatedAtDesc().stream()
                .filter(g -> goalIds.contains(g.getId()) || hasAssignedDescendant(g, goalIds))
                .collect(Collectors.toList());
        return parents.stream().map(g -> toResponse(g, studentId)).collect(Collectors.toList());
    }

    // ====== 评论 ======

    /**
     * 添加评论。
     * - 学生：仅可评论被分配的目标
     * - 老师：仅可评论自己创建的目标，且只能发 PUBLIC 或 PRIVATE_TO_STUDENT
     */
    @Transactional
    public CommentResponse addComment(Long goalId, Long authorId, CommentRequest req) {
        Goal goal = findOrThrow(goalId);
        User author = findUserOrThrow(authorId);
        String role = author.getRole().name();
        String visibility = req.getVisibility() != null ? req.getVisibility() : "PUBLIC";

        GoalComment comment = new GoalComment();
        comment.setGoal(goal);
        comment.setAuthor(author);
        comment.setAuthorRole(role);
        comment.setContent(req.getContent());
        comment.setVisibility(visibility);
        comment.setTargetStudentId(req.getTargetStudentId());
        comment.setImageUrls(encodeAttachments(req.getImageUrls(), req.getAttachmentNames()));

        if ("STUDENT".equals(role)) {
            // 学生必须是被分配的目标才能评论
            if (!goalAssigneeRepository.existsByGoalAndStudent(goal, author)) {
                throw new RuntimeException("您未被分配此目标，无法评论");
            }
            // 学生可发公开评论或回复私密评论
            if ("PRIVATE_TO_STUDENT".equals(visibility)) {
                // 学生回复私密评论时，目标学生设为老师（即目标创建者）
                if (req.getTargetStudentId() == null && goal.getManager() != null) {
                    comment.setTargetStudentId(goal.getManager().getId());
                }
            }
            comment.setStudent(author);
        } else if ("TEACHER".equals(role) || "ADMIN".equals(role)) {
            // 老师只能评论自己创建的目标（管理员可评论任意目标）
            if (!"ADMIN".equals(role) && (goal.getManager() == null || !goal.getManager().getId().equals(authorId))) {
                throw new RuntimeException("您只能评论自己创建的目标");
            }
            if ("PRIVATE_TO_STUDENT".equals(visibility) && req.getTargetStudentId() == null) {
                throw new RuntimeException("私密评论必须指定目标学生");
            }
        }

        goalCommentRepository.save(comment);
        return toCommentResponse(comment, authorId);
    }

    /**
     * 编辑自己的评论（仅可编辑内容）
     */
    @Transactional
    public CommentResponse updateComment(Long commentId, Long userId, CommentRequest req) {
        GoalComment comment = goalCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("评论不存在"));
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("无权编辑他人的评论");
        }
        comment.setContent(req.getContent());
        comment.setImageUrls(encodeAttachments(req.getImageUrls(), req.getAttachmentNames()));
        goalCommentRepository.save(comment);
        return toCommentResponse(comment, userId);
    }

    /**
     * 删除自己的评论
     */
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        GoalComment comment = goalCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("评论不存在"));
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("无权删除他人的评论");
        }
        goalCommentRepository.delete(comment);
    }

    /**
     * 获取某个目标对当前用户可见的公开评论列表。
     * - 所有人（老师/学生/管理员）：看到该目标下所有公开评论（群聊效果）
     */
    public List<CommentResponse> getComments(Long goalId, Long currentUserId, String currentRole) {
        Goal goal = findOrThrow(goalId);
        List<GoalComment> comments;
        if (currentUserId != null && currentRole != null) {
            // 所有已登录用户都能看到目标的全部公开评论（群聊）
            comments = goalCommentRepository.findByGoalAndVisibilityOrderByCreatedAtAsc(goal, "PUBLIC");
        } else {
            comments = Collections.emptyList();
        }
        return comments.stream()
                .map(c -> toCommentResponse(c, currentUserId))
                .collect(Collectors.toList());
    }

    /**
     * 获取私密评论。
     * - 学生：获取老师发给自己的私密评论 + 自己的回复
     * - 老师：获取自己发给学生的私密评论
     */
    public List<CommentResponse> getPrivateComments(Long goalId, Long currentUserId, String currentRole) {
        Goal goal = findOrThrow(goalId);
        boolean isTeacher = currentUserId != null && currentRole != null &&
                ("TEACHER".equals(currentRole) || "ADMIN".equals(currentRole));

        List<GoalComment> comments;
        if (isTeacher) {
            // 老师看到自己发出的私密评论（关联该目标被分配的学生）
            List<GoalAssignee> assignees = goalAssigneeRepository.findByGoal(goal);
            List<Long> studentIds = assignees.stream()
                    .map(a -> a.getStudent().getId())
                    .collect(Collectors.toList());
            if (studentIds.isEmpty()) {
                return Collections.emptyList();
            }
            comments = goalCommentRepository.findPrivateCommentsByGoalAndTeacher(goal, currentUserId, studentIds);
        } else if (currentUserId != null) {
            // 学生看到涉及自己的私密评论
            comments = goalCommentRepository.findPrivateCommentsByGoalAndStudent(goal, currentUserId);
        } else {
            comments = Collections.emptyList();
        }
        return comments.stream()
                .map(c -> toCommentResponse(c, currentUserId))
                .collect(Collectors.toList());
    }

    // ====== 老师查看学生执行概览 ======

    /**
     * 老师获取某个目标下所有学生的执行情况（递归包含子目标）
     */
    public GoalStudentOverviewResponse getStudentOverview(Long goalId, Long teacherId) {
        Goal goal = findOrThrow(goalId);
        GoalStudentOverviewResponse overview = new GoalStudentOverviewResponse();
        overview.setGoalId(goal.getId());
        overview.setGoalTitle(goal.getTitle());

        // 获取该目标下所有学生的进度
        List<StudentGoalProgress> progresses = studentGoalProgressRepository.findByGoal(goal);
        overview.setStudentProgresses(progresses.stream().map(p -> {
            StudentProgressResponse sp = new StudentProgressResponse();
            sp.setStudentId(p.getStudent().getId());
            sp.setStudentName(p.getStudent().getRealName());
            sp.setProgress(p.getProgress());
            sp.setStatus(p.getStatus().name());
            sp.setActualStart(p.getActualStart());
            sp.setActualEnd(p.getActualEnd());
            return sp;
        }).collect(Collectors.toList()));

        // 获取该目标下所有学生的公开评论 + 私密评论（按学生分组）
        List<GoalComment> publicComments = goalCommentRepository.findByGoalAndVisibilityOrderByCreatedAtAsc(goal, "PUBLIC");
        // 私密评论：老师发出的 + 学生回复的
        List<GoalAssignee> assignees = goalAssigneeRepository.findByGoal(goal);
        List<Long> studentIds = assignees.stream()
                .map(a -> a.getStudent().getId())
                .collect(Collectors.toList());
        List<GoalComment> privateComments = Collections.emptyList();
        if (!studentIds.isEmpty() && teacherId != null) {
            privateComments = goalCommentRepository.findPrivateCommentsByGoalAndTeacher(goal, teacherId, studentIds);
        }

        // 合并所有评论
        List<GoalComment> allComments = new ArrayList<>();
        allComments.addAll(publicComments);
        allComments.addAll(privateComments);
        allComments.sort(Comparator.comparing(GoalComment::getCreatedAt));

        // 按学生分组（对于私密评论，归属到目标学生或作者）
        Map<Long, List<GoalComment>> commentsByStudent = new LinkedHashMap<>();
        // 先为每个被分配的学生初始化空列表
        for (GoalAssignee ga : assignees) {
            commentsByStudent.put(ga.getStudent().getId(), new ArrayList<>());
        }
        // 把评论归类到对应的学生
        for (GoalComment c : allComments) {
            Long targetId = null;
            if (c.getStudent() != null) {
                // 学生发的评论，归到该学生
                targetId = c.getStudent().getId();
            } else if (c.getTargetStudentId() != null) {
                // 老师发的私密评论，归到目标学生
                targetId = c.getTargetStudentId();
            }
            if (targetId != null && commentsByStudent.containsKey(targetId)) {
                commentsByStudent.get(targetId).add(c);
            }
        }

        overview.setStudentComments(commentsByStudent.entrySet().stream().map(entry -> {
            StudentCommentGroupResponse group = new StudentCommentGroupResponse();
            group.setStudentId(entry.getKey());
            // 查找学生姓名
            String name = assignees.stream()
                    .filter(a -> a.getStudent().getId().equals(entry.getKey()))
                    .map(a -> a.getStudent().getRealName())
                    .findFirst().orElse("学生#" + entry.getKey());
            group.setStudentName(name);
            group.setComments(entry.getValue().stream()
                    .map(c -> toCommentResponse(c, teacherId))
                    .collect(Collectors.toList()));
            return group;
        }).collect(Collectors.toList()));

        // 递归获取子目标概览
        List<Goal> subGoals = goalRepository.findByParentIdOrderByPlannedStartAsc(goalId);
        if (!subGoals.isEmpty()) {
            overview.setSubGoals(subGoals.stream()
                    .map(sg -> getStudentOverview(sg.getId(), teacherId))
                    .collect(Collectors.toList()));
        }

        return overview;
    }

    // ====== 可复制目标列表 ======

    /**
     * 获取当前老师创建的可复制目标列表（仅父目标）
     */
    public List<GoalResponse> listCopyableGoals(Long teacherId) {
        List<Goal> goals = goalRepository.findByParentIsNullAndManagerIdOrderByCreatedAtDesc(teacherId);
        return goals.stream()
                .filter(g -> Boolean.TRUE.equals(g.getCopyable()))
                .map(g -> toResponse(g, teacherId))
                .collect(Collectors.toList());
    }

    /**
     * 切换目标的可复制标记
     */
    @Transactional
    public GoalResponse toggleCopyable(Long goalId, Long userId, String role, Boolean copyable) {
        Goal goal = findOrThrow(goalId);
        // 权限校验：管理员或目标创建者
        if (!"ADMIN".equals(role) && (goal.getManager() == null || !goal.getManager().getId().equals(userId))) {
            throw new RuntimeException("无权修改此目标");
        }
        goal.setCopyable(copyable);
        Goal saved = goalRepository.save(goal);
        return toResponse(saved, userId);
    }

    // ====== 目标复制（递归包含子目标） ======

    /**
     * 复制目标树（父目标 + 所有子目标递归）。
     * 新目标将由当前用户管理，重置状态为 TODO、进度为 0，清空时间信息。
     */
    @Transactional
    public GoalResponse copyGoalTree(Long sourceGoalId, Long userId) {
        Goal source = findOrThrow(sourceGoalId);
        User manager = findUserOrThrow(userId);

        // 递归复制目标树，返回新创建的顶层目标
        Goal newRoot = copyGoalRecursive(source, null, manager);
        return toResponse(newRoot, userId);
    }

    /**
     * 递归复制一个目标及其所有子目标
     */
    private Goal copyGoalRecursive(Goal source, Goal newParent, User manager) {
        Goal target = new Goal();
        target.setTitle(source.getTitle());
        target.setDescription(source.getDescription());
        target.setStatus(GoalStatus.TODO);
        target.setProgress(0);
        target.setOwners(source.getOwners());
        target.setCopyable(false);           // 复制品默认不可再复制
        target.setManager(manager);
        target.setParent(newParent);
        target.setDepth(newParent != null ? newParent.getDepth() + 1 : 1);
        target.setClassGroup(source.getClassGroup());
        target.setAssignee(source.getAssignee());
        target.setPlannedStart(null);
        target.setPlannedEnd(null);
        target.setActualStart(null);
        target.setActualEnd(null);
        // 保存当前层目标
        Goal saved = goalRepository.save(target);

        // 复制多对多学生分配
        List<GoalAssignee> sourceAssignees = goalAssigneeRepository.findByGoal(source);
        if (sourceAssignees != null && !sourceAssignees.isEmpty()) {
            for (GoalAssignee ga : sourceAssignees) {
                GoalAssignee newGa = new GoalAssignee();
                newGa.setGoal(saved);
                newGa.setStudent(ga.getStudent());
                goalAssigneeRepository.save(newGa);
                // 自动创建学生个人进度记录
                initStudentProgress(saved, ga.getStudent().getId());
            }
        }

        // 复制关联作业
        List<GoalAssignment> sourceAssignments = goalAssignmentRepository.findByGoal(source);
        if (sourceAssignments != null && !sourceAssignments.isEmpty()) {
            for (GoalAssignment ga : sourceAssignments) {
                GoalAssignment newGa = new GoalAssignment();
                newGa.setGoal(saved);
                newGa.setAssignmentId(ga.getAssignmentId());
                goalAssignmentRepository.save(newGa);
            }
        }

        // 递归复制子目标
        List<Goal> subGoals = source.getSubGoals();
        if (subGoals != null && !subGoals.isEmpty()) {
            for (Goal sub : subGoals) {
                copyGoalRecursive(sub, saved, manager);
            }
        }

        return saved;
    }

    /**
     * 从源目标递归复制子目标及其关联数据到新创建的父目标下。
     * 仅在「从已有目标复制」创建模式中调用，此时新父目标已通过用户提交的表单创建完毕。
     */
    private void copySubGoalsRecursive(Goal source, Goal newParent, User manager) {
        List<Goal> subGoals = source.getSubGoals();
        if (subGoals == null || subGoals.isEmpty()) return;

        for (Goal sub : subGoals) {
            Goal target = new Goal();
            target.setTitle(sub.getTitle());
            target.setDescription(sub.getDescription());
            target.setStatus(GoalStatus.TODO);
            target.setProgress(0);
            target.setOwners(sub.getOwners());
            target.setCopyable(false);
            target.setManager(manager);
            target.setParent(newParent);
            target.setDepth(newParent.getDepth() + 1);
            target.setClassGroup(sub.getClassGroup());
            target.setAssignee(sub.getAssignee());
            target.setPlannedStart(null);
            target.setPlannedEnd(null);
            target.setActualStart(null);
            target.setActualEnd(null);
            Goal saved = goalRepository.save(target);

            // 复制多对多学生分配
            List<GoalAssignee> sourceAssignees = goalAssigneeRepository.findByGoal(sub);
            if (sourceAssignees != null && !sourceAssignees.isEmpty()) {
                for (GoalAssignee ga : sourceAssignees) {
                    GoalAssignee newGa = new GoalAssignee();
                    newGa.setGoal(saved);
                    newGa.setStudent(ga.getStudent());
                    goalAssigneeRepository.save(newGa);
                    initStudentProgress(saved, ga.getStudent().getId());
                }
            }

            // 复制关联作业
            List<GoalAssignment> sourceAssignments = goalAssignmentRepository.findByGoal(sub);
            if (sourceAssignments != null && !sourceAssignments.isEmpty()) {
                for (GoalAssignment ga : sourceAssignments) {
                    GoalAssignment newGa = new GoalAssignment();
                    newGa.setGoal(saved);
                    newGa.setAssignmentId(ga.getAssignmentId());
                    goalAssignmentRepository.save(newGa);
                }
            }

            // 递归复制子目标的子目标
            copySubGoalsRecursive(sub, saved, manager);
        }
    }

    // ====== 关联作业管理 ======

    /**
     * 更新目标（通过 ID）的关联作业
     */
    @Transactional
    public void updateAssignments(Long goalId, List<Long> assignmentIds) {
        Goal goal = findOrThrow(goalId);
        replaceAssignments(goal, assignmentIds);
    }

    /**
     * 更新目标的关联作业（内部方法，传入 Goal 实体）
     */
    @Transactional
    public void replaceAssignments(Goal goal, List<Long> assignmentIds) {
        // 删除旧的关联
        List<GoalAssignment> existing = goalAssignmentRepository.findByGoal(goal);
        if (!existing.isEmpty()) {
            goalAssignmentRepository.deleteAll(existing);
            goalAssignmentRepository.flush();
        }
        if (assignmentIds == null || assignmentIds.isEmpty()) {
            goal.getGoalAssignments().clear();
            return;
        }
        for (Long aid : new HashSet<>(assignmentIds)) {
            GoalAssignment ga = new GoalAssignment();
            ga.setGoal(goal);
            ga.setAssignmentId(aid);
            goalAssignmentRepository.save(ga);
            goal.getGoalAssignments().add(ga);
        }
    }

    /**
     * 获取目标关联的作业 ID 列表
     */
    public List<Long> listAssignmentIds(Long goalId) {
        Goal goal = findOrThrow(goalId);
        return goalAssignmentRepository.findByGoal(goal).stream()
                .map(GoalAssignment::getAssignmentId)
                .collect(Collectors.toList());
    }

    // ====== 自动填充学生进度 ======

    private void initStudentProgress(Goal goal, Long studentId) {
        User student = findUserOrThrow(studentId);
        if (studentGoalProgressRepository.existsByGoalAndStudent(goal, student)) return;
        StudentGoalProgress p = new StudentGoalProgress();
        p.setGoal(goal);
        p.setStudent(student);
        p.setProgress(0);
        p.setStatus(GoalStatus.TODO);
        studentGoalProgressRepository.save(p);
    }

    // ====== 多对多学生分配 ======

    private void autoFillStudentsFromClass(Goal goal, Long classGroupId) {
        List<User> students = classGroupRepository.findStudentsByClassGroupId(classGroupId);
        if (students.isEmpty()) return;
        List<Long> studentIds = students.stream().map(User::getId).collect(Collectors.toList());
        replaceAssignees(goal, studentIds);
    }

    private void replaceAssignees(Goal goal, List<Long> studentIds) {
        List<GoalAssignee> existing = goalAssigneeRepository.findByGoal(goal);
        if (!existing.isEmpty()) {
            goalAssigneeRepository.deleteAll(existing);
            goalAssigneeRepository.flush();
        }
        if (studentIds == null || studentIds.isEmpty()) {
            goal.getAssignees().clear();
            return;
        }
        Set<Long> dedup = new HashSet<>(studentIds);
        for (Long sid : dedup) {
            User student = userRepository.findById(sid)
                    .orElseThrow(() -> new RuntimeException("学生不存在: " + sid));
            GoalAssignee ga = new GoalAssignee();
            ga.setGoal(goal);
            ga.setStudent(student);
            goalAssigneeRepository.save(ga);
            goal.getAssignees().add(ga);
            // 自动创建学生个人进度记录
            initStudentProgress(goal, sid);
        }
    }

    @Transactional
    public void assignStudent(Long goalId, Long studentId) {
        Goal goal = findOrThrow(goalId);
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("学生不存在: " + studentId));
        if (goalAssigneeRepository.existsByGoalAndStudent(goal, student)) {
            return;
        }
        GoalAssignee ga = new GoalAssignee();
        ga.setGoal(goal);
        ga.setStudent(student);
        goalAssigneeRepository.save(ga);
        goal.getAssignees().add(ga);
        initStudentProgress(goal, studentId);
    }

    @Transactional
    public void unassignStudent(Long goalId, Long studentId) {
        Goal goal = findOrThrow(goalId);
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("学生不存在: " + studentId));
        goalAssigneeRepository.deleteByGoalAndStudent(goal, student);
        goal.getAssignees().removeIf(a -> a.getStudent().getId().equals(studentId));
        // 删除对应的个人进度记录
        studentGoalProgressRepository.findByGoalAndStudent(goal, student)
                .ifPresent(studentGoalProgressRepository::delete);
    }

    public List<Long> listAssignedStudentIds(Long goalId) {
        Goal goal = findOrThrow(goalId);
        return goalAssigneeRepository.findByGoal(goal).stream()
                .map(a -> a.getStudent().getId())
                .collect(Collectors.toList());
    }

    // ====== 内部 ======

    private void applyRequest(Goal goal, GoalRequest req) {
        goal.setTitle(req.getTitle());
        goal.setDescription(req.getDescription());
        goal.setStatus(req.getStatus() != null ? req.getStatus() : GoalStatus.TODO);
        goal.setPlannedStart(req.getPlannedStart());
        goal.setPlannedEnd(req.getPlannedEnd());
        goal.setActualStart(req.getActualStart());
        goal.setActualEnd(req.getActualEnd());
        goal.setProgress(req.getProgress() != null ? req.getProgress() : 0);
        goal.setOwners(req.getOwners());
        if (req.getCopyable() != null) {
            goal.setCopyable(req.getCopyable());
        }
        if (req.getParentId() != null) {
            goal.setParent(findOrThrow(req.getParentId()));
        } else {
            goal.setParent(null);
        }
        if (req.getClassGroupId() != null) {
            ClassGroup cg = classGroupRepository.findById(req.getClassGroupId())
                    .orElseThrow(() -> new RuntimeException("班级不存在: " + req.getClassGroupId()));
            goal.setClassGroup(cg);
        } else {
            goal.setClassGroup(null);
        }
        if (req.getAssigneeId() != null) {
            User assignee = userRepository.findById(req.getAssigneeId())
                    .orElseThrow(() -> new RuntimeException("用户不存在: " + req.getAssigneeId()));
            goal.setAssignee(assignee);
        }
    }

    private void recalcFromSubs(Goal parent) {
        List<Goal> subs = parent.getSubGoals();
        if (subs.isEmpty()) return;
        double[] weights = {1.0, 0.8, 0.6, 0.4};
        int maxDepth = 4;
        double total = 0;
        double weightSum = 0;
        for (Goal sub : subs) {
            int d = Math.min(sub.getDepth() - parent.getDepth(), maxDepth - 1);
            double w = weights[Math.max(0, d)];
            total += calcProgressRecursive(sub, 1, weights);
            weightSum += w;
        }
        int weightedProgress = weightSum > 0 ? (int) Math.round(total / weightSum) : 0;
        parent.setProgress(weightedProgress);
        GoalStatus derived;
        if (subs.stream().allMatch(s -> s.getStatus() == GoalStatus.DONE)) {
            derived = GoalStatus.DONE;
        } else if (subs.stream().anyMatch(s -> s.getStatus() == GoalStatus.LATE)) {
            derived = GoalStatus.LATE;
        } else if (subs.stream().anyMatch(s ->
                s.getStatus() == GoalStatus.IN_PROGRESS || s.getStatus() == GoalStatus.DONE)) {
            derived = GoalStatus.IN_PROGRESS;
        } else {
            derived = GoalStatus.TODO;
        }
        parent.setStatus(derived);
    }

    private double calcProgressRecursive(Goal goal, int depthLevel, double[] weights) {
        if (goal.getSubGoals().isEmpty() || depthLevel > weights.length) {
            return goal.getProgress() * weights[Math.min(depthLevel - 1, weights.length - 1)];
        }
        double total = 0;
        double weightSum = 0;
        for (Goal sub : goal.getSubGoals()) {
            total += calcProgressRecursive(sub, depthLevel + 1, weights);
            weightSum += weights[Math.min(depthLevel, weights.length - 1)];
        }
        return weightSum > 0 ? total / weightSum : 0;
    }

    private int calcProgress(Goal g) {
        double[] weights = {1.0, 0.8, 0.6, 0.4};
        return (int) Math.round(calcProgressRecursive(g, 1, weights));
    }

    private GoalStatus calcStatus(Goal g) {
        if (g.getSubGoals().isEmpty()) return g.getStatus();
        List<Goal> subs = g.getSubGoals();
        if (subs.stream().allMatch(s -> s.getStatus() == GoalStatus.DONE)) return GoalStatus.DONE;
        if (subs.stream().anyMatch(s -> s.getStatus() == GoalStatus.LATE)) return GoalStatus.LATE;
        if (subs.stream().anyMatch(s ->
                s.getStatus() == GoalStatus.IN_PROGRESS || s.getStatus() == GoalStatus.DONE))
            return GoalStatus.IN_PROGRESS;
        return GoalStatus.TODO;
    }

    /**
     * 转为响应 DTO，如果 currentUserId 是学生则注入其个人进度
     */
    private GoalResponse toResponse(Goal g, Long currentUserId) {
        GoalResponse r = new GoalResponse();
        r.setId(g.getId());
        r.setTitle(g.getTitle());
        r.setDescription(g.getDescription());
        r.setStatus(calcStatus(g));
        r.setPlannedStart(g.getPlannedStart());
        r.setPlannedEnd(g.getPlannedEnd());
        r.setActualStart(g.getActualStart());
        r.setActualEnd(g.getActualEnd());
        r.setProgress(calcProgress(g));
        r.setOwners(g.getOwners());
        r.setDepth(g.getDepth());
        r.setCopyable(Boolean.TRUE.equals(g.getCopyable()));
        r.setParentId(g.getParent() != null ? g.getParent().getId() : null);
        r.setCreatedAt(g.getCreatedAt());
        r.setUpdatedAt(g.getUpdatedAt());
        List<Goal> subGoals = g.getSubGoals();
        if (subGoals == null || subGoals.isEmpty()) {
            r.setSubGoals(Collections.emptyList());
        } else {
            r.setSubGoals(subGoals.stream().map(s -> toResponse(s, currentUserId)).collect(Collectors.toList()));
        }
        if (g.getManager() != null) {
            r.setManagerId(g.getManager().getId());
            r.setManagerName(g.getManager().getRealName());
        }
        if (g.getAssignee() != null) {
            r.setAssigneeId(g.getAssignee().getId());
            r.setAssigneeName(g.getAssignee().getRealName());
        }
        if (g.getClassGroup() != null) {
            r.setClassGroupId(g.getClassGroup().getId());
            r.setClassGroupName(g.getClassGroup().getName());
        }
        // 多对多学生分配
        List<GoalAssignee> assignees = goalAssigneeRepository.findByGoal(g);
        if (assignees != null && !assignees.isEmpty()) {
            r.setAssigneeIds(new ArrayList<>());
            r.setAssigneeNames(new ArrayList<>());
            for (GoalAssignee ga : assignees) {
                if (ga.getStudent() != null) {
                    r.getAssigneeIds().add(ga.getStudent().getId());
                    r.getAssigneeNames().add(ga.getStudent().getRealName());
                }
            }
        } else {
            r.setAssigneeIds(Collections.emptyList());
            r.setAssigneeNames(Collections.emptyList());
        }
        // 关联作业
        List<GoalAssignment> goalAssignments = goalAssignmentRepository.findByGoal(g);
        if (goalAssignments != null && !goalAssignments.isEmpty()) {
            r.setAssignmentIds(goalAssignments.stream()
                    .map(GoalAssignment::getAssignmentId)
                    .collect(Collectors.toList()));
            // 作业标题需要单独查询（简化：只返回 ID，前端自己映射）
            r.setAssignmentTitles(goalAssignments.stream()
                    .map(ga -> "作业#" + ga.getAssignmentId())
                    .collect(Collectors.toList()));
        } else {
            r.setAssignmentIds(Collections.emptyList());
            r.setAssignmentTitles(Collections.emptyList());
        }
        // 注入当前用户的评论（仅公开评论）
        r.setCanComment(false);
        if (currentUserId != null) {
            User currentUser = userRepository.findById(currentUserId).orElse(null);
            if (currentUser != null) {
                if (currentUser.getRole() == User.Role.STUDENT) {
                    StudentGoalProgress sp = studentGoalProgressRepository
                            .findByGoalAndStudent(g, currentUser).orElse(null);
                    if (sp != null) {
                        r.setStudentProgress(sp.getProgress());
                        r.setStudentStatus(sp.getStatus().name());
                        r.setMyActualStart(sp.getActualStart());
                        r.setMyActualEnd(sp.getActualEnd());
                        r.setCanComment(true);
                    }
                    // 学生看到目标的所有公开评论（群聊）
                    List<GoalComment> publicComments = goalCommentRepository
                            .findByGoalAndVisibilityOrderByCreatedAtAsc(g, "PUBLIC");
                    if (!publicComments.isEmpty()) {
                        r.setComments(publicComments.stream()
                                .map(c -> toCommentResponse(c, currentUserId))
                                .collect(Collectors.toList()));
                    }
                } else if (currentUser.getRole() == User.Role.TEACHER || currentUser.getRole() == User.Role.ADMIN) {
                    // 老师和管理员：可以评论自己创建的目标
                    boolean isManager = g.getManager() != null && g.getManager().getId().equals(currentUserId);
                    if (isManager || currentUser.getRole() == User.Role.ADMIN) {
                        r.setCanComment(true);
                    }
                    // 加载所有公开评论
                    List<GoalComment> allComments = goalCommentRepository
                            .findByGoalAndVisibilityOrderByCreatedAtAsc(g, "PUBLIC");
                    if (!allComments.isEmpty()) {
                        r.setComments(allComments.stream()
                                .map(c -> toCommentResponse(c, currentUserId))
                                .collect(Collectors.toList()));
                    }
                }
            }
        }
        return r;
    }

    /**
     * 将 GoalComment 转为 CommentResponse
     */
    private CommentResponse toCommentResponse(GoalComment c, Long currentUserId) {
        CommentResponse r = new CommentResponse();
        r.setId(c.getId());
        r.setGoalId(c.getGoal().getId());
        r.setStudentId(c.getStudent() != null ? c.getStudent().getId() : null);
        r.setStudentName(c.getStudent() != null ? c.getStudent().getRealName() : null);
        // 作者信息
        r.setAuthorId(c.getAuthor().getId());
        r.setAuthorName(c.getAuthor().getRealName());
        r.setAuthorRole(c.getAuthorRole());
        r.setVisibility(c.getVisibility());
        r.setTargetStudentId(c.getTargetStudentId());
        r.setContent(c.getContent());
        // 解析 imageUrls JSON -> List<String>
        if (c.getImageUrls() != null && !c.getImageUrls().isEmpty()) {
            try {
                List<String> urls = JSON_MAPPER.readValue(c.getImageUrls(), new TypeReference<List<String>>() {});
                r.setImageUrls(urls);
            } catch (JsonProcessingException e) {
                r.setImageUrls(Collections.emptyList());
            }
        } else {
            r.setImageUrls(Collections.emptyList());
        }
        r.setCreatedAt(c.getCreatedAt());
        r.setUpdatedAt(c.getUpdatedAt());
        r.setOwn(currentUserId != null && c.getAuthor().getId().equals(currentUserId));
        return r;
    }

    private String encodeAttachments(List<String> urls, List<String> names) {
        if (urls == null || urls.isEmpty()) {
            return null;
        }
        try {
            List<String> encoded = new ArrayList<>();
            for (int i = 0; i < urls.size(); i++) {
                String url = urls.get(i);
                String name = (names != null && i < names.size() && names.get(i) != null)
                        ? names.get(i)
                        : extractFileNameFromUrl(url);
                // 编码格式: url::name，如果 name 为空则只存 url
                if (name != null && !name.isEmpty()) {
                    encoded.add(url + "::" + name);
                } else {
                    encoded.add(url);
                }
            }
            return JSON_MAPPER.writeValueAsString(encoded);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("附件数据解析失败", e);
        }
    }

    private String extractFileNameFromUrl(String url) {
        if (url == null || url.isEmpty()) return "";
        // 提取 URL 最后一段作为文件名
        int idx = url.lastIndexOf('/');
        return idx >= 0 ? url.substring(idx + 1) : url;
    }

    private Goal findOrThrow(Long id) {
        return goalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Goal not found: " + id));
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + userId));
    }
}