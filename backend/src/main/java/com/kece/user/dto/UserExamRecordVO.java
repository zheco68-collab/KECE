package com.kece.user.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 用户考试战绩卡片视图。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserExamRecordVO {

    private Long examId;
    private String title;
    private String description;
    private String mode;
    private Integer durationMinutes;
    private Integer score;
    private Integer rank;
    private Integer totalParticipants;
    private LocalDateTime submittedAt;
}
