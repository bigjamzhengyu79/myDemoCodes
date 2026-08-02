package com.example.controller;

import com.example.dto.UserResponse;
import com.example.entity.User;
import com.example.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // 与 GoalController / QuestionController 中的同名方法一致（有意重复：三者分属不同包，
    // 现在抽取公共基类要动三个 controller 且无功能收益）。
    private String getCurrentRole(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null || auth.getAuthorities().isEmpty()) return null;
        return auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
    }

    /**
     * 用户列表按调用者角色裁剪：
     *   ADMIN            → 全部（用户管理、班级管理的教师/学生选择器依赖全量）
     *   TEACHER          → 仅学生（目标分配的学生选择器要用；教师看不到教师名册）
     *   STUDENT          → 空
     *   无 token（null） → 仅学生
     *
     * null 不返回全量：本路径在 SecurityConfig 中是 permitAll，
     * 若无 token 就给全量，则任何人 curl 一下即可拿到完整名册，等于没有限制。
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers(Authentication auth) {
        String role = getCurrentRole(auth);
        List<UserResponse> all = userService.getAllUsersWithClasses();

        if ("ADMIN".equals(role)) {
            return ResponseEntity.ok(all);
        }
        if ("STUDENT".equals(role)) {
            return ResponseEntity.ok(List.of());
        }
        // TEACHER 与无 token：只给学生
        return ResponseEntity.ok(all.stream()
                .filter(u -> u.getRole() == User.Role.STUDENT)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User createdUser = userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        try {
            User updatedUser = userService.updateUser(id, user);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "User deleted successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "ok");
        return ResponseEntity.ok(response);
    }
}
