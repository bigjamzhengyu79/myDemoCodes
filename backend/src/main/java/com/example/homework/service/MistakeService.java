package com.example.homework.service;

import com.example.entity.User;
import com.example.homework.dto.MistakeDto;
import com.example.homework.dto.PageResponse;
import com.example.homework.entity.Assignment;
import com.example.homework.entity.MistakeNote;
import com.example.homework.entity.Question;
import com.example.homework.entity.StudentAnswer;
import com.example.homework.repository.AssignmentRepository;
import com.example.homework.repository.MistakeNoteRepository;
import com.example.homework.repository.QuestionRepository;
import com.example.homework.repository.StudentAnswerRepository;
import com.example.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 错题本服务。
 *
 * 【核心前提，改动前务必读】
 * 错题本【完全由学生手动勾选】产生。系统不自动收录任何题目，也不看学生答得对不对 ——
 * 需求原话："说是错题，实际上是做过习题的保存，只要学生勾选，就能把目标习题移入错题本"。
 * 所以本类里没有、也不应该有任何形如 score < totalScore 的自动判定逻辑。
 *
 * 学生的作答情况是查询时从 student_answers 实时关联的，不做快照 ——
 * 教师重新批改会改分，快照会立刻与成绩单不一致。
 *
 * 【越权防护】
 * studentId 永不从客户端接受，一律由 Controller 从 JWT 取出后传入，
 * 且所有查询都带 student.id = :studentId 谓词。凡是接受客户端 ID 的方法
 * （getDetail 收 noteId），必须在加载后再校验一次归属。
 */
@Service
@RequiredArgsConstructor
public class MistakeService {

    private final MistakeNoteRepository noteRepository;
    private final StudentAnswerRepository answerRepository;
    private final AssignmentRepository assignmentRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;

    /* ==================== 查询 ==================== */

    /**
     * 分页列表。
     * 事务必需：Item.from 会读取 LAZY 的 question.knowledgeTags。
     */
    @Transactional(readOnly = true)
    public PageResponse<MistakeDto.Item> list(Long studentId, String questionType, String mastery,
                                              Long tagId, Pageable pageable) {
        Page<Long> idPage = noteRepository.findNoteIds(
                studentId, parseQuestionType(questionType), parseMastery(mastery), tagId, pageable);
        if (idPage.isEmpty()) {
            // 空集合传给 IN 会生成 `in ()` —— MySQL 语法错误，必须在这里短路
            return PageResponse.of(List.of(), idPage);
        }

        List<MistakeNote> notes = orderByIds(
                noteRepository.findByIdsWithQuestion(idPage.getContent()), idPage.getContent());

        List<Long> questionIds = notes.stream()
                .map(n -> n.getQuestion().getId()).collect(Collectors.toList());
        Map<Long, StudentAnswer> latestAnswers = latestAnswersByQuestion(studentId, questionIds);
        Map<Long, String> assignmentTitles = assignmentTitles(notes);

        List<MistakeDto.Item> content = notes.stream()
                .map(n -> MistakeDto.Item.from(
                        n,
                        latestAnswers.get(n.getQuestion().getId()),
                        assignmentTitles.get(n.getSourceAssignmentId())))
                .collect(Collectors.toList());
        return PageResponse.of(content, idPage);
    }

    /**
     * 单条详情。noteId 来自客户端，必须校验归属。
     * 事务必需：读 LAZY 的 options / solutionSteps / knowledgeTags。
     */
    @Transactional(readOnly = true)
    public MistakeDto.Detail getDetail(Long noteId, Long studentId) {
        MistakeNote note = noteRepository.findDetailById(noteId)
                .orElseThrow(() -> new RuntimeException("收藏不存在"));
        // mistake_notes.id 是自增序列，可枚举 —— 没有这一行就是 IDOR
        if (!studentId.equals(note.getStudent().getId())) {
            throw new RuntimeException("无权查看此收藏");
        }
        return buildDetail(note, studentId);
    }

    /** 做题页批量回填星标状态 */
    @Transactional(readOnly = true)
    public List<Long> collectedIn(List<Long> questionIds, Long studentId) {
        if (questionIds == null || questionIds.isEmpty()) return List.of();
        return noteRepository.findCollectedQuestionIds(studentId, questionIds);
    }

    /** 列表页头部的「共 N 题 · 已掌握 M 题」 */
    @Transactional(readOnly = true)
    public Map<String, Long> summary(Long studentId) {
        Map<String, Long> m = new LinkedHashMap<>();
        m.put("total", noteRepository.countByStudentId(studentId));
        m.put("mastered", noteRepository.countByStudentIdAndMastery(studentId, MistakeNote.Mastery.MASTERED));
        return m;
    }

    /* ==================== 写入 ==================== */

