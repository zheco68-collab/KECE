package com.kece.objective.entity;

import java.time.LocalDateTime;
import lombok.Data;

/** 客观题实体，映射 {@code objective_question} 表。 */
@Data
public class ObjectiveQuestion {

    private Long id;

    /** 题型：SINGLE/MULTIPLE/FILL。 */
    private String type;

    /** 题干（Markdown）。 */
    private String content;

    /** 选项（单选/多选），JSON 数组字符串。 */
    private String options;

    /** 答案：单选 "A"；多选 "A,C"；填空关键字或正则。 */
    private String answer;

    /** 填空匹配方式：EXACT/IGNORE_CASE/REGEX。 */
    private String matchType;

    private Integer score;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
