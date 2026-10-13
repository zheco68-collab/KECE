package com.kece.exam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 考试作答提交请求。 */
@Data
public class ExamAnswerRequest {

    /** 试卷题目快照条目 ID。 */
    @NotNull(message = "题目快照 ID 不能为空")
    private Long examQuestionId;

    /** 作答内容：客观题为答案串；数据库题为 SQL 原文；代码题为多文件 JSON（字符串形式）。 */
    @NotBlank(message = "作答内容不能为空")
    private String answer;

    /** 代码题必需的语言（cpp/java/python），其余题型忽略。 */
    private String language;
}
