package com.example.homework.controller;

import com.example.homework.dto.ApiResponse;
import com.example.homework.dto.AuthDto;
import com.example.homework.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDto.LoginResponse>> login(@RequestBody AuthDto.LoginRequest req) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(authService.login(req)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        var user = authService.currentUser(auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
            "id", user.getId(),
            "username", user.getUsername(),
            "realName", user.getRealName(),
            "role", user.getRole().name(),
            "className", user.getClassName() != null ? user.getClassName() : ""
        )));
    }
}
