package com.kece.exam.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 考试排名行：ACM 看 AC 数与罚时；OI 看总分。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamRankVO {

    private Long userId;

    private String username;

    /** 通过题数（ACM 排序主键 / OI 展示）。 */
    private int solved;

    /** ACM 罚时（分钟）：首 AC 距开考分钟 + 每题 20 分 × 首 AC 前错误次数。 */
    private long penaltyMinutes;

    /** OI 总分。 */
    private int totalScore;

    /** 已作答题数。 */
    private int finished;

    /** 试卷全部分值之和。 */
    private int fullScore;
}
