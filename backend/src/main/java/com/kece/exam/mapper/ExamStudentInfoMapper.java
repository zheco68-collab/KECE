package com.kece.exam.mapper;

import com.kece.exam.entity.ExamStudentInfo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ExamStudentInfoMapper {

    ExamStudentInfo findByExamAndUser(@Param("examId") Long examId, @Param("userId") Long userId);

    List<ExamStudentInfo> findByExamId(@Param("examId") Long examId);

    int insert(ExamStudentInfo info);

    int update(ExamStudentInfo info);

    int deleteByExamId(@Param("examId") Long examId);
}
