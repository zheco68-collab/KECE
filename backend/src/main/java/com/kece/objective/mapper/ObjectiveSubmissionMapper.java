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
            @Param("questionId") Long questionId,
            @Param("correct") Integer correct,
            @Param("offset") int offset,
            @Param("size") int size);

    long count(
            @Param("userId") Long userId,
            @Param("questionId") Long questionId,
            @Param("correct") Integer correct);

    /** 题目是否存在作答记录（删除保护）。 */
    boolean existsByQuestionId(@Param("questionId") Long questionId);

    /** 删除某题目的全部作答记录（ADMIN 强制删除题目时级联清理）。 */
    int deleteByQuestionId(@Param("questionId") Long questionId);

    /** 按主键查询作答记录（重判用）。 */
    ObjectiveSubmission findById(Long id);

    /** 按题查询全部作答记录（批量重判用，仅需 id 与答案）。 */
    List<ObjectiveSubmission> findByQuestionId(@Param("questionId") Long questionId);

    /** 重判回写：同时覆盖对错与得分（多选漏选是半分，只改一个会错）。 */
    int updateCorrectAndScore(
            @Param("id") Long id, @Param("correct") Integer correct, @Param("score") Integer score);
}
