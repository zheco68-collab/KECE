package com.kece.practice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kece.common.exception.BusinessException;
import com.kece.common.response.ResultCode;
import com.kece.common.security.AuthUser;
import com.kece.common.security.Role;
import com.kece.database.entity.DbQuestion;
import com.kece.database.mapper.DbQuestionMapper;
import com.kece.objective.dto.AnswerResultVO;
import com.kece.objective.entity.ObjectiveQuestion;
import com.kece.objective.mapper.ObjectiveQuestionMapper;
import com.kece.objective.service.ObjectiveGrader;
import com.kece.practice.dto.*;
import com.kece.practice.entity.*;
import com.kece.practice.mapper.*;
import com.kece.question.entity.Question;
import com.kece.question.mapper.QuestionMapper;
import com.kece.user.entity.User;
import com.kece.user.mapper.UserMapper;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LessonPracticeService {

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final LessonPracticeMapper practiceMapper;
    private final LessonPracticeQuestionMapper questionMapper;
    private final LessonPracticeMemberMapper memberMapper;
    private final LessonPracticeSubmissionMapper submissionMapper;
    private final ObjectiveQuestionMapper objectiveQuestionMapper;
    private final QuestionMapper algoQuestionMapper;
    private final DbQuestionMapper dbQuestionMapper;
    private final UserMapper userMapper;
    private final ObjectiveGrader objectiveGrader;
    private final ObjectMapper objectMapper;

    /** 生成唯一 6 位大写房间口令。 */
    private String generateAccessCode() {
        for (int i = 0; i < 20; i++) {
            StringBuilder sb = new StringBuilder(6);
            for (int j = 0; j < 6; j++) {
                sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            }
            String code = sb.toString();
            if (practiceMapper.findByAccessCode(code) == null) {
                return code;
            }
        }
        return "KE" + (1000 + RANDOM.nextInt(9000));
    }

    /** 教师发起新课练。 */
    @Transactional
    public PracticeSummaryVO createPractice(Long teacherId, CreatePracticeRequest req) {
        String accessCode = generateAccessCode();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = req.getDurationMinutes() != null && req.getDurationMinutes() > 0
                ? now.plusMinutes(req.getDurationMinutes())
                : null;

        LessonPractice practice = LessonPractice.builder()
                .title(req.getTitle().trim())
                .description(req.getDescription())
                .accessCode(accessCode)
                .durationMinutes(req.getDurationMinutes())
                .status("ACTIVE")
                .teacherId(teacherId)
                .startTime(now)
                .endTime(endTime)
                .build();

        practiceMapper.insert(practice);

        if (req.getQuestions() != null && !req.getQuestions().isEmpty()) {
            List<LessonPracticeQuestion> qList = new ArrayList<>();
            int sort = 1;
            for (PracticeQuestionItem item : req.getQuestions()) {
                String title = item.getTitle();
                if (title == null || title.isBlank()) {
                    // 自动按题型拉取标题
                    if ("OBJECTIVE".equalsIgnoreCase(item.getQuestionType())) {
                        ObjectiveQuestion oq = objectiveQuestionMapper.findById(item.getQuestionId());
                        title = oq != null ? (oq.getContent().length() > 30 ? oq.getContent().substring(0, 30) + "…" : oq.getContent()) : "客观题";
                    } else if ("ALGO".equalsIgnoreCase(item.getQuestionType())) {
                        Question q = algoQuestionMapper.findById(item.getQuestionId());
                        title = q != null ? q.getTitle() : "算法题";
                    } else if ("DB".equalsIgnoreCase(item.getQuestionType())) {
                        DbQuestion dq = dbQuestionMapper.findById(item.getQuestionId());
                        title = dq != null ? dq.getTitle() : "数据库题";
                    } else {
                        title = "题目 " + sort;
                    }
                }

                qList.add(LessonPracticeQuestion.builder()
                        .practiceId(practice.getId())
                        .questionId(item.getQuestionId())
                        .questionType(item.getQuestionType().toUpperCase())
                        .title(title)
                        .score(item.getScore() != null ? item.getScore() : 10)
                        .sortOrder(item.getSortOrder() != null ? item.getSortOrder() : sort++)
                        .build());
            }
            questionMapper.insertBatch(qList);
        }

        return getSummaryVO(practice, teacherId);
    }

    /** 学生或教师通过 6 位口令加入课练房间。 */
    @Transactional
    public PracticeDetailVO joinPractice(Long userId, String accessCode) {
        String code = accessCode.trim().toUpperCase();
        LessonPractice practice = practiceMapper.findByAccessCode(code);
        if (practice == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "课练口令不存在，请核对后重试");
        }

        LessonPracticeMember member = memberMapper.findByPracticeAndUser(practice.getId(), userId);
        if ("ENDED".equals(practice.getStatus()) && member == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该课练已截止收卷，无法入场");
        }
        if (member == null) {
            member = LessonPracticeMember.builder()
                    .practiceId(practice.getId())
                    .userId(userId)
                    .totalScore(0)
                    .solvedCount(0)
                    .joinedAt(LocalDateTime.now())
                    .submitted(0)
                    .build();
            memberMapper.insert(member);
        }

        return getPracticeDetail(code, userId);
    }

    /** 获取课练房间主页（题目列表与作答概览）。 */
    public PracticeDetailVO getPracticeDetail(String accessCode, Long userId) {
        String code = accessCode.trim().toUpperCase();
        LessonPractice practice = practiceMapper.findByAccessCode(code);
        if (practice == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "课练口令不存在");
        }

        User teacher = userMapper.findById(practice.getTeacherId());
        String teacherName = teacher != null ? teacher.getUsername() : "教师";
        int memberCount = memberMapper.countByPracticeId(practice.getId());

        LocalDateTime now = LocalDateTime.now();
        Long remainingSeconds = null;
        if ("ENDED".equals(practice.getStatus())) {
            remainingSeconds = 0L;
        } else if (practice.getEndTime() != null) {
            remainingSeconds = Math.max(0, Duration.between(now, practice.getEndTime()).getSeconds());
            if (remainingSeconds <= 0 && "ACTIVE".equals(practice.getStatus())) {
                practice.setStatus("ENDED");
                practiceMapper.updateStatusAndEndTime(practice.getId(), "ENDED", now);
                remainingSeconds = 0L;
            }
        }

        LessonPracticeMember myMember = userId != null ? memberMapper.findByPracticeAndUser(practice.getId(), userId) : null;
        if ("ENDED".equals(practice.getStatus())) {
            User currentUser = userId != null ? userMapper.findById(userId) : null;
            boolean isTeacherOrAdmin = currentUser != null && (practice.getTeacherId().equals(userId)
                    || (currentUser.getRoles() != null && (currentUser.getRoles().contains("ADMIN") || currentUser.getRoles().contains("TEACHER"))));
            if (!isTeacherOrAdmin) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "该课练已结束，无法进入");
            }
        }
        List<LessonPracticeQuestion> allQuestions = questionMapper.listByPracticeId(practice.getId());
        List<LessonPracticeSubmission> mySubmissions = userId != null
                ? submissionMapper.listByPracticeAndUser(practice.getId(), userId)
                : Collections.emptyList();

        Map<Long, LessonPracticeSubmission> subMap = mySubmissions.stream()
                .collect(Collectors.toMap(
                        LessonPracticeSubmission::getPracticeQuestionId,
                        s -> s,
                        (existing, replacement) -> existing
                ));

        List<PracticeObjectiveQuestionVO> objectiveList = new ArrayList<>();
        List<PracticePracticalQuestionVO> practicalList = new ArrayList<>();

        for (LessonPracticeQuestion pq : allQuestions) {
            LessonPracticeSubmission sub = subMap.get(pq.getId());
            if ("OBJECTIVE".equals(pq.getQuestionType())) {
                ObjectiveQuestion oq = objectiveQuestionMapper.findById(pq.getQuestionId());
                List<String> options = parseOptions(oq != null ? oq.getOptions() : null);
                boolean isCorrect = sub != null && "ACCEPTED".equals(sub.getStatus());

                objectiveList.add(PracticeObjectiveQuestionVO.builder()
                        .practiceQuestionId(pq.getId())
                        .questionId(pq.getQuestionId())
                        .type(oq != null ? oq.getType() : "SINGLE")
                        .content(oq != null ? oq.getContent() : pq.getTitle())
                        .options(options)
                        .score(pq.getScore())
                        .sortOrder(pq.getSortOrder())
                        .userAnswer(sub != null ? sub.getAnswer() : null)
                        .userScore(sub != null ? sub.getScore() : null)
                        .isCorrect(sub != null ? isCorrect : null)
                        .answered(sub != null)
                        .build());
            } else if ("ALGO".equals(pq.getQuestionType())) {
                Question aq = algoQuestionMapper.findById(pq.getQuestionId());
                List<String> langs = aq != null && aq.getLanguages() != null
                        ? Arrays.stream(aq.getLanguages().split(",")).map(String::trim).toList()
                        : List.of("cpp", "java", "python");

                practicalList.add(PracticePracticalQuestionVO.builder()
                        .practiceQuestionId(pq.getId())
                        .questionId(pq.getQuestionId())
                        .questionType("ALGO")
                        .title(aq != null ? aq.getTitle() : pq.getTitle())
                        .score(pq.getScore())
                        .sortOrder(pq.getSortOrder())
                        .difficulty(aq != null ? aq.getDifficulty() : "MEDIUM")
                        .timeLimitMs(aq != null && aq.getTimeLimit() != null ? aq.getTimeLimit() : 1000)
                        .memoryLimitKb(aq != null && aq.getMemoryLimit() != null ? aq.getMemoryLimit() * 1024 : 262144)
                        .languages(langs)
                        .userStatus(sub != null ? sub.getStatus() : "UNATTEMPTED")
                        .userScore(sub != null ? sub.getScore() : 0)
                        .submissionId(sub != null ? sub.getSubmissionId() : null)
                        .build());
            } else if ("DB".equals(pq.getQuestionType())) {
                DbQuestion dq = dbQuestionMapper.findById(pq.getQuestionId());

                practicalList.add(PracticePracticalQuestionVO.builder()
                        .practiceQuestionId(pq.getId())
                        .questionId(pq.getQuestionId())
                        .questionType("DB")
                        .title(dq != null ? dq.getTitle() : pq.getTitle())
                        .score(pq.getScore())
                        .sortOrder(pq.getSortOrder())
                        .difficulty(dq != null ? dq.getType() : "SELECT")
                        .timeLimitMs(3000)
                        .memoryLimitKb(262144)
                        .languages(List.of("sql"))
                        .userStatus(sub != null ? sub.getStatus() : "UNATTEMPTED")
                        .userScore(sub != null ? sub.getScore() : 0)
                        .submissionId(sub != null ? sub.getSubmissionId() : null)
                        .build());
            }
        }

        return PracticeDetailVO.builder()
                .id(practice.getId())
                .title(practice.getTitle())
                .description(practice.getDescription())
                .accessCode(practice.getAccessCode())
                .durationMinutes(practice.getDurationMinutes())
                .status(practice.getStatus())
                .teacherId(practice.getTeacherId())
                .teacherName(teacherName)
                .memberCount(memberCount)
                .startTime(practice.getStartTime())
                .endTime(practice.getEndTime())
                .remainingSeconds(remainingSeconds)
                .myJoined(myMember != null)
                .myScore(myMember != null ? myMember.getTotalScore() : 0)
                .mySolvedCount(myMember != null ? myMember.getSolvedCount() : 0)
                .mySubmitted(myMember != null && myMember.getSubmitted() == 1)
                .objectiveQuestions(objectiveList)
                .practicalQuestions(practicalList)
                .build();
    }

    /** 客观题集中答题并批量即时判分。 */
    @Transactional
    public PracticeObjectiveResultVO submitObjective(String accessCode, Long userId, PracticeObjectiveSubmitRequest req) {
        String code = accessCode.trim().toUpperCase();
        LessonPractice practice = practiceMapper.findByAccessCode(code);
        if (practice == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "课练不存在");
        }
        if ("ENDED".equals(practice.getStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "本次课练已结束，无法继续作答");
        }

        // 随堂练习定位：只要课练处于未截止状态，完全支持随时订正并重新判分
        LessonPracticeMember member = memberMapper.findByPracticeAndUser(practice.getId(), userId);
        if (member == null) {
            member = LessonPracticeMember.builder()
                    .practiceId(practice.getId())
                    .userId(userId)
                    .totalScore(0)
                    .solvedCount(0)
                    .joinedAt(LocalDateTime.now())
                    .submitted(0)
                    .build();
            memberMapper.insert(member);
        }

        List<LessonPracticeQuestion> questions = questionMapper.listByPracticeId(practice.getId()).stream()
                .filter(q -> "OBJECTIVE".equals(q.getQuestionType()))
                .toList();

        List<PracticeObjectiveResultVO.ItemResult> itemResults = new ArrayList<>();
        int totalEarned = 0;
        int totalFull = 0;
        int correctCount = 0;

        for (LessonPracticeQuestion pq : questions) {
            totalFull += pq.getScore();
            String submittedAnswer = req.getAnswers().get(pq.getId());
            if (submittedAnswer == null) {
                submittedAnswer = "";
            }

            ObjectiveQuestion oq = objectiveQuestionMapper.findById(pq.getQuestionId());
            AnswerResultVO gradeResult = oq != null
                    ? objectiveGrader.grade(oq, submittedAnswer)
                    : AnswerResultVO.builder().correct(false).score(0).fullScore(pq.getScore()).build();

            // 折算满分权重
            int questionScore = (int) Math.round((double) gradeResult.getScore() * pq.getScore() / (gradeResult.getFullScore() > 0 ? gradeResult.getFullScore() : 1));
            totalEarned += questionScore;
            if (gradeResult.isCorrect()) {
                correctCount++;
            }

            String status = gradeResult.isCorrect() ? "ACCEPTED" : "WRONG_ANSWER";

            // 保存或更新提交
            LessonPracticeSubmission existSub = submissionMapper.findByPracticeUserQuestion(practice.getId(), userId, pq.getId());
            if (existSub != null) {
                existSub.setAnswer(submittedAnswer);
                existSub.setStatus(status);
                existSub.setScore(questionScore);
                submissionMapper.updateSubmission(existSub);
            } else {
                LessonPracticeSubmission newSub = LessonPracticeSubmission.builder()
                        .practiceId(practice.getId())
                        .practiceQuestionId(pq.getId())
                        .userId(userId)
                        .questionType("OBJECTIVE")
                        .answer(submittedAnswer)
                        .status(status)
                        .score(questionScore)
                        .createdAt(LocalDateTime.now())
                        .build();
                submissionMapper.insert(newSub);
            }

            itemResults.add(PracticeObjectiveResultVO.ItemResult.builder()
                    .practiceQuestionId(pq.getId())
                    .questionId(pq.getQuestionId())
                    .correct(gradeResult.isCorrect())
                    .score(questionScore)
                    .fullScore(pq.getScore())
                    .userAnswer(submittedAnswer)
                    .correctAnswer(oq != null ? oq.getAnswer() : null)
                    .build());
        }

        // 重新统计用户在本次课练中的总分与 AC 题目数
        recalculateMemberScore(practice.getId(), userId);

        return PracticeObjectiveResultVO.builder()
                .totalScore(totalEarned)
                .fullScore(totalFull)
                .correctCount(correctCount)
                .totalCount(questions.size())
                .items(itemResults)
                .build();
    }

    /** 将编程/数据库实战提交与课练上下文关联并即刻统计。 */
    @Transactional
    public void recordPracticalSubmission(
            String accessCode,
            Long userId,
            Long practiceQuestionId,
            Long submissionId,
            String questionType,
            String status,
            int score) {
        LessonPractice practice = practiceMapper.findByAccessCode(accessCode.trim().toUpperCase());
        if (practice == null) return;
        if ("ENDED".equals(practice.getStatus())) return;

        LessonPracticeMember member = memberMapper.findByPracticeAndUser(practice.getId(), userId);
        if (member != null && member.getSubmitted() == 1) return;

        LessonPracticeSubmission exist = submissionMapper.findByPracticeUserQuestion(practice.getId(), userId, practiceQuestionId);
        if (exist != null) {
            exist.setSubmissionId(submissionId);
            exist.setStatus(status);
            exist.setScore(score);
            submissionMapper.updateSubmission(exist);
        } else {
            LessonPracticeSubmission sub = LessonPracticeSubmission.builder()
                    .practiceId(practice.getId())
                    .practiceQuestionId(practiceQuestionId)
                    .userId(userId)
                    .questionType(questionType)
                    .submissionId(submissionId)
                    .status(status)
                    .score(score)
                    .createdAt(LocalDateTime.now())
                    .build();
            submissionMapper.insert(sub);
        }

        recalculateMemberScore(practice.getId(), userId);
    }

    /** 重新计算成员总分与通过题目数（只统计实战编程/SQL题目通过数）。 */
    private void recalculateMemberScore(Long practiceId, Long userId) {
        List<LessonPracticeSubmission> subs = submissionMapper.listByPracticeAndUser(practiceId, userId);
        int totalScore = subs.stream().mapToInt(LessonPracticeSubmission::getScore).sum();
        int solvedCount = (int) subs.stream()
                .filter(s -> !"OBJECTIVE".equals(s.getQuestionType()) && "ACCEPTED".equals(s.getStatus()))
                .count();

        memberMapper.updateScoreAndSolved(practiceId, userId, totalScore, solvedCount);
    }

    /** 学生主动交卷。 */
    @Transactional
    public void submitPractice(String accessCode, Long userId) {
        LessonPractice practice = practiceMapper.findByAccessCode(accessCode.trim().toUpperCase());
        if (practice == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "课练不存在");
        }
        memberMapper.markSubmitted(practice.getId(), userId, LocalDateTime.now());
    }

    /** 获取教师端课堂投屏大屏实时数据。 */
    public PracticeMonitorVO getMonitorData(String accessCode, Long teacherId) {
        String code = accessCode.trim().toUpperCase();
        LessonPractice practice = practiceMapper.findByAccessCode(code);
        if (practice == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "课练不存在");
        }

        int joinedCount = memberMapper.countByPracticeId(practice.getId());
        int submittedCount = memberMapper.countSubmittedByPracticeId(practice.getId());
        int totalSubmissions = submissionMapper.countTotalByPracticeId(practice.getId());

        List<LessonPracticeMember> members = memberMapper.listByPracticeId(practice.getId());
        double avgScore = members.isEmpty() ? 0.0 : members.stream().mapToInt(LessonPracticeMember::getTotalScore).average().orElse(0.0);

        List<LessonPracticeQuestion> questions = questionMapper.listByPracticeId(practice.getId());
        int totalPossibleScore = questions.stream().mapToInt(LessonPracticeQuestion::getScore).sum();
        int perfectCount = (int) members.stream().filter(m -> m.getTotalScore() >= totalPossibleScore && totalPossibleScore > 0).count();

        LocalDateTime now = LocalDateTime.now();
        Long remainingSeconds = null;
        if ("ENDED".equals(practice.getStatus())) {
            remainingSeconds = 0L;
        } else if (practice.getEndTime() != null) {
            remainingSeconds = Math.max(0, Duration.between(now, practice.getEndTime()).getSeconds());
            if (remainingSeconds <= 0 && "ACTIVE".equals(practice.getStatus())) {
                practice.setStatus("ENDED");
                practiceMapper.updateStatusAndEndTime(practice.getId(), "ENDED", now);
                remainingSeconds = 0L;
            }
        }

        // 逐题统计
        List<PracticeMonitorVO.QuestionStatItem> questionStats = new ArrayList<>();
        for (LessonPracticeQuestion q : questions) {
            int submitCount = submissionMapper.countSubmitByPracticeQuestionId(q.getId());
            int passCount = submissionMapper.countPassByPracticeQuestionId(q.getId());
            double rate = submitCount > 0 ? (double) passCount / submitCount * 100 : 0.0;

            questionStats.add(PracticeMonitorVO.QuestionStatItem.builder()
                    .practiceQuestionId(q.getId())
                    .questionId(q.getQuestionId())
                    .questionType(q.getQuestionType())
                    .title(q.getTitle())
                    .score(q.getScore())
                    .submitCount(submitCount)
                    .passCount(passCount)
                    .passRate(Math.round(rate * 10.0) / 10.0)
                    .build());
        }

        // 实时提交流水
        List<LessonPracticeSubmission> recentSubs = submissionMapper.listRecentByPracticeId(practice.getId(), 15);
        Map<Long, String> qTitleMap = questions.stream().collect(Collectors.toMap(LessonPracticeQuestion::getId, LessonPracticeQuestion::getTitle, (a, b) -> a));

        List<PracticeMonitorVO.RecentActivityItem> recentActivities = new ArrayList<>();
        for (LessonPracticeSubmission s : recentSubs) {
            User u = userMapper.findById(s.getUserId());
            recentActivities.add(PracticeMonitorVO.RecentActivityItem.builder()
                    .id(s.getId())
                    .username(u != null ? u.getUsername() : "学生")
                    .nickname(u != null ? u.getUsername() : "学生")
                    .questionTitle(qTitleMap.getOrDefault(s.getPracticeQuestionId(), "题目"))
                    .questionType(s.getQuestionType())
                    .status(s.getStatus())
                    .score(s.getScore())
                    .createdAt(s.getCreatedAt())
                    .build());
        }

        // 天梯排行榜
        List<PracticeMonitorVO.LeaderboardItem> leaderboard = new ArrayList<>();
        int rank = 1;
        for (LessonPracticeMember m : members) {
            User u = userMapper.findById(m.getUserId());
            leaderboard.add(PracticeMonitorVO.LeaderboardItem.builder()
                    .rank(rank++)
                    .userId(m.getUserId())
                    .username(u != null ? u.getUsername() : "学生" + m.getUserId())
                    .nickname(u != null ? u.getUsername() : "学生" + m.getUserId())
                    .solvedCount(m.getSolvedCount())
                    .totalScore(m.getTotalScore())
                    .submitted(m.getSubmitted() == 1)
                    .joinedAt(m.getJoinedAt())
                    .build());
        }

        return PracticeMonitorVO.builder()
                .practiceId(practice.getId())
                .title(practice.getTitle())
                .accessCode(practice.getAccessCode())
                .durationMinutes(practice.getDurationMinutes())
                .status(practice.getStatus())
                .remainingSeconds(remainingSeconds)
                .joinedCount(joinedCount)
                .submittedCount(submittedCount)
                .totalSubmissions(totalSubmissions)
                .avgScore(Math.round(avgScore * 10.0) / 10.0)
                .perfectScoreCount(perfectCount)
                .questionStats(questionStats)
                .recentActivities(recentActivities)
                .leaderboard(leaderboard)
                .build();
    }

    /** 教师提前结束课练。 */
    @Transactional
    public void endPractice(String accessCode, Long teacherId) {
        LessonPractice practice = practiceMapper.findByAccessCode(accessCode.trim().toUpperCase());
        if (practice == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "课练不存在");
        }
        practiceMapper.updateStatusAndEndTime(practice.getId(), "ENDED", LocalDateTime.now());
    }

    /** 教师延长课练时长。 */
    @Transactional
    public void extendDuration(String accessCode, int extraMinutes, Long teacherId) {
        LessonPractice practice = practiceMapper.findByAccessCode(accessCode.trim().toUpperCase());
        if (practice == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "课练不存在");
        }
        int newDuration = practice.getDurationMinutes() + extraMinutes;
        LocalDateTime newEndTime = practice.getEndTime() != null ? practice.getEndTime().plusMinutes(extraMinutes) : LocalDateTime.now().plusMinutes(newDuration);
        practiceMapper.updateDuration(practice.getId(), newDuration, newEndTime);
    }

    /** 教师或管理员删除课练（级联清理题目、学生作答与成员记录）。教师仅可删自己的，管理员可删全部。 */
    @Transactional
    public void deletePractice(Long id, AuthUser user) {
        LessonPractice practice = practiceMapper.findById(id);
        if (practice == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "课练不存在");
        }
        boolean isAdmin = user != null && user.hasRole(Role.ADMIN);
        boolean isOwner = user != null && Objects.equals(practice.getTeacherId(), user.getId());
        if (!isAdmin && !isOwner) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权删除该课练");
        }

        submissionMapper.deleteByPracticeId(id);
        memberMapper.deleteByPracticeId(id);
        questionMapper.deleteByPracticeId(id);
        practiceMapper.deleteById(id);
    }

    /** 分页查询课练列表（支持关键词、状态及指定教师筛选）。 */
    public Map<String, Object> listPractices(String keyword, String status, Long teacherId, Long currentUserId, int page, int size) {
        int offset = Math.max(0, (page - 1) * size);
        String kw = keyword != null && !keyword.isBlank() ? keyword.trim() : null;
        String st = status != null && !status.isBlank() ? status.trim() : null;
        List<LessonPractice> list = practiceMapper.listFilter(kw, st, teacherId, offset, size);
        int total = practiceMapper.countFilter(kw, st, teacherId);

        List<PracticeSummaryVO> voList = list.stream()
                .map(p -> getSummaryVO(p, currentUserId))
                .toList();

        return Map.of("list", voList, "total", total, "page", page, "size", size);
    }

    private PracticeSummaryVO getSummaryVO(LessonPractice p, Long currentUserId) {
        User teacher = userMapper.findById(p.getTeacherId());
        String teacherName = teacher != null ? teacher.getUsername() : "教师";
        int memberCount = memberMapper.countByPracticeId(p.getId());

        List<LessonPracticeQuestion> qList = questionMapper.listByPracticeId(p.getId());
        int objectiveCount = (int) qList.stream().filter(q -> "OBJECTIVE".equals(q.getQuestionType())).count();
        int practicalCount = qList.size() - objectiveCount;

        LessonPracticeMember member = currentUserId != null ? memberMapper.findByPracticeAndUser(p.getId(), currentUserId) : null;

        boolean canSeeCode = false;
        if (currentUserId != null) {
            User currentUser = userMapper.findById(currentUserId);
            boolean isAdmin = currentUser != null && currentUser.getRoles() != null && currentUser.getRoles().contains("ADMIN");
            boolean isTeacherOfThis = Objects.equals(p.getTeacherId(), currentUserId);
            boolean isJoined = member != null;
            canSeeCode = isAdmin || isTeacherOfThis || isJoined;
        }

        return PracticeSummaryVO.builder()
                .id(p.getId())
                .title(p.getTitle())
                .description(p.getDescription())
                .accessCode(canSeeCode ? p.getAccessCode() : null)
                .durationMinutes(p.getDurationMinutes())
                .status(p.getStatus())
                .teacherId(p.getTeacherId())
                .teacherName(teacherName)
                .memberCount(memberCount)
                .questionCount(qList.size())
                .objectiveCount(objectiveCount)
                .practicalCount(practicalCount)
                .startTime(p.getStartTime())
                .endTime(p.getEndTime())
                .myJoined(member != null)
                .myScore(member != null ? member.getTotalScore() : null)
                .mySubmitted(member != null && member.getSubmitted() == 1)
                .createdAt(p.getCreatedAt())
                .build();
    }

    private List<String> parseOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(optionsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}

