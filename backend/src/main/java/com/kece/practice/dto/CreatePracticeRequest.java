package com.kece.practice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

/** 创建课练请求。 */
@Data
public class CreatePracticeRequest {

    @NotBlank(message = "课练标题不能为空")
    private String title;

    private String description;

    @NotNull(message = "课练时长不能为空")
    private Integer durationMinutes;

    private List<PracticeQuestionItem> questions;
}

