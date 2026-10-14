package com.kece.practice.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 课练房间实体。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonPractice {

    private Long id;
    private String title;
    private String description;
    private String accessCode;
    private Integer durationMinutes;
    private String status; // ACTIVE, ENDED
    private Long teacherId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

