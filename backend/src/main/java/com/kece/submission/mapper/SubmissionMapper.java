package com.kece.submission.mapper;

import com.kece.submission.dto.SubmissionListVO;
import com.kece.submission.entity.Submission;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 提交记录 Mapper。 */
@Mapper
public interface SubmissionMapper {

    int insert(Submission submission);

    Submission findById(Long id);

    List<SubmissionListVO> findPage(
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("offset") int offset,
            @Param("size") int size);

    long count(@Param("userId") Long userId, @Param("status") String status);

    int updateResult(
            @Param("id") Long id,
            @Param("status") String status,
            @Param("score") Integer score,
            @Param("timeUsedMs") Integer timeUsedMs,
            @Param("memoryUsedKb") Integer memoryUsedKb,
            @Param("errorLog") String errorLog,
            @Param("resultJson") String resultJson);
}
