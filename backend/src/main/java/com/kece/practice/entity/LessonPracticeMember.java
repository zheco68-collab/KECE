package com.kece.practice.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 课练成员实体。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonPracticeMember {

    private Long id;
    private Long practiceId;
    private Long userId;
    private Integer totalScore;
    private Integer solvedCount;
    private LocalDateTime joinedAt;
    private Integer submitted; // 1 已交卷, 0 进行中
    private LocalDateTime submittedAt;
}

