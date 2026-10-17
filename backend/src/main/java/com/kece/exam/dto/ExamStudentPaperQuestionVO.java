package com.kece.exam.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 单张学生试卷中的题目及答卷明细。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamStudentPaperQuestionVO {

    private Long examQuestionId;
    private Long questionId;
    private String questionType;
    private String objectiveType;
    private String title;
    private String content;
    private List<String> options;
    private Integer fullScore;
    private Integer studentScore;
    private String status;
    private String studentAnswer;
    private String standardAnswer;
    private Boolean isCorrect;
    private String language;
    private String resultJson;
}
