package com.kece.practice.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 课练列表概览数据对象。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticeSummaryVO {

    private Long id;
    private String title;
    private String description;
    private String accessCode;
    private Integer durationMinutes;
    private String status;
    private Long teacherId;
    private String teacherName;
    private Integer memberCount;
    private Integer questionCount;
    private Integer objectiveCount;
    private Integer practicalCount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean myJoined;
    private Integer myScore;
    private Boolean mySubmitted;
    private LocalDateTime createdAt;
}

