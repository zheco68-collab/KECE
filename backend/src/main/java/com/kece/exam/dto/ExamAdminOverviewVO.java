package com.kece.exam.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 教师后台考试总览及考生试卷列表。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamAdminOverviewVO {

    private ExamVO exam;
    private Integer totalStudents;
    private Integer submittedCount;
    private Double avgScore;
    private Integer maxScore;
    private Integer minScore;
    private List<ExamStudentSubmissionRowVO> students;
}
