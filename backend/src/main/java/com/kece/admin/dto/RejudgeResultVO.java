package com.kece.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 重判结果视图对象：单条重判返回新状态；按题批量重判返回重判条数。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejudgeResultVO {

    /** 单条重判：提交/作答记录 id。 */
    private Long submissionId;

    /** 按题批量重判：题目 id。 */
    private Long questionId;

    /** 按题批量重判：本次重判条数。 */
    private Integer rejudged;

    /** 新状态：代码/数据库题 'PENDING'；客观题 'ACCEPTED'/'WRONG_ANSWER'；批量 'DONE'。 */
    private String status;
}
