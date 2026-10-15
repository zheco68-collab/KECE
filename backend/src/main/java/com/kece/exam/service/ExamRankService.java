package com.kece.exam.service;

import com.kece.common.exception.BusinessException;
import com.kece.common.response.ResultCode;
import com.kece.database.entity.DbSubmission;
import com.kece.database.mapper.DbSubmissionMapper;
import com.kece.exam.dto.ExamRankVO;
import com.kece.exam.entity.Exam;
import com.kece.exam.entity.ExamQuestion;
import com.kece.exam.entity.ExamSubmission;
import com.kece.exam.mapper.ExamMapper;
import com.kece.exam.mapper.ExamQuestionMapper;
import com.kece.exam.mapper.ExamSubmissionMapper;
import com.kece.submission.entity.Submission;
import com.kece.submission.mapper.SubmissionMapper;
import com.kece.user.entity.User;
import com.kece.user.mapper.UserMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 考试排名：ACM（AC 数 + 罚时）与 OI（总分）双模式。 */
@Service
@RequiredArgsConstructor
public class ExamRankService {

    private final ExamMapper examMapper;
    private final ExamQuestionMapper examQuestionMapper;
    private final ExamSubmissionMapper submissionMapper;
    private final SubmissionMapper codeSubmissionMapper;
    private final DbSubmissionMapper dbSubmissionMapper;
    private final UserMapper userMapper;

    public List<ExamRankVO> ranking(Long examId) {
        Exam exam = examMapper.findById(examId);
        if (exam == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "考试不存在");
        }
        boolean acm = "ACM".equalsIgnoreCase(exam.getMode());

        List<ExamSubmission> answers = submissionMapper.findByExam(examId);
        List<Submission> codeSubs = codeSubmissionMapper.findByContest(examId);
        List<DbSubmission> dbSubs = dbSubmissionMapper.findByContest(examId);

        // 代码/数据库题最新提交索引：key = userId:questionId
        Map<String, Submission> latestCode = new HashMap<>();
        for (Submission s : codeSubs) {
            latestCode.put(key(s.getUserId(), s.getQuestionId()), s);
        }
        Map<String, DbSubmission> latestDb = new HashMap<>();
        for (DbSubmission s : dbSubs) {
            latestDb.put(key(s.getUserId(), s.getQuestionId()), s);
        }

        List<ExamQuestion> examQuestions = examQuestionMapper.findByExamId(examId);
        int examTotalFullScore =
                examQuestions.stream().mapToInt(q -> q.getScore() == null ? 0 : q.getScore()).sum();

        Map<Long, RankAgg> aggByUser = new LinkedHashMap<>();
        for (ExamSubmission row : answers) {
            ExamQuestion eq = examQuestionMapper.findById(row.getExamQuestionId());
            if (eq == null) {
                continue;
            }
            RankAgg agg =
                    aggByUser.computeIfAbsent(
                            row.getUserId(),
                            k -> {
                                RankAgg a = new RankAgg();
                                a.fullScore = examTotalFullScore;
                                return a;
                            });
            agg.finished++;
            String status = row.getStatus();
            Integer score = row.getScore();
            if ("ALGO".equals(eq.getQuestionType())) {
                Submission s = latestCode.get(key(row.getUserId(), eq.getQuestionId()));
                if (s != null) {
                    status = s.getStatus();
                    score = scaleScore(s.getScore(), eq.getScore());
                }
            } else if ("DB".equals(eq.getQuestionType())) {
                DbSubmission s = latestDb.get(key(row.getUserId(), eq.getQuestionId()));
                if (s != null) {
                    status = s.getStatus();
                    score = scaleScore(s.getScore(), eq.getScore());
                }
            }
            if (isAccepted(status)) {
                agg.solved++;
            }
            agg.totalScore += score == null ? 0 : score;
        }

        // ACM 罚时：代码/数据库题按 contest 内逐条计算（首 AC 分钟数 + 20×错误次数）
        if (acm) {
            Map<Long, RankAgg> byUser = new HashMap<>();
            aggByUser.forEach((k, v) -> byUser.put(k, v));
            for (Submission s : codeSubs) {
                RankAgg agg = byUser.computeIfAbsent(s.getUserId(), k -> new RankAgg());
                accPenalty(agg, exam, s.getCreatedAt(), s.getStatus());
            }
            for (DbSubmission s : dbSubs) {
                RankAgg agg = byUser.computeIfAbsent(s.getUserId(), k -> new RankAgg());
                accPenalty(agg, exam, s.getCreatedAt(), s.getStatus());
            }
        }

        List<ExamRankVO> result = new ArrayList<>();
        for (Map.Entry<Long, RankAgg> e : aggByUser.entrySet()) {
            User user = userMapper.findById(e.getKey());
            RankAgg agg = e.getValue();
            result.add(
                    ExamRankVO.builder()
                            .userId(e.getKey())
                            .username(user == null ? "#" + e.getKey() : user.getUsername())
                            .solved(agg.solved)
                            .penaltyMinutes(agg.penaltyMinutes)
                            .totalScore(agg.totalScore)
                            .finished(agg.finished)
                            .fullScore(agg.fullScore)
                            .build());
        }
        Comparator<ExamRankVO> comparator =
                acm
                        ? Comparator.comparingInt(ExamRankVO::getSolved)
                                .reversed()
                                .thenComparing(ExamRankVO::getPenaltyMinutes)
                                .thenComparing(ExamRankVO::getUsername)
                        : Comparator.comparingInt(ExamRankVO::getTotalScore)
                                .reversed()
                                .thenComparing(ExamRankVO::getFinished, Comparator.reverseOrder())
                                .thenComparing(ExamRankVO::getUsername);
        result.sort(comparator);
        return result;
    }

    /** ACM 罚时累计：每道题在首 AC 前每次非 AC 记 20 分钟；AC 后不再累计。 */
    private void accPenalty(
            RankAgg agg, Exam exam, java.time.LocalDateTime createdAt, String status) {
        if (status == null || createdAt == null) {
            return;
        }
        if (isAccepted(status)) {
            long minutes =
                    Math.max(0, Duration.between(exam.getStartTime(), createdAt).toMinutes());
            agg.penaltyMinutes += minutes;
        } else {
            agg.penaltyMinutes += 20;
        }
    }

    private boolean isAccepted(String status) {
        return "ACCEPTED".equals(status);
    }

    private Integer scaleScore(Integer judgeScore, Integer fullScore) {
        if (judgeScore == null || fullScore == null) {
            return null;
        }
        return (int) Math.round((double) judgeScore * fullScore / 100.0);
    }

    private String key(Long userId, Long questionId) {
        return userId + ":" + questionId;
    }

    /** 排名聚合中间态。 */
    private static class RankAgg {
        private int solved;
        private long penaltyMinutes;
        private int totalScore;
        private int finished;
        private int fullScore;
    }
}
