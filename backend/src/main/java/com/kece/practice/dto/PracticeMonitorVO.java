package com.kece.practice.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 教师课堂投屏大屏监控数据对象。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticeMonitorVO {

    private Long practiceId;
    private String title;
    private String accessCode;
    private Integer durationMinutes;
    private String status;
    private Long remainingSeconds;

    /** 核心 Bento 指标。 */
    private Integer joinedCount;

    private Integer submittedCount;
    private Integer totalSubmissions;
    private Double avgScore;
    private Integer perfectScoreCount;

    /** 逐题通过率统计。 */
    private List<QuestionStatItem> questionStats;

    /** 实时提交流水动态。 */
    private List<RecentActivityItem> recentActivities;

    /** 全班作答排行榜。 */
    private List<LeaderboardItem> leaderboard;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionStatItem {
        private Long practiceQuestionId;
        private Long questionId;
        private String questionType;
        private String title;
        private Integer score;
        private Integer submitCount;
        private Integer passCount;
        private Double passRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivityItem {
        private Long id;
        private String username;
        private String nickname;
        private String questionTitle;
        private String questionType;
        private String status;
        private Integer score;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeaderboardItem {
        private Integer rank;
        private Long userId;
        private String username;
        private String nickname;
        private Integer solvedCount;
        private Integer totalScore;
        private Boolean submitted;
        private LocalDateTime joinedAt;
    }
}
