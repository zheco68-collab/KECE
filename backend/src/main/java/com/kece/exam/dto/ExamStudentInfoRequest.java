package com.kece.exam.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 考生实名登记提交请求。 */
@Data
public class ExamStudentInfoRequest {

    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    @NotBlank(message = "学号/准考证号不能为空")
    private String studentNo;

    private String college;

    private String major;

    private String className;
}
