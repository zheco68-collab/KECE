package com.kece.practice.mapper;

import com.kece.practice.entity.LessonPracticeSubmission;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface LessonPracticeSubmissionMapper {

    @Insert(
            """
        INSERT INTO `lesson_practice_submission` (
            practice_id, practice_question_id, user_id, question_type, answer, submission_id, status, score, result_json, created_at
        ) VALUES (
            #{practiceId}, #{practiceQuestionId}, #{userId}, #{questionType}, #{answer}, #{submissionId}, #{status}, #{score}, #{resultJson}, #{createdAt}
        )
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(LessonPracticeSubmission submission);

    @Select(
            """
        SELECT * FROM `lesson_practice_submission`
        WHERE practice_id = #{practiceId} AND user_id = #{userId} AND practice_question_id = #{practiceQuestionId}
        ORDER BY id DESC LIMIT 1
    """)
    LessonPracticeSubmission findByPracticeUserQuestion(
            @Param("practiceId") Long practiceId,
            @Param("userId") Long userId,
            @Param("practiceQuestionId") Long practiceQuestionId);

    @Select(
            """
        SELECT * FROM `lesson_practice_submission`
        WHERE practice_id = #{practiceId} AND user_id = #{userId}
        ORDER BY id DESC
    """)
    List<LessonPracticeSubmission> listByPracticeAndUser(
            @Param("practiceId") Long practiceId, @Param("userId") Long userId);

    @Select(
            """
        SELECT * FROM `lesson_practice_submission`
        WHERE practice_id = #{practiceId}
        ORDER BY id DESC LIMIT #{limit}
    """)
    List<LessonPracticeSubmission> listRecentByPracticeId(
            @Param("practiceId") Long practiceId, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM `lesson_practice_submission` WHERE practice_id = #{practiceId}")
    int countTotalByPracticeId(Long practiceId);

    @Select(
            "SELECT COUNT(DISTINCT user_id) FROM `lesson_practice_submission` WHERE practice_question_id = #{practiceQuestionId} AND status = 'ACCEPTED'")
    int countPassByPracticeQuestionId(Long practiceQuestionId);

    @Select(
            "SELECT COUNT(*) FROM `lesson_practice_submission` WHERE practice_question_id = #{practiceQuestionId}")
    int countSubmitByPracticeQuestionId(Long practiceQuestionId);

    @Update(
            """
        UPDATE `lesson_practice_submission`
        SET answer = #{answer}, submission_id = #{submissionId}, status = #{status}, score = #{score}, result_json = #{resultJson}
        WHERE id = #{id}
    """)
    int updateSubmission(LessonPracticeSubmission submission);

    @org.apache.ibatis.annotations.Delete(
            "DELETE FROM `lesson_practice_submission` WHERE practice_id = #{practiceId}")
    int deleteByPracticeId(Long practiceId);
}
