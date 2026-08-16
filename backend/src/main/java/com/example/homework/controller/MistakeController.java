package com.example.homework.controller;

import com.example.homework.dto.ApiResponse;
import com.example.homework.dto.MistakeDto;
import com.example.homework.dto.PageResponse;
import com.example.homework.service.MistakeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * 错题本（学生手动收藏的题目）。
 *
 * 【与相邻模块的两点差异，改动前注意】
 *
 * 1. 本路径【不在】SecurityConfig 的 permitAll 列表里，会落到 .anyRequest().authenticated()。
 *    这是刻意的：收藏本没有 studentId 参数（学生身份完全由 JWT 决定），匿名请求没有意义。
 *    请不要"为了与 /api/questions、/api/assignments 保持一致"把它加进 permitAll ——
 *    那几个是历史遗留，其中 /api/assignments 的 permitAll 正是 answerKey 泄露给匿名调用方的原因。
 *
 * 2. 因此前端拿到的是 403 而非 {success:false}，promise 会 reject 进 catch。
 *    前端两个视图的 catch 需要渲染通用错误文案。
 */
@RestController
@RequestMapping("/api/mistakes")
@RequiredArgsConstructor
public class MistakeController {

    private final MistakeService mistakeService;

    /**
     * 从 Authentication 中提取当前用户 ID
     * SecurityConfig 中设置了 principal=username, credentials=userId
     *
     * 与 QuestionController / GoalController / UserController 中的同名方法一致
     * （有意重复：四者分属不同模块，抽公共基类要动四个 controller 且无功能收益）。
     *
     * 注意 AnswerController 走的是另一条路 —— 它用 auth.getName() 拿用户名再查库。
     * 这里选 getCredentials()：错题本每次请求都要按学生过滤，省掉一次 users 查询。
     */
    private Long getCurrentUserId(Authentication auth) {
        if (auth == null || auth.getCredentials() == null) return null;
        return (Long) auth.getCredentials();
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<MistakeDto.Item>>> list(
            @RequestParam(required = false) String questionType,
            @RequestParam(required = false) String mastery,
            @RequestParam(required = false) Long tagId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        Long studentId = getCurrentUserId(auth);
        if (studentId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("请先登录"));
        }
        try {
            int safePage = Math.max(0, page);
            int safeSize = Math.min(Math.max(1, size), 100);   // 钉死上限，避免客户端一次性索取全量
            return ResponseEntity.ok(ApiResponse.ok(mistakeService.list(
                    studentId, questionType, mastery, tagId, PageRequest.of(safePage, safeSize))));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    /** 列表页头部的「共 N 题 · 已掌握 M 题」 */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Long>>> summary(Authentication auth) {
        Long studentId = getCurrentUserId(auth);
        if (studentId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("请先登录"));
        }
        try {
            return ResponseEntity.ok(ApiResponse.ok(mistakeService.summary(studentId)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    /**
     * 做题页批量回填星标状态。
     * 放在 /{id} 之前 —— 否则 "collected" 会被当成路径变量去匹配 /{id}。
     */
    @GetMapping("/collected")
    public ResponseEntity<ApiResponse<List<Long>>> collected(
            @RequestParam(required = false) List<Long> questionIds, Authentication auth) {
        Long studentId = getCurrentUserId(auth);
        if (studentId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("请先登录"));
        }
        try {
            return ResponseEntity.ok(ApiResponse.ok(mistakeService.collectedIn(questionIds, studentId)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MistakeDto.Detail>> get(
            @PathVariable Long id, Authentication auth) {
        Long studentId = getCurrentUserId(auth);
        if (studentId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("请先登录"));
        }
        try {
            return ResponseEntity.ok(ApiResponse.ok(mistakeService.getDetail(id, studentId)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    /** 收藏一道题（幂等，重复收藏不报错） */
    @PostMapping
    public ResponseEntity<ApiResponse<MistakeDto.Detail>> add(
            @RequestBody MistakeDto.SaveRequest req, Authentication auth) {
        Long studentId = getCurrentUserId(auth);
        if (studentId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("请先登录"));
        }
        try {
            return ResponseEntity.ok(ApiResponse.ok("已加入错题本", mistakeService.add(req, studentId)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    /** 按 questionId 而非 note id 取消收藏：做题页只知道题目 ID */
    @DeleteMapping("/questions/{questionId}")
    public ResponseEntity<ApiResponse<Void>> remove(
            @PathVariable Long questionId, Authentication auth) {
        Long studentId = getCurrentUserId(auth);
        if (studentId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("请先登录"));
        }
        try {
            mistakeService.remove(questionId, studentId);
            return ResponseEntity.ok(ApiResponse.ok("已移出错题本", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    /**
     * 写订正笔记 / 改掌握状态。
     * 用 PUT 而非 POST：这是按 (student, question) 键的幂等 upsert，
     * 客户端不知道也不关心记录是否已存在。
     */
    @PutMapping("/questions/{questionId}/note")
    public ResponseEntity<ApiResponse<MistakeDto.Detail>> saveNote(
            @PathVariable Long questionId,
            @RequestBody MistakeDto.NoteRequest req,
            Authentication auth) {
        Long studentId = getCurrentUserId(auth);
        if (studentId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("请先登录"));
        }
        try {
            return ResponseEntity.ok(ApiResponse.ok("已保存", mistakeService.saveNote(questionId, req, studentId)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }
}
