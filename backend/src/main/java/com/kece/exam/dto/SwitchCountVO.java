package com.kece.exam.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 切屏计数视图对象。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SwitchCountVO {

    private Long examId;

    private Long userId;

    /** 当前累计切屏次数。 */
    private long switchCount;
}
