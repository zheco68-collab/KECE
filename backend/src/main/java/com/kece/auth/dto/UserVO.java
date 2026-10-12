package com.kece.auth.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/** 用户信息视图对象。 */
@Data
@Builder
public class UserVO {

    private Long id;
    private String username;
    private String email;
    private List<String> roles;
}
