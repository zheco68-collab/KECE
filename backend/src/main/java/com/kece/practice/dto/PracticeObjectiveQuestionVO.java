package com.kece.practice.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 课练客观题详细项。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticeObjectiveQuestionVO {

    private Long practiceQuestionId;
    private Long questionId;
    private String type; // SINGLE, MULTIPLE, FILL
    private String content;
    private List<String> options;
    private Integer score;
    private Integer sortOrder;

    /** 当前登录用户的作答状态。 */
    private String userAnswer;

    private Integer userScore;
    private Boolean isCorrect;
    private Boolean answered;
}
