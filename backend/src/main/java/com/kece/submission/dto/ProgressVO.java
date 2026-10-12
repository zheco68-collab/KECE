package com.kece.submission.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 评测进度（用于前端渲染进度条）。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressVO {

    private int totalCases;
    private int finishedCases;
}
