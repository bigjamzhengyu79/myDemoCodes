package com.example.service;

import com.example.dto.UserResponse;
import com.example.entity.ClassGroup;
import com.example.entity.User;
import com.example.repository.ClassGroupRepository;
import com.example.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClassGroupRepository classGroupRepository;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * 获取所有用户及其关联班级名称
     */
    public List<UserResponse> getAllUsersWithClasses() {
        List<User> users = userRepository.findAll();

        // 收集所有学生ID
        List<Long> studentIds = users.stream()
                .filter(u -> u.getRole() == User.Role.STUDENT)
                .map(User::getId)
                .collect(Collectors.toList());

        // 批量查询学生 -> 班级名称映射
        Map<Long, List<String>> studentClassMap = new HashMap<>();
        if (!studentIds.isEmpty()) {
            List<Object[]> studentRows = classGroupRepository.findStudentClassNames(studentIds);
            for (Object[] row : studentRows) {
                Long sid = ((Number) row[0]).longValue();
                String cgName = (String) row[1];
                studentClassMap.computeIfAbsent(sid, k -> new ArrayList<>()).add(cgName);
            }
        }

        // 收集所有老师ID
        List<Long> teacherIds = users.stream()
                .filter(u -> u.getRole() == User.Role.TEACHER || u.getRole() == User.Role.ADMIN)
                .map(User::getId)
                .collect(Collectors.toList());

        // 批量查询老师 -> 班级名称映射
        Map<Long, List<String>> teacherClassMap = new HashMap<>();
        if (!teacherIds.isEmpty()) {
            List<Object[]> teacherRows = classGroupRepository.findTeacherClassNames(teacherIds);
            for (Object[] row : teacherRows) {
                Long tid = ((Number) row[0]).longValue();
                String cgName = (String) row[1];
                teacherClassMap.computeIfAbsent(tid, k -> new ArrayList<>()).add(cgName);
            }
        }

        // 组装 DTO
        return users.stream().map(user -> {
            UserResponse r = new UserResponse();
            r.setId(user.getId());
            r.setUsername(user.getUsername());
            r.setEmail(user.getEmail());
            r.setRealName(user.getRealName());
            r.setRole(user.getRole());
            r.setAvatarUrl(user.getAvatarUrl());
            r.setCreatedAt(user.getCreatedAt());
            r.setUpdatedAt(user.getUpdatedAt());

            List<String> names;
            if (user.getRole() == User.Role.STUDENT) {
                names = studentClassMap.getOrDefault(user.getId(), Collections.emptyList());
            } else {
                names = teacherClassMap.getOrDefault(user.getId(), Collections.emptyList());
            }
            r.setClassNames(names);
            return r;
        }).collect(Collectors.toList());
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public User createUser(User user) {
        return userRepository.save(user);
    }

    public User updateUser(Long id, User user) {
        return userRepository.findById(id).map(existingUser -> {
            existingUser.setUsername(user.getUsername());
            existingUser.setEmail(user.getEmail());
            existingUser.setPassword(user.getPassword());
            existingUser.setRealName(user.getRealName());
            existingUser.setRole(user.getRole());
            return userRepository.save(existingUser);
        }).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}