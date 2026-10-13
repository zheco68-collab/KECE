package com.kece.exam.entity;

/** 考试场次状态机：UPCOMING → ONGOING → ENDED（或 CANCELLED）。 */
public enum ExamSessionStatus {
    UPCOMING,
    ONGOING,
    ENDED,
    CANCELLED;

    /** 是否允许继续作答（未结束且未取消）。 */
    public boolean isActive() {
        return this == UPCOMING || this == ONGOING;
    }
}
