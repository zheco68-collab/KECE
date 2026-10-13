package com.kece.exam.mapper;

import com.kece.exam.entity.ExamQuestion;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/** 试卷题目快照 Mapper。 */
@Mapper
public interface ExamQuestionMapper {

    int insert(ExamQuestion examQuestion);

    int deleteByExamId(Long examId);

    ExamQuestion findById(Long id);

    List<ExamQuestion> findByExamId(Long examId);
}