    /**
     * 收藏一道题。幂等 —— 已收藏时直接返回既有记录，不报错。
     * 前端星标可能被重复点击（网络慢时），失败反而更难处理。
     */
    @Transactional
    public MistakeDto.Detail add(MistakeDto.SaveRequest req, Long studentId) {
        if (req == null || req.getQuestionId() == null) {
            throw new RuntimeException("缺少题目 ID");
        }
        MistakeNote note = noteRepository
                .findByStudentIdAndQuestionId(studentId, req.getQuestionId())
                .orElseGet(MistakeNote::new);

        if (note.getId() == null) {
            User student = userRepository.findById(studentId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));
            Question question = questionRepository.findById(req.getQuestionId())
                    .orElseThrow(() -> new RuntimeException("题目不存在"));
            note.setStudent(student);
            note.setQuestion(question);
            note.setSourceAssignmentId(req.getSourceAssignmentId());
            note = noteRepository.save(note);
        }
        return buildDetail(note, studentId);
    }

    /** 取消收藏。按 questionId 删除 —— 做题页只知道题目 ID，不知道 note ID。 */
    @Transactional
    public void remove(Long questionId, Long studentId) {
        noteRepository.findByStudentIdAndQuestionId(studentId, questionId)
                .ifPresent(noteRepository::delete);
        // 找不到时静默返回：取消一个本就不在收藏里的题，结果与预期一致，不该报错
    }

    /**
     * 写订正笔记 / 改掌握状态。
     * 只能改自己已收藏的题 —— 否则学生可以对任意 questionId 写入，
     * 把这个接口当成题目存在性的探测器，并悄悄堆积垃圾行。
     */
    @Transactional
    public MistakeDto.Detail saveNote(Long questionId, MistakeDto.NoteRequest req, Long studentId) {
        MistakeNote note = noteRepository.findByStudentIdAndQuestionId(studentId, questionId)
                .orElseThrow(() -> new RuntimeException("该题不在你的错题本中"));

        if (req.getNoteContent() != null) note.setNoteContent(req.getNoteContent());
        if (req.getImageUrlsJson() != null) note.setImageUrlsJson(req.getImageUrlsJson());
        if (req.getMastery() != null && !req.getMastery().isBlank()) {
            MistakeNote.Mastery m = parseMastery(req.getMastery());
            if (m == null) throw new RuntimeException("无效的掌握状态: " + req.getMastery());
            note.setMastery(m);
        }
        return buildDetail(noteRepository.save(note), studentId);
    }

    /* ==================== 内部 ==================== */

    /** 组装详情。调用方需保证已在事务内且已完成归属校验。 */
    private MistakeDto.Detail buildDetail(MistakeNote note, Long studentId) {
        StudentAnswer sa = latestAnswersByQuestion(studentId, List.of(note.getQuestion().getId()))
                .get(note.getQuestion().getId());
        String title = note.getSourceAssignmentId() == null ? null
                : assignmentRepository.findById(note.getSourceAssignmentId())
                        .map(Assignment::getTitle).orElse(null);
        return MistakeDto.Detail.from(note, sa, title, shouldRevealAnswer(sa));
    }

    /**
     * 是否向学生下发 answerKey 与 solutionSteps。
     * 规则：该题的作答已判分（AUTO_GRADED / REVIEWED）才下发。
     *
     * 不采用「作业 CLOSED 后下发」：Assignment.Status.CLOSED 虽在枚举里，
     * 但 AssignmentService 没有任何转换到 CLOSED 的方法（只有 publish()），
     * 该条件永远不成立，会导致答案永不可见。
     *
     * 另注：AssignmentController.get 今天已经把 answerKey 连同题目一起下发给学生了
     * （见 QuestionDto.Response.from，且该路径是 permitAll），做题页只是没渲染。
     * 所以这里不是新增暴露面，而是把既有的暴露收敛到一个明确的条件下。
     */
    private boolean shouldRevealAnswer(StudentAnswer sa) {
        return sa != null
                && (sa.getStatus() == StudentAnswer.Status.AUTO_GRADED
                 || sa.getStatus() == StudentAnswer.Status.REVIEWED);
    }

    /**
     * 按 questionId 取该学生最新的一条作答。
     * 同一题可能在多份作业里都答过，查询已按 submittedAt 倒序，
     * 这里用 (a, b) -> a 保留先出现的那条（即最新的）。
     */
    private Map<Long, StudentAnswer> latestAnswersByQuestion(Long studentId, List<Long> questionIds) {
        if (questionIds.isEmpty()) return Map.of();
        return answerRepository.findByStudentIdAndQuestionIdIn(studentId, questionIds).stream()
                .collect(Collectors.toMap(
                        sa -> sa.getQuestion().getId(),
                        Function.identity(),
                        (a, b) -> a));
    }

    /** 批量解析来源作业标题，避免每行一次查询 */
    private Map<Long, String> assignmentTitles(List<MistakeNote> notes) {
        List<Long> ids = notes.stream()
                .map(MistakeNote::getSourceAssignmentId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) return Map.of();
        return assignmentRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Assignment::getId, Assignment::getTitle, (a, b) -> a));
    }

    /**
     * 把 fetch join 取回的结果按 ID 页的顺序重排。
     * `WHERE id IN (...)` 不保证返回顺序，而列表是按 createdAt DESC 排的 ——
     * 少了这一步，翻页时的顺序会变成数据库的物理顺序。
     */
    private List<MistakeNote> orderByIds(List<MistakeNote> notes, List<Long> ids) {
        Map<Long, MistakeNote> byId = notes.stream()
                .collect(Collectors.toMap(MistakeNote::getId, Function.identity(), (a, b) -> a));
        return ids.stream().map(byId::get).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /** 无效值一律视为不过滤，避免前端传错参数直接 500 —— 同 QuestionService 的处理 */
    private Question.QuestionType parseQuestionType(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Question.QuestionType.valueOf(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private MistakeNote.Mastery parseMastery(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return MistakeNote.Mastery.valueOf(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
