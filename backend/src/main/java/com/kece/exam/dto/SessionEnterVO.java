package com.kece.exam.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 考生进入考试的视图对象：包含倒计时与题目快照。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionEnterVO {

    private Long id;

    private Long examId;

    private String examTitle;

    /** 评分模式：ACM/OI。 */
    private String mode;

    private Integer durationMinutes;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /** 状态：UPCOMING/ONGOING/ENDED/CANCELLED。 */
    private String status;

    /** 距结束剩余秒数（倒计时依据）。 */
    private long remainingSeconds;

    /** 是否要求填写考生信息。 */
    private Boolean requireStudentInfo;

    /** 当前考生是否已登记考生信息。 */
    private Boolean hasFilledStudentInfo;

    /** 当前考生的登记信息。 */
    private ExamStudentInfoVO studentInfo;

    /** 题目快照 + 实时题面（按 sort_order）。 */
    private List<ExamSessionQuestionVO> questions;

    /** 考生已暂存的题目作答映射：key 为 examQuestionId。 */
    private java.util.Map<Long, UserSavedAnswerVO> myAnswers;
}
