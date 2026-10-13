package com.kece.exam.entity;

import java.time.LocalDateTime;
import lombok.Data;

/** 试卷题目快照条目，映射 {@code exam_question} 表。 */
@Data
public class ExamQuestion {

    private Long id;

    private Long examId;

    /** 原题 ID（引用；评测时取题目当前数据）。 */
    private Long questionId;

    /** 题型：ALGO/DB/OBJECTIVE。 */
    private String questionType;

    /** 题目标题快照（组卷时定格，改题不影响已组卷）。 */
    private String title;

    /** 本题分值快照（组卷时定格）。 */
    private Integer score;

    /** 组卷顺序（升序）。 */
    private Integer sortOrder;

    private LocalDateTime createdAt;
}
