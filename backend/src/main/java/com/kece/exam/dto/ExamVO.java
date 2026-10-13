package com.kece.exam.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 试卷视图对象。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamVO {

    private Long id;

    private String title;

    private String description;

    private Integer durationMinutes;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /** 评分模式：ACM/OI。 */
    private String mode;

    /** 可见性：1公开 0私有。 */
    private Integer status;

    /** 考试状态：UPCOMING/ONGOING/ENDED/CANCELLED。 */
    private String sessionStatus;

    /** 当前登录用户是否已提交交卷。 */
    private Boolean submitted;

    private Long createdBy;

    /** 题目快照条目（列表接口为空，详情接口携带）。 */
    private List<ExamQuestionVO> questions;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
