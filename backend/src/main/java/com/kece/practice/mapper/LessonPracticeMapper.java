package com.kece.practice.mapper;

import com.kece.practice.entity.LessonPractice;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface LessonPracticeMapper {

    @Insert(
            """
        INSERT INTO `lesson_practice` (title, description, access_code, duration_minutes, status, teacher_id, start_time, end_time)
        VALUES (#{title}, #{description}, #{accessCode}, #{durationMinutes}, #{status}, #{teacherId}, #{startTime}, #{endTime})
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(LessonPractice practice);

    @Select("SELECT * FROM `lesson_practice` WHERE id = #{id} LIMIT 1")
    LessonPractice findById(Long id);

    @Select("SELECT * FROM `lesson_practice` WHERE access_code = #{accessCode} LIMIT 1")
    LessonPractice findByAccessCode(String accessCode);

    @Select("SELECT * FROM `lesson_practice` ORDER BY id DESC LIMIT #{offset}, #{limit}")
    List<LessonPractice> listAll(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM `lesson_practice`")
    int countAll();

    @Select(
            """
        <script>
        SELECT * FROM `lesson_practice`
        <where>
            <if test="keyword != null and keyword != ''">
                (title LIKE CONCAT('%', #{keyword}, '%') OR access_code LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="status != null and status != ''">
                AND status = #{status}
            </if>
            <if test="teacherId != null">
                AND teacher_id = #{teacherId}
            </if>
        </where>
        ORDER BY id DESC
        LIMIT #{offset}, #{limit}
        </script>
    """)
    List<LessonPractice> listFilter(
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("teacherId") Long teacherId,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Select(
            """
        <script>
        SELECT COUNT(*) FROM `lesson_practice`
        <where>
            <if test="keyword != null and keyword != ''">
                (title LIKE CONCAT('%', #{keyword}, '%') OR access_code LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="status != null and status != ''">
                AND status = #{status}
            </if>
            <if test="teacherId != null">
                AND teacher_id = #{teacherId}
            </if>
        </where>
        </script>
    """)
    int countFilter(
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("teacherId") Long teacherId);

    @Select("SELECT * FROM `lesson_practice` WHERE teacher_id = #{teacherId} ORDER BY id DESC")
    List<LessonPractice> listByTeacherId(Long teacherId);

    @Update("UPDATE `lesson_practice` SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Update(
            "UPDATE `lesson_practice` SET status = #{status}, end_time = #{endTime} WHERE id = #{id}")
    int updateStatusAndEndTime(
            @Param("id") Long id,
            @Param("status") String status,
            @Param("endTime") LocalDateTime endTime);

    @Update(
            "UPDATE `lesson_practice` SET duration_minutes = #{durationMinutes}, end_time = #{endTime} WHERE id = #{id}")
    int updateDuration(
            @Param("id") Long id,
            @Param("durationMinutes") int durationMinutes,
            @Param("endTime") LocalDateTime endTime);

    @org.apache.ibatis.annotations.Delete("DELETE FROM `lesson_practice` WHERE id = #{id}")
    int deleteById(Long id);
}
