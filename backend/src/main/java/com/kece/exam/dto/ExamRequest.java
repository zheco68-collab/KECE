package com.kece.exam.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

/** 试卷创建/更新请求。 */
@Data
public class ExamRequest {

    @NotBlank(message = "试卷标题不能为空")
    private String title;

    private String description;

    @NotNull(message = "考试时长不能为空")
    @Min(value = 1, message = "考试时长至少 1 分钟")
    private Integer durationMinutes;

    /** 开考时间窗口开始（NULL 表示不限制）。 */
    private java.time.LocalDateTime startTime;

    /** 最晚开考时间（NULL 表示不限制）。 */
    private java.time.LocalDateTime endTime;

    @NotBlank(message = "评分模式不能为空")
    private String mode;

    /** 状态：1公开 0私有。 */
    private Integer status;

    @NotNull(message = "试卷至少包含一道题目")
    @Size(min = 1, message = "试卷至少包含一道题目")
    private List<ExamQuestionRequest> questions;
}
