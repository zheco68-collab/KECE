package com.kece.exam.entity;

import java.time.LocalDateTime;
import lombok.Data;

/** 试卷实体，映射 {@code exam} 表。 */
@Data
public class Exam {

    private Long id;

    private String title;

    private String description;

    /** 考试时长（分钟）。 */
    private Integer durationMinutes;

    /** 开考时间窗口开始（NULL 表示不限制）。 */
    private LocalDateTime startTime;

    /** 最晚开考时间（NULL 表示不限制）。 */
    private LocalDateTime endTime;

    /** 评分模式：ACM/OI。 */
    private String mode;

    /** 可见性：1公开 0私有。 */
    private Integer status;

    /** 考试状态：UPCOMING/ONGOING/ENDED/CANCELLED（惰性推进）。 */
    private String sessionStatus;

    private Long createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
