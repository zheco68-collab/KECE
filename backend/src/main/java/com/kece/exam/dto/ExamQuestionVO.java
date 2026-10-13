package com.kece.exam.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 试卷题目快照条目视图对象。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamQuestionVO {

    private Long id;

    /** 原题 ID。 */
    private Long questionId;

    /** 题型：ALGO/DB/OBJECTIVE。 */
    private String questionType;

    /** 题目标题快照。 */
    private String title;

    /** 本题分值快照。 */
    private Integer score;

    /** 组卷顺序（升序）。 */
    private Integer sortOrder;
}
