package com.example.homework.controller;

import com.example.homework.dto.ApiResponse;
import com.example.homework.dto.QuestionDto;
import com.example.homework.repository.KnowledgeTagRepository;
import com.example.homework.service.QuestionService;
import lombok.RequiredArgsConstructor;
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

    @GetMapping
    public ResponseEntity<ApiResponse<List<QuestionDto.Response>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(questionService.listAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<QuestionDto.Response>> get(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(questionService.getById(id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<QuestionDto.Response>> create(
            @RequestBody QuestionDto.Request req, Authentication auth) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(questionService.create(req, auth.getName())));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<QuestionDto.Response>> update(
            @PathVariable Long id, @RequestBody QuestionDto.Request req) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(questionService.update(id, req)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        questionService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("删除成功", null));
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
