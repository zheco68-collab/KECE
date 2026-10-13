package com.kece.objective.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.Data;

/** 客观题创建/更新请求。 */
@Data
public class ObjectiveQuestionRequest {

    @NotBlank(message = "题型不能为空")
    private String type;

    @NotBlank(message = "题干不能为空")
    private String content;

    /** 选项（单选/多选）。 */
    private List<String> options;

    @NotBlank(message = "答案不能为空")
    private String answer;

    private String matchType = "EXACT";

    @Min(value = 1, message = "分值不能小于 1")
    @Max(value = 100, message = "分值不能大于 100")
    private Integer score = 5;

    /** 状态：1 公开 0 私有。 */
    @Min(value = 0, message = "状态取值只能为 0 或 1")
    @Max(value = 1, message = "状态取值只能为 0 或 1")
    private Integer status = 1;
}
