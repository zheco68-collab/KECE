package com.kece.exam.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 考试考生实名信息，映射 {@code exam_student_info} 表。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamStudentInfo {

    private Long id;
    private Long examId;
    private Long userId;
    private String realName;
    private String studentNo;
    private String college;
    private String major;
    private String className;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
