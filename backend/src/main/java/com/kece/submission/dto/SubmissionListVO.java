package com.kece.submission.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 提交记录列表项视图对象（不含代码与逐用例详情）。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionListVO {

    private Long submissionId;
    private Long questionId;
    private String questionTitle;
    private String language;
    private String codeContent;
    private String status;
    private Integer score;
    private Integer timeUsedMs;
    private Integer memoryUsedKb;
    private LocalDateTime createdAt;
}
