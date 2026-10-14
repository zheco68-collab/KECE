package com.kece.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 单日提交打卡视图。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeatmapDayVO {

    /** 格式 yyyy-MM-dd */
    private String date;

    /** 当天 AC 通过数 */
    private Integer count;
}

