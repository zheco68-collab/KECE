package com.kece.exam.mapper;

import com.kece.exam.entity.ExamSubmission;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 考试提交 Mapper。 */
@Mapper
public interface ExamSubmissionMapper {

    int insert(ExamSubmission submission);

    int updateAnswer(
            @Param("id") Long id,
            @Param("answerContent") String answerContent,
            @Param("language") String language,
            @Param("status") String status);

    int updateResult(
            @Param("id") Long id,
            @Param("status") String status,
            @Param("score") Integer score,
            @Param("resultJson") String resultJson);

    /** 更新评测结果但不锁定（ACM 考试中单题实时提交用，允许重复提交覆盖）。 */
    int updateJudgeResult(
            @Param("id") Long id,
            @Param("status") String status,
            @Param("score") Integer score,
            @Param("resultJson") String resultJson);

    /** 交卷幂等标记：置 submitted=1。 */
    int markSubmitted(@Param("id") Long id);

    /** 按唯一键（考试+考生+题目快照条目）查询。 */
    ExamSubmission findByUnique(
            @Param("examId") Long examId,
            @Param("userId") Long userId,
            @Param("examQuestionId") Long examQuestionId);

    /** 考试内某考生的全部作答。 */
    List<ExamSubmission> findByExamAndUser(
            @Param("examId") Long examId, @Param("userId") Long userId);

    /** 考试内全部作答（排名聚合用）。 */
    List<ExamSubmission> findByExam(@Param("examId") Long examId);

    /** 考试内提交/作答记录数（删除保护）。 */
    int countByExam(@Param("examId") Long examId);

    /** 删除考试的全部作答记录（ADMIN 强制删除考试时级联清理）。 */
    int deleteByExam(@Param("examId") Long examId);
}
