package com.example.homework.controller;

import com.example.homework.dto.ApiResponse;
import com.example.homework.dto.AssignmentDto;
import com.example.homework.dto.PageResponse;
import com.example.homework.entity.Assignment;
import com.example.homework.service.AssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AssignmentDto.Response>>> list(Authentication auth) {
        String role = auth.getAuthorities().iterator().next().getAuthority();
        List<AssignmentDto.Response> list = role.contains("TEACHER")
                ? assignmentService.listByTeacher(auth.getName())
                : assignmentService.listForStudent(auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    /**
     * 作业选择器专用：分页 + 关键词 / 班级 / 进行中筛选。
     *
     * 仅老师使用（目标关联作业时挑选），学生走上面的 list()。
     * status 默认 PUBLISHED —— 草稿不该出现在关联候选里；
     * 传空串可显式取消状态过滤。
     */
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponse<AssignmentDto.Response>>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long classGroupId,
            @RequestParam(required = false, defaultValue = "PUBLISHED") String status,
            @RequestParam(required = false, defaultValue = "false") boolean onlyOngoing,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {

        int safePage = Math.max(0, page);
        // 钉死上限，避免客户端一次性索取全量（同 QuestionController.listSummaryPaged）
        int safeSize = Math.min(Math.max(1, size), 100);

        Assignment.Status st = null;
        if (status != null && !status.isBlank()) {
            try {
                st = Assignment.Status.valueOf(status);
            } catch (IllegalArgumentException e) {
                // 无效状态视为不过滤，避免 500（同题库选择器对无效题型的处理）
            }
        }

        return ResponseEntity.ok(ApiResponse.ok(assignmentService.listForPicker(
                auth.getName(), keyword, classGroupId, st, onlyOngoing,
                PageRequest.of(safePage, safeSize))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        try {
            Map<String, Object> resp = assignmentService.getAssignmentDetail(id);
            return ResponseEntity.ok(ApiResponse.ok(resp));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AssignmentDto.Response>> create(
            @RequestBody AssignmentDto.Request req, Authentication auth) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(assignmentService.create(req, auth.getName())));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @PatchMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<AssignmentDto.Response>> publish(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(assignmentService.publish(id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }
}
