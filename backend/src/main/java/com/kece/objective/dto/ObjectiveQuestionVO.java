package com.kece.objective.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/** 客观题视图对象。答案字段仅对出题人/管理员可见。 */
@Data
@Builder
public class ObjectiveQuestionVO {

    private Long id;
    private String type;
    private String content;
    private List<String> options;
    private String answer;
    private String matchType;
    private Integer score;
    private Long createdBy;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
