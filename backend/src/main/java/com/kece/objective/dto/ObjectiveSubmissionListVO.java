package com.kece.objective.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 客观题作答记录列表项。正确答案随判分结果公开，这里回显不影响保密性。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObjectiveSubmissionListVO {

    private Long submissionId;
    private Long questionId;

    /** 题干（Markdown 原文，前端按需截断展示）。 */
    private String questionContent;

    private String type;

    /** 用户提交的答案原文。 */
    private String answer;

    /** 是否答对：1 是 / 0 否。 */
    private Integer correct;

    /** 正确答案。 */
    private String correctAnswer;

    /** 本次得分。 */
    private Integer score;

    /** 该题满分。 */
    private Integer fullScore;

    private LocalDateTime createdAt;
}
