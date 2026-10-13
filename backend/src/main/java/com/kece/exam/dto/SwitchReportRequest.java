package com.kece.exam.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/** 切屏上报：delta 为本次新增的切屏次数（前端可批量合并上报）。 */
@Data
public class SwitchReportRequest {

    @Min(value = 1, message = "delta 至少为 1")
    @Max(value = 20, message = "delta 单次不超过 20")
    private Integer delta = 1;
}
