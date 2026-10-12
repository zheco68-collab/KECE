package com.kece.database.mapper;

import com.kece.database.entity.DbQuestion;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 数据库题目 Mapper。 */
@Mapper
public interface DbQuestionMapper {

    int insert(DbQuestion question);

    int update(DbQuestion question);

    int deleteById(Long id);

    DbQuestion findById(Long id);

    List<DbQuestion> findPage(
            @Param("type") String type,
            @Param("keyword") String keyword,
            @Param("status") Integer status,
            @Param("offset") int offset,
            @Param("size") int size);

    long count(
            @Param("type") String type,
            @Param("keyword") String keyword,
            @Param("status") Integer status);
}
