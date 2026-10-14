package com.kece.practice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 课练关联题目请求条目。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticeQuestionItem {

    @NotNull(message = "原题目ID不能为空")
    private Long questionId;

    @NotBlank(message = "题型不能为空(OBJECTIVE/ALGO/DB)")
    private String questionType;

    private String title;

    private Integer score;

    private Integer sortOrder;
}

