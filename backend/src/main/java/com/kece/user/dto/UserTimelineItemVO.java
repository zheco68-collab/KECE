package com.kece.user.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 用户最近做题动态时间线条目。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTimelineItemVO {

    private Long submissionId;
    private Long questionId;
    private String questionTitle;

    /** 题目分类：ALGO / DB */
    private String type;

    /** 评测状态：ACCEPTED / WRONG_ANSWER / 等 */
    private String status;

    private String language;
    private Integer timeUsedMs;
    private LocalDateTime createdAt;
}
