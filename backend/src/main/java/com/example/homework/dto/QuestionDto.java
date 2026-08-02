package com.example.homework.dto;

import com.example.entity.User;
import com.example.homework.entity.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class QuestionDto {

    @Data
    public static class Request {
        private String title;
        private String contentLatex;
        private String questionType;
        private Integer difficulty;
        private Integer totalScore;
        private String answerKey;
        private String source;
        private Long parentId;
        private String imageUrlsJson;          // JSON array of base64 data URLs
        private List<OptionReq> options;
        private List<StepReq> solutionSteps;
        private Set<Long> knowledgeTagIds;
        private String visibility;             // PUBLIC / SHARED / PRIVATE，为空时新建落 PUBLIC、更新不改
        private Set<Long> sharedUserIds;       // 仅 visibility=SHARED 时有意义
    }

    /** 管理员专用：指定共享范围与共享教师（PATCH /api/questions/{id}/shares） */
    @Data
    public static class ShareRequest {
        private String visibility;         // PUBLIC / SHARED / PRIVATE，为空视为 SHARED
        private Set<Long> sharedUserIds;   // null 表示不改动名单
    }

    @Data
    public static class OptionReq {
        private String optionLabel;
        private String contentLatex;
        private Boolean isCorrect;
    }

    @Data
    public static class StepReq {
        private Integer stepOrder;
        private String contentLatex;
        private Integer stepScore;
        private String commonErrors;
        private String imageUrlsJson;  // JSON array of base64 data URLs for step images
    }

    @Data
    public static class Response {
        private Long id;
        private String title;
        private String contentLatex;
        private String questionType;
        private Integer difficulty;
        private Integer totalScore;
        private String answerKey;
        private String source;
        private Long parentId;
        private String imageUrlsJson;
        private Long createdById;
        private String createdByName;
        private String visibility;
        private List<Long> sharedUserIds;      // 编辑页回填共享名单用
        private List<String> sharedUserNames;  // 教师列表尚未加载完时的兜底展示
        private LocalDateTime createdAt;
        private List<OptionResp> options;
        private List<StepResp> solutionSteps;
        private List<TagResp> knowledgeTags;

        public static Response from(Question q) {
            Response r = new Response();
            r.id = q.getId();
            r.title = q.getTitle();
            r.contentLatex = q.getContentLatex();
            r.questionType = q.getQuestionType().name();
            r.difficulty = q.getDifficulty();
            r.totalScore = q.getTotalScore();
            r.answerKey = q.getAnswerKey();
            r.source = q.getSource();
            r.parentId = q.getParentId();
            r.imageUrlsJson = q.getImageUrlsJson();
            r.createdById = q.getCreatedBy() != null ? q.getCreatedBy().getId() : null;
            r.createdByName = q.getCreatedBy() != null ? q.getCreatedBy().getRealName() : null;
            r.visibility = q.getVisibility() != null ? q.getVisibility().name() : "PUBLIC";
            r.sharedUserIds = q.getSharedWith().stream()
                    .map(User::getId).collect(Collectors.toList());
            r.sharedUserNames = q.getSharedWith().stream()
                    .map(QuestionDto::displayName).collect(Collectors.toList());
            r.createdAt = q.getCreatedAt();
            r.options = q.getOptions().stream().map(OptionResp::from).collect(Collectors.toList());
            r.solutionSteps = q.getSolutionSteps().stream().map(StepResp::from).collect(Collectors.toList());
            r.knowledgeTags = q.getKnowledgeTags().stream().map(TagResp::from).collect(Collectors.toList());
            return r;
        }
    }

    /**
     * 列表/选择器用的轻量视图：不含选项、解析步骤与 base64 图片。
     * 完整版 Response 会把每道题的解析步骤图片一并序列化（数 MB 级），列表场景不需要。
     */
    @Data
    public static class Summary {
        private Long id;
        private String title;
        private String contentLatex;
        private String questionType;
        private Integer difficulty;
        private Integer totalScore;
        private String source;
        private Long createdById;
        private String createdByName;
        private String visibility;
        private List<TagResp> knowledgeTags;

        // 刻意不暴露 sharedUserIds：那是 ToMany，列表场景下会引发真正的 N+1

        public static Summary from(Question q) {
            Summary r = new Summary();
            r.id = q.getId();
            r.title = q.getTitle();
            r.contentLatex = q.getContentLatex();
            r.questionType = q.getQuestionType() != null ? q.getQuestionType().name() : null;
            r.difficulty = q.getDifficulty();
            r.totalScore = q.getTotalScore();
            r.source = q.getSource();
            r.createdById = q.getCreatedBy() != null ? q.getCreatedBy().getId() : null;
            r.createdByName = q.getCreatedBy() != null ? displayName(q.getCreatedBy()) : null;
            r.visibility = q.getVisibility() != null ? q.getVisibility().name() : "PUBLIC";
            r.knowledgeTags = q.getKnowledgeTags().stream().map(TagResp::from).collect(Collectors.toList());
            return r;
        }
    }

    /** 展示名：优先真实姓名，缺失时回退用户名 */
    private static String displayName(User u) {
        if (u == null) return null;
        return (u.getRealName() != null && !u.getRealName().isBlank()) ? u.getRealName() : u.getUsername();
    }

    @Data
    public static class OptionResp {
        private Long id;
        private String optionLabel;
        private String contentLatex;
        private Boolean isCorrect;

        public static OptionResp from(QuestionOption o) {
            OptionResp r = new OptionResp();
            r.id = o.getId();
            r.optionLabel = o.getOptionLabel();
            r.contentLatex = o.getContentLatex();
            r.isCorrect = o.getIsCorrect();
            return r;
        }
    }

    @Data
    public static class StepResp {
        private Long id;
        private Integer stepOrder;
        private String contentLatex;
        private Integer stepScore;
        private String commonErrors;
        private String imageUrlsJson;  // JSON array of base64 data URLs for step images

        public static StepResp from(SolutionStep s) {
            StepResp r = new StepResp();
            r.id = s.getId();
            r.stepOrder = s.getStepOrder();
            r.contentLatex = s.getContentLatex();
            r.stepScore = s.getStepScore();
            r.commonErrors = s.getCommonErrors();
            r.imageUrlsJson = s.getImageUrlsJson();
            return r;
        }
    }

    @Data
    public static class TagResp {
        private Long id;
        private String name;
        private String chapter;

        public static TagResp from(KnowledgeTag t) {
            TagResp r = new TagResp();
            r.id = t.getId();
            r.name = t.getName();
            r.chapter = t.getChapter();
            return r;
        }
    }
}
