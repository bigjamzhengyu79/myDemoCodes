package com.example.dto;

import com.example.entity.User;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String realName;
    private User.Role role;
    private String avatarUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> classNames;
}