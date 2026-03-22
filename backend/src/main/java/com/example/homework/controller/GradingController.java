package com.example.homework.controller;

import com.example.homework.dto.ApiResponse;
import com.example.homework.dto.AnswerDto;
import com.example.homework.service.AnswerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/grading")
@RequiredArgsConstructor
public class GradingController {

    private final AnswerService answerService;

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<AnswerDto.Response>>> pending() {
        return ResponseEntity.ok(ApiResponse.ok(answerService.getPendingReview()));
    }

    @PostMapping("/grade")
    public ResponseEntity<ApiResponse<AnswerDto.Response>> grade(
            @RequestBody AnswerDto.GradeRequest req, Authentication auth) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(answerService.grade(req, auth.getName())));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }
}
