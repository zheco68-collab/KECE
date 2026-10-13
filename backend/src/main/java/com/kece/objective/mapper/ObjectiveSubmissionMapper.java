package com.kece.objective.mapper;

import com.kece.objective.dto.ObjectiveSubmissionListVO;
import com.kece.objective.entity.ObjectiveSubmission;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 客观题作答记录 Mapper。 */
@Mapper
public interface ObjectiveSubmissionMapper {

    int insert(ObjectiveSubmission submission);

    /** 当前用户已答对的客观题 id（去重）。 */
    List<Long> findCorrectQuestionIds(@Param("userId") Long userId);

    /** 作答记录分页：correct 为 null 表示不筛对错。 */
    List<ObjectiveSubmissionListVO> findPage(
            @Param("userId") Long userId,
            @Param("correct") Integer correct,
            @Param("offset") int offset,
            @Param("size") int size);

    long count(@Param("userId") Long userId, @Param("correct") Integer correct);
}
