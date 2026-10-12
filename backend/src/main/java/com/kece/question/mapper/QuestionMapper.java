package com.kece.question.mapper;

import com.kece.question.entity.Question;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 算法题目 Mapper。 */
@Mapper
public interface QuestionMapper {

    int insert(Question question);

    int update(Question question);

    int deleteById(Long id);

    Question findById(Long id);

    List<Question> findPage(
            @Param("keyword") String keyword,
            @Param("difficulty") String difficulty,
            @Param("status") Integer status,
            @Param("offset") int offset,
            @Param("size") int size);

    long count(
            @Param("keyword") String keyword,
            @Param("difficulty") String difficulty,
            @Param("status") Integer status);
}
