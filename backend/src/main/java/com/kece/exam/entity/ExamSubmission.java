package com.kece.exam.entity;

import java.time.LocalDateTime;
import lombok.Data;

/** 考试提交实体，映射 {@code exam_submission} 表。 */
@Data
public class ExamSubmission {

    private Long id;

    private Long examId;

    private Long userId;

    /** 试卷题目快照条目 ID。 */
    private Long examQuestionId;

    /** 作答内容（代码多文件 JSON / SQL 原文 / 客观题答案）。 */
    private String answerContent;

    /** 代码题语言（cpp/java/python），其余题型为空。 */
    private String language;

    /** 评测状态：PENDING（待统一评测）/ACCEPTED/WRONG_ANSWER 等。 */
    private String status;

    /** 是否已随交卷评测：1 是 / 0 否（交卷幂等标记）。 */
    private Integer submitted;

    /** 得分（客观题判分后落位；代码/数据库题成绩以 submission 表为准）。 */
    private Integer score;

    /** 评测详情 JSON。 */
    private String resultJson;

    private LocalDateTime submittedAt;

    private LocalDateTime createdAt;
}
