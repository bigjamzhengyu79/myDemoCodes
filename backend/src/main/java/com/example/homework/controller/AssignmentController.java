package com.example.homework.controller;

import com.example.homework.dto.ApiResponse;
import com.example.homework.dto.AssignmentDto;
import com.example.homework.service.AssignmentService;
import lombok.RequiredArgsConstructor;
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
