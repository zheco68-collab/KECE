package com.kece.objective.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 客观题作答请求。 */
@Data
public class AnswerSubmitRequest {

    @NotBlank(message = "答案不能为空")
    private String answer;
}
