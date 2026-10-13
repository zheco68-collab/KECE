package com.kece.exam.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 交卷评测结果：客观题已判分条数 + 代码/数据库题已入队条数。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitResultVO {

    private Long examId;

    /** 客观题同步判分条数。 */
    private int graded;

    /** 代码/数据库题入队条数。 */
    private int queued;
}
