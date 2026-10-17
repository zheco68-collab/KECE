package com.kece.exam.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 考生在考场中已暂存的单题作答记录。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSavedAnswerVO {

    private Long examQuestionId;

    private String answerContent;

    private String language;

    private String status;

    private Integer score;

    private Long submissionId;
}
