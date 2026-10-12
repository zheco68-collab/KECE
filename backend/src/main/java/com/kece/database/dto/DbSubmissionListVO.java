package com.kece.database.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 数据库提交记录列表项视图对象。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DbSubmissionListVO {

    private Long submissionId;
    private Long questionId;
    private String questionTitle;
    private String status;
    private Integer score;
    private Integer timeUsedMs;
    private String sqlContent;
    private LocalDateTime createdAt;
}
