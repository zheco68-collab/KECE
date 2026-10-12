package com.kece.objective.dto;

import lombok.Builder;
import lombok.Data;

/** 客观题作答结果。 */
@Data
@Builder
public class AnswerResultVO {

    private boolean correct;
    private int score;
    private int fullScore;
    private String correctAnswer;
}
