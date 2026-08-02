package com.example.homework.controller;

import com.example.homework.dto.ApiResponse;
import com.example.homework.dto.PageResponse;
import com.example.homework.dto.QuestionDto;
import com.example.homework.repository.KnowledgeTagRepository;
import com.example.homework.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;
    private final KnowledgeTagRepository tagRepository;

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
    public ResponseEntity<ApiResponse<List<QuestionDto.Response>>> list(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(
                questionService.listAll(getCurrentUserId(auth), getCurrentRole(auth))));
    }

    // 轻量列表：供题库管理页使用，避免传输解析步骤中的 base64 图片
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<List<QuestionDto.Summary>>> listSummary(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(
                questionService.listSummary(getCurrentUserId(auth), getCurrentRole(auth))));
    }

    /**
     * 分页 + 筛选的轻量列表，供作业的题目选择器使用。
     * 题库规模增大后，选择器每次只取一页，不再拉全量。
     */
    @GetMapping("/summary/page")
    public ResponseEntity<ApiResponse<PageResponse<QuestionDto.Summary>>> listSummaryPaged(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String questionType,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) Long tagId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication auth) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 200);   // 钉死上限，避免客户端一次性索取全量
        return ResponseEntity.ok(ApiResponse.ok(
                questionService.listSummaryPaged(keyword, questionType, difficulty, tagId,
                        PageRequest.of(safePage, safeSize),
                        getCurrentUserId(auth), getCurrentRole(auth))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<QuestionDto.Response>> get(
            @PathVariable Long id, Authentication auth) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(
                    questionService.getById(id, getCurrentUserId(auth), getCurrentRole(auth))));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<QuestionDto.Response>> create(
            @RequestBody QuestionDto.Request req, Authentication auth) {
        // 本路径在 SecurityConfig 中是 permitAll，auth 可能为 null —— 需要作成者才能建题
        if (auth == null) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("请先登录"));
        }
        try {
            return ResponseEntity.ok(ApiResponse.ok(
                    questionService.create(req, auth.getName(), getCurrentRole(auth))));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<QuestionDto.Response>> update(
            @PathVariable Long id, @RequestBody QuestionDto.Request req, Authentication auth) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(
                    questionService.update(id, req, getCurrentUserId(auth), getCurrentRole(auth))));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id, Authentication auth) {
        try {
            questionService.delete(id, getCurrentUserId(auth), getCurrentRole(auth));
            return ResponseEntity.ok(ApiResponse.ok("删除成功", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    /**
     * 管理员专用：指定共享范围与共享教师。
     * 教师侧没有此入口，后端亦拒绝非 ADMIN 调用（见 QuestionService.setShares）。
     * 单独开端点而不复用 PUT：PUT 走的是"仅作成者"的归属判定，
     * 而共享指定是按角色的另一条权限轴，混在一处会让判定难以审计。
     */
    @PatchMapping("/{id}/shares")
    public ResponseEntity<ApiResponse<QuestionDto.Response>> updateShares(
            @PathVariable Long id, @RequestBody QuestionDto.ShareRequest req, Authentication auth) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(
                    questionService.setShares(id, req, getCurrentRole(auth))));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @GetMapping("/tags")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTags() {
        List<Map<String, Object>> tags = tagRepository.findAll().stream()
                .map(t -> Map.<String, Object>of(
                    "id", t.getId(),
                    "name", t.getName(),
                    "chapter", t.getChapter() != null ? t.getChapter() : "",
                    "parentId", t.getParentId() != null ? t.getParentId() : 0
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(tags));
    }
}
