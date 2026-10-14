package com.kece.practice.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 客观题集中提交判定结果。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticeObjectiveResultVO {

    private Integer totalScore;
    private Integer fullScore;
    private Integer correctCount;
    private Integer totalCount;

    private List<ItemResult> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemResult {
        private Long practiceQuestionId;
        private Long questionId;
        private Boolean correct;
        private Integer score;
        private Integer fullScore;
        private String userAnswer;
        private String correctAnswer;
    }
}

