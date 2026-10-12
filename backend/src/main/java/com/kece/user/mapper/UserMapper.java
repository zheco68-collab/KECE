package com.kece.user.mapper;

import com.kece.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

/** 用户表 Mapper。 */
@Mapper
public interface UserMapper {

    User findById(Long id);

    User findByUsername(String username);

    User findByEmail(String email);

    int insert(User user);
}
