package com.example.homework.service;

import com.example.entity.User;
import com.example.homework.dto.QuestionDto;
import com.example.homework.entity.*;
import com.example.homework.repository.*;
import com.example.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final KnowledgeTagRepository tagRepository;
    private final UserRepository userRepository;

    public List<QuestionDto.Response> listAll() {
        return questionRepository.findAll().stream()
                .map(QuestionDto.Response::from)
                .collect(Collectors.toList());
    }

    public QuestionDto.Response getById(Long id) {
        Question q = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("题目不存在: " + id));
        return QuestionDto.Response.from(q);
    }

    @Transactional
    public QuestionDto.Response create(QuestionDto.Request req, String username) {
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
        q.setCreatedBy(creator);

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
                return step;
            }).collect(Collectors.toList());
            saved.setSolutionSteps(steps);
        }

        return QuestionDto.Response.from(questionRepository.save(saved));
    }

    @Transactional
    public QuestionDto.Response update(Long id, QuestionDto.Request req) {
        Question q = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("题目不存在"));
        if (req.getTitle() != null) q.setTitle(req.getTitle());
        if (req.getContentLatex() != null) q.setContentLatex(req.getContentLatex());
        if (req.getDifficulty() != null) q.setDifficulty(req.getDifficulty());
        if (req.getTotalScore() != null) q.setTotalScore(req.getTotalScore());
        if (req.getAnswerKey() != null) q.setAnswerKey(req.getAnswerKey());
        if (req.getSource() != null) q.setSource(req.getSource());
        return QuestionDto.Response.from(questionRepository.save(q));
    }

    public void delete(Long id) {
        questionRepository.deleteById(id);
    }

    public List<QuestionDto.Response> getByTag(Long tagId) {
        return questionRepository.findByKnowledgeTagId(tagId).stream()
                .map(QuestionDto.Response::from)
                .collect(Collectors.toList());
    }
}
