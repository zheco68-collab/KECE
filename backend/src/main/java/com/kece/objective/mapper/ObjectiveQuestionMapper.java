package com.kece.objective.mapper;

import com.kece.objective.entity.ObjectiveQuestion;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 客观题 Mapper。 */
@Mapper
public interface ObjectiveQuestionMapper {

    int insert(ObjectiveQuestion question);

    int update(ObjectiveQuestion question);

    int deleteById(Long id);

    ObjectiveQuestion findById(Long id);

    List<ObjectiveQuestion> findPage(
            @Param("type") String type,
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("size") int size);

    long count(@Param("type") String type, @Param("keyword") String keyword);
}
