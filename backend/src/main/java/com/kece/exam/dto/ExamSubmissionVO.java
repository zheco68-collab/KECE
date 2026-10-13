package com.kece.exam.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 我的考试作答/成绩视图对象。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamSubmissionVO {

    /** 题目快照条目 ID。 */
    private Long examQuestionId;

    /** 原题 ID。 */
    private Long questionId;

    /** 题型：ALGO/DB/OBJECTIVE。 */
    private String questionType;

    /** 题目标题快照。 */
    private String title;

    /** 判分成绩（代码/DB 题以 submission 表为准）。 */
    private Integer score;

    /** 本题分值快照。 */
    private Integer fullScore;

    /** 作答内容（代码/DB 题前端截断展示）。 */
    private String answer;

    /** 代码题语言。 */
    private String language;

    /** 评测状态：PENDING/ACCEPTED/WRONG_ANSWER。 */
    private String status;
}
