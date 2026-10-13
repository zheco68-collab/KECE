package com.kece.exam.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 考试场次视图对象（管理列表）。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamSessionVO {

    private Long id;

    private Long examId;

    /** 所属试卷标题（JOIN exam）。 */
    private String examTitle;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer durationMinutes;

    /** 状态：UPCOMING/ONGOING/ENDED/CANCELLED。 */
    private String status;

    private Long createdBy;

    private LocalDateTime createdAt;
}
