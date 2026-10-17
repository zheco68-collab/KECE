package com.kece.exam.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 单张完整的学生答卷（供教师在线查卷、批阅与导出）。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamStudentPaperVO {

    private ExamVO exam;
    private ExamStudentInfoVO studentInfo;
    private Long userId;
    private String username;
    private Integer totalScore;
    private Integer fullScore;
    private Integer solvedCount;
    private Boolean submitted;
    private LocalDateTime submittedAt;
    private List<ExamStudentPaperQuestionVO> questions;
}
