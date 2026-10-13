package com.kece.exam.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 考生进入考试的单题视图：快照 + 实时题面（作答所需）。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamSessionQuestionVO {

    /** 题目快照条目 ID。 */
    private Long id;

    /** 原题 ID。 */
    private Long questionId;

    /** 题型：ALGO/DB/OBJECTIVE。 */
    private String questionType;

    /** 题目标题快照。 */
    private String title;

    /** 本题分值快照。 */
    private Integer score;

    /** 组卷顺序。 */
    private Integer sortOrder;

    /** 题面（Markdown）：算法题取描述，数据库题含建表语句，客观题取题干。 */
    private String content;

    /** 客观题选项（JSON 数组已解析）。 */
    private List<String> options;

    /** 客观题题型：SINGLE/MULTIPLE/FILL。 */
    private String objectiveType;

    /** 数据库题建表语句（供考生参考表结构）。 */
    private String schemaSql;

    /** 算法题时间限制（ms）。 */
    private Integer timeLimitMs;

    /** 算法题内存限制（MB）。 */
    private Integer memoryLimitMb;
}
