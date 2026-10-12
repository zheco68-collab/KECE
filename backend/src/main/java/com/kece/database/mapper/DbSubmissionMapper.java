package com.kece.database.mapper;

import com.kece.database.dto.DbSubmissionListVO;
import com.kece.database.entity.DbSubmission;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 数据库提交记录 Mapper。 */
@Mapper
public interface DbSubmissionMapper {

    int insert(DbSubmission submission);

    DbSubmission findById(Long id);

    List<DbSubmissionListVO> findPage(
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
            @Param("resultJson") String resultJson);
}
