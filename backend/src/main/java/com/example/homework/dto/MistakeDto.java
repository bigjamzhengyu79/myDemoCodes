package com.example.homework.dto;

import com.example.homework.entity.MistakeNote;
import com.example.homework.entity.Question;
import com.example.homework.entity.StudentAnswer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 错题本 DTO。
 *
 * 与其他模块单参 from(Entity) 的约定略有不同：这里的工厂接受两个来源 ——
 * MistakeNote（收藏关系与学生笔记）和可为 null 的 StudentAnswer（作答情况）。
 * 后者是查询时实时关联出来的，未作答过的题就是 null。
 */
public class MistakeDto {

    /**
     * 列表项：不含选项、解析步骤与 base64 图片。
     * 理由同 QuestionDto.Summary —— 完整题目会把解析步骤的图片一并序列化（数 MB 级），
     * 列表场景不需要。
     */
    @Data
    public static class Item {
        private Long id;                  // mistake_notes.id
        private Long questionId;
        private String questionTitle;
        private String contentLatex;
        private String questionType;
        private Integer difficulty;
        private Integer totalScore;
        private List<QuestionDto.TagResp> knowledgeTags;
        private String mastery;
        private boolean hasNote;
        private Long sourceAssignmentId;
        private String sourceAssignmentTitle;
        private LocalDateTime createdAt;  // 收藏时间

        // ↓ 从 student_answers 实时关联出来的作答情况，没答过时全为 null
        private Integer score;
        private String answerStatus;
        /**
         * 可能为 null —— 只有教师手动批改（AnswerService.grade）才写 errorType，
         * 自动判分的选择题/填空题一律留 null。前端必须把 null 和 NONE 一起当「未分类」。
         */
        private String errorType;

        public static Item from(MistakeNote n, StudentAnswer sa, String assignmentTitle) {
            Item r = new Item();
            fill(r, n, sa, assignmentTitle);
            return r;
        }

        /** 供 Item 与 Detail 共用的公共字段填充 */
        protected static void fill(Item r, MistakeNote n, StudentAnswer sa, String assignmentTitle) {
            Question q = n.getQuestion();
            r.id = n.getId();
            r.questionId = q.getId();
            r.questionTitle = q.getTitle();
            r.contentLatex = q.getContentLatex();
            r.questionType = q.getQuestionType() != null ? q.getQuestionType().name() : null;
            r.difficulty = q.getDifficulty();
            r.totalScore = q.getTotalScore();
            r.knowledgeTags = q.getKnowledgeTags().stream()
                    .map(QuestionDto.TagResp::from).collect(Collectors.toList());
            r.mastery = n.getMastery() != null ? n.getMastery().name() : MistakeNote.Mastery.UNREVIEWED.name();
            r.hasNote = n.getNoteContent() != null && !n.getNoteContent().isBlank();
            r.sourceAssignmentId = n.getSourceAssignmentId();
            r.sourceAssignmentTitle = assignmentTitle;
            r.createdAt = n.getCreatedAt();
            if (sa != null) {
                r.score = sa.getScore();
                r.answerStatus = sa.getStatus() != null ? sa.getStatus().name() : null;
                r.errorType = sa.getErrorType() != null ? sa.getErrorType().name() : null;
            }
        }
    }

    /** 详情：在 Item 基础上加题目配图、我的作答、答案解析、我的订正 */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Detail extends Item {
        private String questionImageUrlsJson;
        private List<QuestionDto.OptionResp> options;

        // 我的作答
        private String answerContent;
        private String answerImageUrlsJson;
        private String feedback;

        // 答案与解析：仅在已判分时下发，否则为 null（见 MistakeService.shouldRevealAnswer）
        private String answerKey;
        private List<QuestionDto.StepResp> solutionSteps;
        /** 前端据此显示「答案将在教师批改后公开」的提示 */
        private boolean answerRevealed;

        // 我的订正
        private String noteContent;
        private String noteImageUrlsJson;
        private LocalDateTime noteUpdatedAt;

        public static Detail from(MistakeNote n, StudentAnswer sa, String assignmentTitle, boolean revealAnswer) {
            Detail r = new Detail();
            fill(r, n, sa, assignmentTitle);

            Question q = n.getQuestion();
            r.questionImageUrlsJson = q.getImageUrlsJson();
            r.options = q.getOptions().stream()
                    .map(QuestionDto.OptionResp::from).collect(Collectors.toList());

            if (sa != null) {
                r.answerContent = sa.getAnswerContent();
                r.answerImageUrlsJson = sa.getImageUrlsJson();
                r.feedback = sa.getFeedback();
            }

            r.answerRevealed = revealAnswer;
            if (revealAnswer) {
                r.answerKey = q.getAnswerKey();
                r.solutionSteps = q.getSolutionSteps().stream()
                        .map(QuestionDto.StepResp::from).collect(Collectors.toList());
            }

            r.noteContent = n.getNoteContent();
            r.noteImageUrlsJson = n.getImageUrlsJson();
            r.noteUpdatedAt = n.getUpdatedAt();
            return r;
        }
    }

    /** 做题页勾选收藏 */
    @Data
    public static class SaveRequest {
        private Long questionId;
        private Long sourceAssignmentId;   // 可为空
    }

    /** 写订正笔记 / 改掌握状态 */
    @Data
    public static class NoteRequest {
        private String noteContent;
        private String imageUrlsJson;
        private String mastery;            // 空则不改
    }
}
