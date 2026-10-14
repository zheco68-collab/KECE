package com.kece.user.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 近一年 AC 活跃热力图视图对象。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeatmapVO {

    /** 过去 365 天按日期排列的每日打卡列表 */
    private List<HeatmapDayVO> days;

    /** 年内累计 AC 题目总数 */
    private Integer totalAc;

    /** 最长连续打卡天数 */
    private Integer maxStreak;

    /** 当前连续打卡天数 */
    private Integer currentStreak;
}

