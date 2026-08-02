package com.example.homework.service;

import com.example.entity.User;
import com.example.homework.dto.PageResponse;
import com.example.homework.dto.QuestionDto;
import com.example.homework.entity.*;
import com.example.homework.repository.*;
import com.example.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final KnowledgeTagRepository tagRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<QuestionDto.Response> listAll(Long userId, String role) {
        return questionRepository.findAll().stream()
                .filter(q -> canView(q, userId, role))
                .map(QuestionDto.Response::from)
                .collect(Collectors.toList());
    }

    // 题库列表与题目选择器用的轻量列表，不含选项、解析步骤与图片
    @Transactional(readOnly = true)
    public List<QuestionDto.Summary> listSummary(Long userId, String role) {
        return questionRepository.findAllForSummaryVisibleTo(
                        viewerFilter(userId, role),
                        Question.Visibility.PUBLIC, Question.Visibility.SHARED).stream()
                .map(QuestionDto.Summary::from)
                .collect(Collectors.toList());
    }

    /**
     * 分页 + 筛选的轻量列表，供题目选择器使用。
     * 关键词若为纯数字或 #数字，先按题目 ID 精确匹配（保留"直接输编号"的老习惯）；
     * 匹配不到再按文本搜索。
     */
    @Transactional(readOnly = true)
    public PageResponse<QuestionDto.Summary> listSummaryPaged(
            String keyword, String questionType, Integer difficulty, Long tagId, Pageable pageable,
            Long userId, String role) {

        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();

        // "#12" / "12" → 优先按 ID 精确查找
        if (kw != null) {
            String idPart = kw.startsWith("#") ? kw.substring(1) : kw;
            if (idPart.matches("\\d+")) {
                // 无权查看的题目在此被滤掉，避免通过输入编号探测他人的私有题；
                // 滤空后继续走下面的文本搜索，与"编号查不到"的既有行为一致
                List<QuestionDto.Summary> hit = questionRepository
                        .findByIdWithTags(Long.valueOf(idPart)).stream()
                        .filter(q -> canView(q, userId, role))
                        .map(QuestionDto.Summary::from)
                        .collect(Collectors.toList());
                if (!hit.isEmpty()) {
                    return PageResponse.of(hit, new PageImpl<>(hit, pageable, hit.size()));
                }
                // 查不到该编号时，继续按文本搜索（例如搜 "2" 想找正文含 2 的题）
            }
        }

        Question.QuestionType type = null;
        if (questionType != null && !questionType.isBlank()) {
            try {
                type = Question.QuestionType.valueOf(questionType);
            } catch (IllegalArgumentException e) {
                // 无效题型视为不过滤，避免 500
            }
        }

        Page<Long> idPage = questionRepository.findIdsForSummary(
                kw, type, difficulty, tagId,
                viewerFilter(userId, role), Question.Visibility.PUBLIC, Question.Visibility.SHARED,
                pageable);
        if (idPage.isEmpty()) {
            return PageResponse.of(List.of(), idPage);
        }

        List<QuestionDto.Summary> content = questionRepository
                .findByIdsWithTags(idPage.getContent()).stream()
                .map(QuestionDto.Summary::from)
                .collect(Collectors.toList());
        return PageResponse.of(content, idPage);
    }

    // 事务必需：Response.from 会读取 LAZY 的 sharedWith / createdBy
    @Transactional(readOnly = true)
    public QuestionDto.Response getById(Long id, Long userId, String role) {
        Question q = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("题目不存在: " + id));
        if (!canView(q, userId, role)) {
            throw new RuntimeException("无权查看此题目");
        }
        return QuestionDto.Response.from(q);
    }

    @Transactional
    public QuestionDto.Response create(QuestionDto.Request req, String username, String role) {
        User creator = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Question q = new Question();
        q.setTitle(req.getTitle());
        q.setContentLatex(req.getContentLatex());
        q.setQuestionType(Question.QuestionType.valueOf(req.getQuestionType()));
        q.setDifficulty(req.getDifficulty() != null ? req.getDifficulty() : 3);
        q.setTotalScore(req.getTotalScore() != null ? req.getTotalScore() : 5);
        q.setAnswerKey(req.getAnswerKey());
        q.setSource(req.getSource());
        q.setParentId(req.getParentId());
        q.setImageUrlsJson(req.getImageUrlsJson());
        q.setCreatedBy(creator);

        // 共享设置（须在 setCreatedBy 之后，applyVisibility 要排除作成者本人）
        applyVisibility(q, req, role, true);

        // 知识点标签
        if (req.getKnowledgeTagIds() != null) {
            q.setKnowledgeTags(req.getKnowledgeTagIds().stream()
                    .map(tid -> tagRepository.findById(tid).orElseThrow())
                    .collect(Collectors.toSet()));
        }

        Question saved = questionRepository.save(q);

        // 选项
        if (req.getOptions() != null) {
            List<QuestionOption> opts = req.getOptions().stream().map(o -> {
                QuestionOption opt = new QuestionOption();
                opt.setQuestion(saved);
                opt.setOptionLabel(o.getOptionLabel());
                opt.setContentLatex(o.getContentLatex());
                opt.setIsCorrect(Boolean.TRUE.equals(o.getIsCorrect()));
                return opt;
            }).collect(Collectors.toList());
            saved.setOptions(opts);
        }

        // 解析步骤
        if (req.getSolutionSteps() != null) {
            List<SolutionStep> steps = req.getSolutionSteps().stream().map(s -> {
                SolutionStep step = new SolutionStep();
                step.setQuestion(saved);
                step.setStepOrder(s.getStepOrder());
                step.setContentLatex(s.getContentLatex());
                step.setStepScore(s.getStepScore() != null ? s.getStepScore() : 0);
                step.setCommonErrors(s.getCommonErrors());
                step.setImageUrlsJson(s.getImageUrlsJson());
                return step;
            }).collect(Collectors.toList());
            saved.setSolutionSteps(steps);
        }

        return QuestionDto.Response.from(questionRepository.save(saved));
    }

    @Transactional
    public QuestionDto.Response update(Long id, QuestionDto.Request req, Long userId, String role) {
        Question q = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("题目不存在"));
        assertCanModify(q, userId, role, "编辑");
        if (req.getTitle() != null) q.setTitle(req.getTitle());
        if (req.getContentLatex() != null) q.setContentLatex(req.getContentLatex());
        if (req.getDifficulty() != null) q.setDifficulty(req.getDifficulty());
        if (req.getTotalScore() != null) q.setTotalScore(req.getTotalScore());
        if (req.getAnswerKey() != null) q.setAnswerKey(req.getAnswerKey());
        if (req.getSource() != null) q.setSource(req.getSource());
        if (req.getImageUrlsJson() != null) q.setImageUrlsJson(req.getImageUrlsJson());
 
        // 选项更新：清空旧选项，重新写入
        if (req.getOptions() != null) {
            q.getOptions().clear();
            List<QuestionOption> opts = req.getOptions().stream().map(o -> {
                QuestionOption opt = new QuestionOption();
                opt.setQuestion(q);
                opt.setOptionLabel(o.getOptionLabel());
                opt.setContentLatex(o.getContentLatex());
                opt.setIsCorrect(Boolean.TRUE.equals(o.getIsCorrect()));
                return opt;
            }).collect(Collectors.toList());
            q.getOptions().addAll(opts);
        }
 
        // 解析步骤更新：清空旧步骤，重新写入
        if (req.getSolutionSteps() != null) {
            q.getSolutionSteps().clear();
            List<SolutionStep> steps = req.getSolutionSteps().stream().map(s -> {
                SolutionStep step = new SolutionStep();
                step.setQuestion(q);
                step.setStepOrder(s.getStepOrder());
                step.setContentLatex(s.getContentLatex());
                step.setStepScore(s.getStepScore() != null ? s.getStepScore() : 0);
                step.setCommonErrors(s.getCommonErrors());
                step.setImageUrlsJson(s.getImageUrlsJson());
                return step;
            }).collect(Collectors.toList());
            q.getSolutionSteps().addAll(steps);
        }

        // 共享设置
        applyVisibility(q, req, role, false);

        return QuestionDto.Response.from(questionRepository.save(q));
    }

    @Transactional
    public void delete(Long id, Long userId, String role) {
        Question q = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("题目不存在"));
        assertCanModify(q, userId, role, "删除");
        // 用 delete(entity) 而非 deleteById，确保 question_shares 等关联行被清理
        questionRepository.delete(q);
    }

    public List<QuestionDto.Response> getByTag(Long tagId) {
        return questionRepository.findByKnowledgeTagId(tagId).stream()
                .map(QuestionDto.Response::from)
                .collect(Collectors.toList());
    }

    // ==================== 权限判定 ====================
    //
    // 三条通用约定（与 GoalService 的既有做法保持一致）：
    //   1. userId / role 为 null（无 token 或旧 token）→ 一律放行，向后兼容；
    //   2. ADMIN → 一律放行；
    //   3. 其余按 visibility 判定。

    /** 能否看到这道题：PUBLIC 全放行；作成者本人放行；SHARED 时查共享名单 */
    private boolean canView(Question q, Long userId, String role) {
        if (userId == null || role == null || "ADMIN".equals(role)) return true;
        if (q.getVisibility() == null || q.getVisibility() == Question.Visibility.PUBLIC) return true;
        if (q.getCreatedBy() != null && userId.equals(q.getCreatedBy().getId())) return true;
        return q.getVisibility() == Question.Visibility.SHARED
                && q.getSharedWith() != null
                && q.getSharedWith().stream().anyMatch(u -> userId.equals(u.getId()));
    }

    /**
     * 能否修改这道题：仅作成者。
     * 被共享的老师只能"使用"（组入作业），不能编辑或删除原题。
     */
    private void assertCanModify(Question q, Long userId, String role, String action) {
        if (userId == null || role == null || "ADMIN".equals(role)) return;
        if (q.getCreatedBy() == null || !userId.equals(q.getCreatedBy().getId())) {
            throw new RuntimeException("无权" + action + "此题目");
        }
    }

    /** 传给 repository 的 viewerId：ADMIN 与无 token 的请求都传 null（不施加限制） */
    private Long viewerFilter(Long userId, String role) {
        if (userId == null || role == null || "ADMIN".equals(role)) return null;
        return userId;
    }

    /**
     * 写入可见性与共享名单，create / update 共用。
     *
     * 权限规则（前端隐藏不足以为凭，这里是唯一的强制点）：
     *   1. 非 ADMIN 只能设 PUBLIC / PRIVATE —— 请求里的 SHARED 与 sharedUserIds 一律忽略；
     *   2. 题目已是 SHARED 时，非 ADMIN 不能改动可见性
     *      —— 管理员指定的共享由管理员维护，作成者仍可编辑题目内容；
     *   3. 共享名单只有 ADMIN 能写，实际入口是 PATCH /{id}/shares（见 setShares）。
     *
     * 一律静默降级而不抛异常：老师保存时表单总会带上 visibility，
     * 管理员设为 SHARED 的题目被作成者打开时回填的就是 "SHARED"，
     * 若在此抛错，作成者每次保存正文都会失败 —— 那会直接违反规则 2 的本意。
     *
     * @param isNew true = create（无既有可见性），false = update
     */
    private void applyVisibility(Question q, QuestionDto.Request req, String role, boolean isNew) {
        // role == null（无 token / 旧 token）并入放行分支，与本类其余判定的约定一致
        boolean isAdmin = role == null || "ADMIN".equals(role);

        Question.Visibility current = q.getVisibility();

        // 规则 2：既有 SHARED 的题目，非管理员不动可见性与名单
        if (!isNew && current == Question.Visibility.SHARED && !isAdmin) {
            return;
        }

        Question.Visibility target = current;
        if (req.getVisibility() != null && !req.getVisibility().isBlank()) {
            try {
                target = Question.Visibility.valueOf(req.getVisibility());
            } catch (IllegalArgumentException e) {
                // 无效值降级为 PUBLIC，避免 500（与上面处理 questionType 的方式一致）
                target = Question.Visibility.PUBLIC;
            }
        }
        if (target == null) target = Question.Visibility.PUBLIC;   // 新建默认公开，保持历史行为

        // 规则 1：非管理员请求 SHARED —— 保持原值，新建则落 PUBLIC
        if (target == Question.Visibility.SHARED && !isAdmin) {
            target = (current != null) ? current : Question.Visibility.PUBLIC;
        }

        q.setVisibility(target);

        // 规则 3：名单仅 ADMIN 可写。
        // 刻意不在切到 PUBLIC/PRIVATE 时清空 —— 老师的一次保存不应冲掉管理员指定的名单；
        // 名单只在 visibility=SHARED 时被读取（见 canView 与两个 repository 查询），
        // 所以留着的旧名单不会授予任何权限。
        if (!isAdmin || req.getSharedUserIds() == null) return;
        if (target != Question.Visibility.SHARED) return;
        replaceShares(q, req.getSharedUserIds());
    }

    /** 覆盖共享名单：排除作成者本人，过滤掉学生 */
    private void replaceShares(Question q, Set<Long> userIds) {
        Long ownerId = q.getCreatedBy() != null ? q.getCreatedBy().getId() : null;
        q.setSharedWith(userIds.stream()
                .filter(uid -> uid != null && !uid.equals(ownerId))   // 作成者无需出现在共享名单里
                .distinct()
                .map(uid -> userRepository.findById(uid)
                        .orElseThrow(() -> new RuntimeException("用户不存在: " + uid)))
                .filter(u -> u.getRole() == User.Role.TEACHER || u.getRole() == User.Role.ADMIN)  // 学生不能被共享
                .collect(Collectors.toSet()));
    }

    /**
     * 管理员专用：设置题目的共享范围与共享名单。
     *
     * 注意这里对 role == null 的处理与 applyVisibility 相反 —— 那边放行（兼容旧 token），
     * 这里拒绝。本端点是新增的，没有历史流量需要兼容，且属于授权路径，应当 fail closed。
     * 两处的不对称是有意为之，不要「顺手统一」。
     */
    @Transactional
    public QuestionDto.Response setShares(Long id, QuestionDto.ShareRequest req, String role) {
        if (!"ADMIN".equals(role)) {
            throw new RuntimeException("仅系统管理员可以指定共享教师");
        }
        Question q = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("题目不存在"));

        Question.Visibility target = Question.Visibility.SHARED;   // 端点名即为 shares，缺省按 SHARED 处理
        if (req.getVisibility() != null && !req.getVisibility().isBlank()) {
            try {
                target = Question.Visibility.valueOf(req.getVisibility());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("无效的可见性: " + req.getVisibility());
            }
        }
        q.setVisibility(target);

        // 名单始终可更新（即使切到 PUBLIC/PRIVATE），便于管理员先配好名单再开共享
        if (req.getSharedUserIds() != null) {
            replaceShares(q, req.getSharedUserIds());
        }
        return QuestionDto.Response.from(questionRepository.save(q));
    }
}
