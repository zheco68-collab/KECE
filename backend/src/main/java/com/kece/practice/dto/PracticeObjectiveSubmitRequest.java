package com.kece.practice.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.Map;
import lombok.Data;

/** 客观题集中提交请求。 */
@Data
public class PracticeObjectiveSubmitRequest {

    /** 题目 practiceQuestionId 到 作答文本 的映射。例如 { 101: "A", 102: "A,B" }。 */
    @NotEmpty(message = "作答内容不能为空")
    private Map<Long, String> answers;
}

