package com.example.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins = "*")
public class FileUploadController {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    /** 自定义文件上传大小限制，格式如 5MB、10MB、1024KB */
    @Value("${file.max-size:5MB}")
    private String maxSize;

    /** 解析后的最大字节数 */
    private long maxSizeBytes;

    private static final Pattern SIZE_PATTERN = Pattern.compile("^(\\d+)\\s*(B|KB|MB|GB)$", Pattern.CASE_INSENSITIVE);

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
        } catch (IOException e) {
            throw new RuntimeException("无法创建上传目录: " + uploadDir, e);
        }
        this.maxSizeBytes = parseSizeToBytes(maxSize);
    }

    /**
     * 解析大小字符串为字节数
     */
    private long parseSizeToBytes(String sizeStr) {
        if (sizeStr == null || sizeStr.isBlank()) {
            return 5 * 1024 * 1024L; // 默认 5MB
        }
        Matcher matcher = SIZE_PATTERN.matcher(sizeStr.trim());
        if (!matcher.matches()) {
            return 5 * 1024 * 1024L;
        }
        long value = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2).toUpperCase();
        return switch (unit) {
            case "B" -> value;
            case "KB" -> value * 1024;
            case "MB" -> value * 1024 * 1024;
            case "GB" -> value * 1024 * 1024 * 1024;
            default -> 5 * 1024 * 1024L;
        };
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "文件为空"));
        }

        // 校验文件大小
        if (file.getSize() > maxSizeBytes) {
            return ResponseEntity.status(413).body(Map.of("error",
                "文件大小超过限制（最大 " + maxSize + "）"));
        }

        try {
            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + extension;

            // 保存文件
            Path targetPath = Paths.get(uploadDir, filename);
            Files.copy(file.getInputStream(), targetPath);

            // 返回可访问的 URL 和原始文件名
            String fileUrl = "/uploads/" + filename;
            return ResponseEntity.ok(Map.of(
                "url", fileUrl,
                "originalName", originalFilename != null ? originalFilename : filename
            ));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "文件上传失败: " + e.getMessage()));
        }
    }
}