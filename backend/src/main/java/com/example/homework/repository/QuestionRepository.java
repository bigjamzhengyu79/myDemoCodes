package com.example.homework.repository;

import com.example.homework.entity.Question;
import com.example.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByCreatedBy(User user);

    @Query("SELECT q FROM Question q JOIN q.knowledgeTags t WHERE t.id = :tagId")
    List<Question> findByKnowledgeTagId(@Param("tagId") Long tagId);

    @Query("SELECT q FROM Question q WHERE q.difficulty = :d")
    List<Question> findByDifficulty(@Param("d") Integer difficulty);

    /**
     * 列表视图用：一次性抓取知识点标签与创建者，避免 N+1；
     * 不触碰选项与解析步骤（含大体积 base64 图片），也不抓 sharedWith
     * （两个集合同时 fetch 会放大结果行数）。
     *
     * viewerId 为 null 表示不做可见性限制 —— 同时服务 ADMIN 与无 token 的旧请求。
     * 详见 findIdsForSummary 上的说明。
     */
    @Query("""
            SELECT DISTINCT q FROM Question q
            LEFT JOIN FETCH q.knowledgeTags
            LEFT JOIN FETCH q.createdBy
            WHERE (:viewerId IS NULL
                   OR q.visibility = :publicVis
                   OR q.createdBy.id = :viewerId
                   OR (q.visibility = :sharedVis AND EXISTS (
                        SELECT 1 FROM Question q3 JOIN q3.sharedWith su
                        WHERE q3.id = q.id AND su.id = :viewerId)))
            ORDER BY q.id
            """)
    List<Question> findAllForSummaryVisibleTo(@Param("viewerId") Long viewerId,
                                              @Param("publicVis") Question.Visibility publicVis,
                                              @Param("sharedVis") Question.Visibility sharedVis);

    /**
     * 第一步：按筛选条件分页取出题目 ID。
     * 这里刻意不做 JOIN FETCH —— Hibernate 6 下集合抓取与 firstResult/maxResults 并用会破坏分页，
     * 且 count 查询无法从 fetch join 语句正确派生。
     *
     * 关键词：title 优先；title 为空时回退匹配 content_latex（兜底无标题的题目）。
     * 知识点：用 EXISTS 子查询而非 join，避免一题多标签时结果行重复、影响分页与计数。
     *
     * 可见性：同样用 EXISTS 查 question_shares，理由同上（一题共享给多人时 join 会造成行重复）。
     * viewerId 为 null 时整段短路 —— 这一个方法因此同时服务三种调用方：
     *   普通教师 → 传其 userId；ADMIN → 传 null；无 token 的旧请求 → 传 null（向后兼容）。
     * 过滤必须留在 SQL 层：若改为取出后在 Java 里 filter，分页与 count 都会失真。
     */
    @Query("""
            SELECT q.id FROM Question q
            WHERE (:keyword IS NULL OR
                   (q.title IS NOT NULL AND q.title <> '' AND LOWER(q.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
                   OR ((q.title IS NULL OR q.title = '') AND LOWER(q.contentLatex) LIKE LOWER(CONCAT('%', :keyword, '%'))))
              AND (:questionType IS NULL OR q.questionType = :questionType)
              AND (:difficulty IS NULL OR q.difficulty = :difficulty)
              AND (:tagId IS NULL OR EXISTS (
                    SELECT 1 FROM Question q2 JOIN q2.knowledgeTags t
                    WHERE q2.id = q.id AND t.id = :tagId))
              AND (:viewerId IS NULL
                   OR q.visibility = :publicVis
                   OR q.createdBy.id = :viewerId
                   OR (q.visibility = :sharedVis AND EXISTS (
                        SELECT 1 FROM Question q3 JOIN q3.sharedWith su
                        WHERE q3.id = q.id AND su.id = :viewerId)))
            ORDER BY q.id
            """)
    Page<Long> findIdsForSummary(@Param("keyword") String keyword,
                                 @Param("questionType") Question.QuestionType questionType,
                                 @Param("difficulty") Integer difficulty,
                                 @Param("tagId") Long tagId,
                                 @Param("viewerId") Long viewerId,
                                 @Param("publicVis") Question.Visibility publicVis,
                                 @Param("sharedVis") Question.Visibility sharedVis,
                                 Pageable pageable);

    // 第二步：只对当前页的 ID 抓取知识点标签，无分页参数，fetch join 在此是安全的。
    // createdBy 是 LAZY 的 ToOne，Summary 要展示创建者，一并 fetch 避免每行一条 user 查询；
    // ToOne 与 ToMany 同时 fetch 不会产生额外的笛卡尔积。
    @Query("""
            SELECT DISTINCT q FROM Question q
            LEFT JOIN FETCH q.knowledgeTags
            LEFT JOIN FETCH q.createdBy
            WHERE q.id IN :ids ORDER BY q.id
            """)
    List<Question> findByIdsWithTags(@Param("ids") Collection<Long> ids);

    // 按精确 ID 查一题（供选择器的 "#编号" 搜索使用），同样带标签与创建者。
    // 这里不加可见性谓词 —— 只有 0/1 条结果，在 Service 用 canView 过滤更清晰。
    @Query("""
            SELECT DISTINCT q FROM Question q
            LEFT JOIN FETCH q.knowledgeTags
            LEFT JOIN FETCH q.createdBy
            WHERE q.id = :id
            """)
    List<Question> findByIdWithTags(@Param("id") Long id);
}
