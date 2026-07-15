package com.example.goal.controller;

import com.example.goal.dto.GoalDto.*;
import com.example.goal.service.GoalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GoalController {

    private final GoalService goalService;

    /**
     * 从 Authentication 中提取当前用户 ID
     * SecurityConfig 中设置了 principal=username, credentials=userId
     */
    private Long getCurrentUserId(Authentication auth) {
        if (auth == null || auth.getCredentials() == null) return null;
        return (Long) auth.getCredentials();
    }

    private String getCurrentRole(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null || auth.getAuthorities().isEmpty()) return null;
        return auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
    }

    @GetMapping
    public ResponseEntity<List<GoalResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            Authentication auth) {
        Long userId = getCurrentUserId(auth);
        String role = getCurrentRole(auth);
        return ResponseEntity.ok(goalService.listParentGoals(status, keyword, userId, role));
    }

    @GetMapping("/sub-goals")
    public ResponseEntity<List<GoalResponse>> loadSubGoals(
            @RequestParam Long parentId,
            @RequestParam(required = false) Integer depth,
            Authentication auth) {
        Long userId = getCurrentUserId(auth);
        return ResponseEntity.ok(goalService.loadSubGoals(parentId, depth, userId));
    }

    @GetMapping("/stats")
    public ResponseEntity<GoalStatsResponse> stats(Authentication auth) {
        Long userId = getCurrentUserId(auth);
        String role = getCurrentRole(auth);
        return ResponseEntity.ok(goalService.getStats(userId, role));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GoalResponse> get(@PathVariable Long id, Authentication auth) {
        Long userId = getCurrentUserId(auth);
        return ResponseEntity.ok(goalService.getGoal(id, userId));
    }

    @PostMapping
    public ResponseEntity<GoalResponse> create(@RequestBody GoalRequest req, Authentication auth) {
        Long userId = getCurrentUserId(auth);
        return ResponseEntity.ok(goalService.createGoal(req, userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GoalResponse> update(
            @PathVariable Long id, @RequestBody GoalRequest req, Authentication auth) {
        Long userId = getCurrentUserId(auth);
        String role = getCurrentRole(auth);
        return ResponseEntity.ok(goalService.updateGoal(id, req, userId, role));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        Long userId = getCurrentUserId(auth);
        String role = getCurrentRole(auth);
        goalService.deleteGoal(id, userId, role);
        return ResponseEntity.noContent().build();
    }

    /**
     * 获取当前老师可复制目标列表
     */
    @GetMapping("/copyable")
    public ResponseEntity<List<GoalResponse>> listCopyable(Authentication auth) {
        Long userId = getCurrentUserId(auth);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(goalService.listCopyableGoals(userId));
    }

    /**
     * 切换目标的可复制标记（仅需 copyable 字段）
     */
    @PatchMapping("/{id}/copyable")
    public ResponseEntity<GoalResponse> toggleCopyable(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body,
            Authentication auth) {
        Long userId = getCurrentUserId(auth);
        String role = getCurrentRole(auth);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        Boolean copyable = body.get("copyable");
        if (copyable == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(goalService.toggleCopyable(id, userId, role, copyable));
    }

    // ====== 多对多学生分配 ======

    @GetMapping("/{id}/assignees")
    public ResponseEntity<List<Long>> getAssignees(@PathVariable Long id) {
        return ResponseEntity.ok(goalService.listAssignedStudentIds(id));
    }

    @PostMapping("/{id}/assignees")
    public ResponseEntity<Void> addAssignee(
            @PathVariable Long id,
            @RequestBody Map<String, Long> body) {
        Long studentId = body.get("studentId");
        if (studentId == null) {
            return ResponseEntity.badRequest().build();
        }
        goalService.assignStudent(id, studentId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/assignees/{studentId}")
    public ResponseEntity<Void> removeAssignee(
            @PathVariable Long id,
            @PathVariable Long studentId) {
        goalService.unassignStudent(id, studentId);
        return ResponseEntity.noContent().build();
    }

    // ====== 学生个人进度 ======

    /**
     * 学生获取自己的目标列表（分配给我的）
     */
    @GetMapping("/my")
    public ResponseEntity<List<GoalResponse>> listMyGoals(Authentication auth) {
        Long userId = getCurrentUserId(auth);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(goalService.listMyGoals(userId));
    }

    /**
     * 学生更新自己在某个目标上的个人进度
     */
    @PutMapping("/{id}/my-progress")
    public ResponseEntity<GoalResponse> updateMyProgress(
            @PathVariable Long id,
            @RequestBody StudentProgressUpdateRequest req,
            Authentication auth) {
        Long userId = getCurrentUserId(auth);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(goalService.updateMyProgress(id, userId, req));
    }

    // ====== 评论 ======

    /**
     * 获取某个目标的公开评论
     * - 学生：看到自己参与的公开评论
     * - 老师：看到所有公开评论
     */
    @GetMapping("/{id}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(
            @PathVariable Long id, Authentication auth) {
        Long userId = getCurrentUserId(auth);
        String role = getCurrentRole(auth);
        return ResponseEntity.ok(goalService.getComments(id, userId, role));
    }

    /**
     * 添加评论（公开或私密）
     * - 学生：仅可发公开评论（必须是被分配的目标）
     * - 老师：可发公开或私密评论（仅限自己创建的目标）
     */
    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long id,
            @RequestBody CommentRequest req,
            Authentication auth) {
        Long userId = getCurrentUserId(auth);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(goalService.addComment(id, userId, req));
    }

    /**
     * 编辑自己的评论
     */
    @PutMapping("/{id}/comments/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable Long id,
            @PathVariable Long commentId,
            @RequestBody CommentRequest req,
            Authentication auth) {
        Long userId = getCurrentUserId(auth);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(goalService.updateComment(commentId, userId, req));
    }

    /**
     * 删除自己的评论
     */
    @DeleteMapping("/{id}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long id,
            @PathVariable Long commentId,
            Authentication auth) {
        Long userId = getCurrentUserId(auth);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        goalService.deleteComment(commentId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 获取私密评论（仅老师或相关学生可见）
     * - 学生：获取老师发给自己的私密评论 + 自己的回复
     * - 老师：获取自己发给学生的私密评论
     */
    @GetMapping("/{id}/private-comments")
    public ResponseEntity<List<CommentResponse>> getPrivateComments(
            @PathVariable Long id, Authentication auth) {
        Long userId = getCurrentUserId(auth);
        String role = getCurrentRole(auth);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(goalService.getPrivateComments(id, userId, role));
    }

    // ====== 老师查看学生执行概览 ======

    /**
     * 老师获取某个目标下所有学生的执行情况（递归包含子目标）
     */
    @GetMapping("/{id}/student-overview")
    public ResponseEntity<GoalStudentOverviewResponse> getStudentOverview(
            @PathVariable Long id, Authentication auth) {
        Long userId = getCurrentUserId(auth);
        String role = getCurrentRole(auth);
        // 仅老师和管理员可查看
        if (userId == null || (!"TEACHER".equals(role) && !"ADMIN".equals(role))) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(goalService.getStudentOverview(id, userId));
    }

    /**
     * 复制目标树（递归包含所有子目标）
     */
    @PostMapping("/{id}/copy")
    public ResponseEntity<GoalResponse> copyGoal(@PathVariable Long id, Authentication auth) {
        Long userId = getCurrentUserId(auth);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(goalService.copyGoalTree(id, userId));
    }

    // ====== 关联作业管理 ======

    /**
     * 获取目标关联的作业 ID 列表
     */
    @GetMapping("/{id}/assignments")
    public ResponseEntity<List<Long>> getAssignments(@PathVariable Long id) {
        return ResponseEntity.ok(goalService.listAssignmentIds(id));
    }

    /**
     * 更新目标关联的作业
     */
    @PutMapping("/{id}/assignments")
    public ResponseEntity<Void> updateAssignments(
            @PathVariable Long id,
            @RequestBody Map<String, List<Long>> body) {
        List<Long> assignmentIds = body.get("assignmentIds");
        if (assignmentIds == null) {
            return ResponseEntity.badRequest().build();
        }
        goalService.updateAssignments(id, assignmentIds);
        return ResponseEntity.ok().build();
    }
}