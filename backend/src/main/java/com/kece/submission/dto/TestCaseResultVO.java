package com.kece.submission.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 单个测试用例评测结果。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseResultVO {

    private Long caseId;
    private String status;
    private Integer timeMs;
    private Integer memoryKb;

    /** 仅在练习模式且用例错误时返回，便于调试。 */
    private String expectedOutput;

    /** 仅在练习模式且用例错误时返回，便于调试。 */
    private String actualOutput;
}
