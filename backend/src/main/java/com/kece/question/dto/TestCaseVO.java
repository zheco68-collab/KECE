package com.kece.question.dto;

import lombok.Builder;
import lombok.Data;

/** 测试用例视图对象。 */
@Data
@Builder
public class TestCaseVO {

    private Long id;
    private String input;
    private String expectedOutput;
    private Integer isSample;
}
