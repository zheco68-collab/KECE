package com.kece.exam.mapper;

import com.kece.exam.entity.Exam;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 试卷 Mapper。 */
@Mapper
public interface ExamMapper {

    int insert(Exam exam);

    int update(Exam exam);

    int deleteById(Long id);

    Exam findById(Long id);

    List<Exam> findPage(
            @Param("keyword") String keyword,
            @Param("status") Integer status,
            @Param("createdBy") Long createdBy,
            @Param("offset") int offset,
            @Param("size") int size);

    long count(
            @Param("keyword") String keyword,
            @Param("status") Integer status,
            @Param("createdBy") Long createdBy);

    /** 该考试是否已有作答记录（删除/改题保护：有作答后题目快照不可再改、考试不可删除）。 */
    int countSubmissions(@Param("examId") Long examId);

    /** 更新考试状态（UPCOMING/ONGOING/ENDED/CANCELLED）。 */
    int updateSessionStatus(@Param("id") Long id, @Param("sessionStatus") String sessionStatus);
}
