package com.kece.exam.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 组卷条目请求：引用一道题目并给定分值（标题由服务端从原题快照）。 */
@Data
public class ExamQuestionRequest {

    /** 题型：ALGO/DB/OBJECTIVE。 */
    @NotBlank(message = "题型不能为空")
    private String questionType;

    @NotNull(message = "题目 ID 不能为空")
    private Long questionId;

    @NotNull(message = "分值不能为空")
    @Min(value = 1, message = "分值至少 1 分")
    private Integer score;
}
