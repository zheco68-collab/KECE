package com.kece.user.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 个人资料视图对象。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileVO {

    private Long id;
    private String username;
    private String email;
    private String avatarUrl;
    private String bio;
    private String githubUrl;
    private List<String> roles;
    private LocalDateTime createdAt;
}
