package com.kece.submission.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 评测结果详情。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionResultVO {

    private Integer score;
    private Integer timeUsedMs;
    private Integer memoryUsedKb;

    /** 编译失败时的报错信息。 */
    private String compileErrorLog;

    private List<TestCaseResultVO> testCasesReport;
}
