package com.example.homework.controller;

import com.example.homework.dto.ApiResponse;
import com.example.homework.dto.AnswerDto;
import com.example.homework.service.AnswerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assignments/{assignmentId}/answers")
@RequiredArgsConstructor
public class AnswerController {

    private final AnswerService answerService;

    @PostMapping
    public ResponseEntity<ApiResponse<AnswerDto.Response>> submit(
            @PathVariable Long assignmentId,
            @RequestBody AnswerDto.SubmitRequest req,
            Authentication auth) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(answerService.submit(assignmentId, req, auth.getName())));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AnswerDto.Response>>> list(
            @PathVariable Long assignmentId, Authentication auth) {
        String role = auth.getAuthorities().iterator().next().getAuthority();
        List<AnswerDto.Response> list = role.contains("TEACHER")
                ? answerService.getByAssignment(assignmentId)
                : answerService.getByAssignmentAndStudent(assignmentId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> stats(
            @PathVariable Long assignmentId, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(answerService.getStudentStats(assignmentId, auth.getName())));
    }
}
