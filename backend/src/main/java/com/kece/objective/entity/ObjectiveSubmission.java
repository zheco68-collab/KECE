package com.kece.objective.entity;

import java.time.LocalDateTime;
import lombok.Data;

/** 客观题作答记录实体，映射 {@code objective_submission} 表。 */
@Data
public class ObjectiveSubmission {

    private Long id;

    private Long userId;

    private Long questionId;

    /** 提交的答案原文。 */
    private String answer;

    /** 是否完全正确：1 是 / 0 否（多选漏选记 0）。 */
    private Integer correct;

    /** 本次得分。 */
    private Integer score;

    private LocalDateTime createdAt;
}
