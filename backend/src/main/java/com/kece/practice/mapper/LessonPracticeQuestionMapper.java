package com.kece.practice.mapper;

import com.kece.practice.entity.LessonPracticeQuestion;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LessonPracticeQuestionMapper {

    @Insert(
            """
        <script>
        INSERT INTO `lesson_practice_question` (practice_id, question_id, question_type, title, score, sort_order)
        VALUES
        <foreach collection="list" item="item" separator=",">
            (#{item.practiceId}, #{item.questionId}, #{item.questionType}, #{item.title}, #{item.score}, #{item.sortOrder})
        </foreach>
        </script>
    """)
    int insertBatch(@Param("list") List<LessonPracticeQuestion> list);

    @Select(
            "SELECT * FROM `lesson_practice_question` WHERE practice_id = #{practiceId} ORDER BY sort_order ASC, id ASC")
    List<LessonPracticeQuestion> listByPracticeId(Long practiceId);

    @Select("SELECT * FROM `lesson_practice_question` WHERE id = #{id} LIMIT 1")
    LessonPracticeQuestion findById(Long id);

    @org.apache.ibatis.annotations.Delete(
            "DELETE FROM `lesson_practice_question` WHERE practice_id = #{practiceId}")
    int deleteByPracticeId(Long practiceId);
}
