package com.example.homework.repository;

import com.example.homework.entity.MistakeNote;
import com.example.homework.entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MistakeNoteRepository extends JpaRepository<MistakeNote, Long> {

    Optional<MistakeNote> findByStudentIdAndQuestionId(Long studentId, Long questionId);

    long countByStudentId(Long studentId);

    long countByStudentIdAndMastery(Long studentId, MistakeNote.Mastery mastery);

    /** 做题页批量回填星标状态：一次查出该学生在这批题目里已收藏的 question_id */
    @Query("SELECT n.question.id FROM MistakeNote n " +
           "WHERE n.student.id = :studentId AND n.question.id IN :questionIds")
    List<Long> findCollectedQuestionIds(@Param("studentId") Long studentId,
                                        @Param("questionIds") Collection<Long> questionIds);

    /**
     * 第一步：按筛选条件分页取出条目 ID。
     * 这里刻意不做 JOIN FETCH —— 理由同 QuestionRepository.findIdsForSummary：
     * Hibernate 6 下集合抓取与 firstResult/maxResults 并用会破坏分页，
     * 且 count 查询无法从 fetch join 语句正确派生。
     *
     * 知识点用 EXISTS 子查询而非 join：一题挂多个标签时 join 会造成结果行重复，
     * 直接破坏分页与计数。
     */
    @Query("""
            SELECT n.id FROM MistakeNote n
            WHERE n.student.id = :studentId
              AND (:questionType IS NULL OR n.question.questionType = :questionType)
              AND (:mastery IS NULL OR n.mastery = :mastery)
              AND (:tagId IS NULL OR EXISTS (
                    SELECT 1 FROM Question q2 JOIN q2.knowledgeTags t
                    WHERE q2.id = n.question.id AND t.id = :tagId))
            ORDER BY n.createdAt DESC, n.id DESC
            """)
    Page<Long> findNoteIds(@Param("studentId") Long studentId,
                           @Param("questionType") Question.QuestionType questionType,
                           @Param("mastery") MistakeNote.Mastery mastery,
                           @Param("tagId") Long tagId,
                           Pageable pageable);

    /**
     * 第二步：只对当前页的 ID 抓取题目与知识点标签，无分页参数，fetch join 在此是安全的。
     * 刻意不 fetch question.options / question.solutionSteps：
     * 那里面是大体积 base64 图片，列表场景用不到（详情接口才需要）。
     *
     * 注意：本查询【不保证】返回顺序与传入 ID 的顺序一致，
     * 而列表是按 createdAt DESC 排序的，Service 层必须按 ID 页顺序重排。
     */
    @Query("""
            SELECT DISTINCT n FROM MistakeNote n
            LEFT JOIN FETCH n.question q
            LEFT JOIN FETCH q.knowledgeTags
            WHERE n.id IN :ids
            """)
    List<MistakeNote> findByIdsWithQuestion(@Param("ids") Collection<Long> ids);

    /**
     * 详情用：连选项一并抓取（选择题要红绿高亮）。
     * solutionSteps 不在这里 fetch —— 它与 options 同为 ToMany，
     * 两个集合同时 fetch 会产生笛卡尔积放大，交由 Service 在事务内按需触发。
     */
    @Query("""
            SELECT DISTINCT n FROM MistakeNote n
            LEFT JOIN FETCH n.question q
            LEFT JOIN FETCH q.knowledgeTags
            WHERE n.id = :id
            """)
    Optional<MistakeNote> findDetailById(@Param("id") Long id);
}
