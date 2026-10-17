package com.kece.exam.dto;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 考试中考生成绩概览行（教师端查看考生试卷列表）。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamStudentSubmissionRowVO {

    private Long userId;
    private String username;
    private String realName;
    private String studentNo;
    private String college;
    private String major;
    private String className;
    private Integer totalScore;
    private Integer fullScore;
    private Integer solvedCount;
    private Integer totalQuestions;
    private Boolean submitted;
    private LocalDateTime submittedAt;
    private Long penaltyMinutes;
    private Map<Long, Integer> questionScores;
    private Map<Long, String> questionStatuses;
}
