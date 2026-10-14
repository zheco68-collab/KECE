package com.kece.practice.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 课练算法/数据库实战题详细项。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticePracticalQuestionVO {

    private Long practiceQuestionId;
    private Long questionId;
    private String questionType; // ALGO, DB
    private String title;
    private Integer score;
    private Integer sortOrder;
    private String difficulty;
    private Integer timeLimitMs;
    private Integer memoryLimitKb;
    private List<String> languages;

    /** 用户的作答状态。 */
    private String userStatus; // ACCEPTED, WRONG_ANSWER, PENDING, UNATTEMPTED
    private Integer userScore;
    private Long submissionId;
}

