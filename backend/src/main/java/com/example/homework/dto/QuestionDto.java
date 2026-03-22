package com.example.homework.dto;

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
        private List<OptionReq> options;
        private List<StepReq> solutionSteps;
        private Set<Long> knowledgeTagIds;
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
        private String createdByName;
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
            r.createdByName = q.getCreatedBy() != null ? q.getCreatedBy().getRealName() : null;
            r.createdAt = q.getCreatedAt();
            r.options = q.getOptions().stream().map(OptionResp::from).collect(Collectors.toList());
            r.solutionSteps = q.getSolutionSteps().stream().map(StepResp::from).collect(Collectors.toList());
            r.knowledgeTags = q.getKnowledgeTags().stream().map(TagResp::from).collect(Collectors.toList());
            return r;
        }
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

        public static StepResp from(SolutionStep s) {
            StepResp r = new StepResp();
            r.id = s.getId();
            r.stepOrder = s.getStepOrder();
            r.contentLatex = s.getContentLatex();
            r.stepScore = s.getStepScore();
            r.commonErrors = s.getCommonErrors();
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
