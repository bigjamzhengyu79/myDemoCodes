package com.example.controller;

import com.example.entity.ClassGroup;
import com.example.entity.User;
import com.example.service.ClassGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/class-groups")
@CrossOrigin(origins = "*")
public class ClassGroupController {

    @Autowired
    private ClassGroupService classGroupService;

    // ====== 班级基础 CRUD ======

    @GetMapping
    public ResponseEntity<List<ClassGroup>> getAllClassGroups() {
        return ResponseEntity.ok(classGroupService.getAllClassGroups());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClassGroup> getClassGroupById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(classGroupService.getClassGroupById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<?> createClassGroup(@RequestBody ClassGroup classGroup) {
        try {
            ClassGroup saved = classGroupService.createClassGroup(classGroup);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateClassGroup(@PathVariable Long id, @RequestBody ClassGroup classGroup) {
        try {
            return ResponseEntity.ok(classGroupService.updateClassGroup(id, classGroup));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClassGroup(@PathVariable Long id) {
        try {
            classGroupService.deleteClassGroup(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ====== 老师管理 ======

    /**
     * 设置班级负责老师
     * Request body: { "teacherId": 123 }
     * 传 null 或空表示取消老师
     */
    @PutMapping("/{id}/teacher")
    public ResponseEntity<?> setTeacher(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        try {
            Long teacherId = body.get("teacherId");
            ClassGroup updated = classGroupService.setTeacher(id, teacherId);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 获取指定老师负责的所有班级
     */
    @GetMapping("/by-teacher/{teacherId}")
    public ResponseEntity<List<ClassGroup>> getClassGroupsByTeacher(@PathVariable Long teacherId) {
        try {
            return ResponseEntity.ok(classGroupService.getClassGroupsByTeacher(teacherId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ====== 学生管理 ======

    /**
     * 获取班级中的所有学生
     */
    @GetMapping("/{id}/students")
    public ResponseEntity<List<User>> getStudents(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(classGroupService.getStudents(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 向班级添加学生
     * Request body: { "studentId": 456 }
     */
    @PostMapping("/{id}/students")
    public ResponseEntity<?> addStudent(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        try {
            Long studentId = body.get("studentId");
            if (studentId == null) {
                return ResponseEntity.badRequest().body("studentId is required");
            }
            ClassGroup updated = classGroupService.addStudent(id, studentId);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 从班级移除学生
     */
    @DeleteMapping("/{id}/students/{studentId}")
    public ResponseEntity<?> removeStudent(@PathVariable Long id, @PathVariable Long studentId) {
        try {
            classGroupService.removeStudent(id, studentId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 获取学生加入的所有班级
     */
    @GetMapping("/by-student/{studentId}")
    public ResponseEntity<List<ClassGroup>> getClassGroupsByStudent(@PathVariable Long studentId) {
        try {
            return ResponseEntity.ok(classGroupService.getClassGroupsByStudent(studentId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}