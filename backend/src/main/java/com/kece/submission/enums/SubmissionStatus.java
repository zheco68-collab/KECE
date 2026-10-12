package com.kece.submission.enums;

/** 评测状态机枚举。非终态需继续轮询，终态停止轮询。 */
public enum SubmissionStatus {
    PENDING,
    COMPILING,
    JUDGING,
    AI_REVIEWING,
    ACCEPTED,
    WRONG_ANSWER,
    TIME_LIMIT_EXCEEDED,
    MEMORY_LIMIT_EXCEEDED,
    RUNTIME_ERROR,
    COMPILE_ERROR,
    SYSTEM_ERROR;

    /** 是否为终态（停止轮询）。 */
    public boolean isTerminal() {
        return switch (this) {
            case ACCEPTED,
                            WRONG_ANSWER,
                            TIME_LIMIT_EXCEEDED,
                            MEMORY_LIMIT_EXCEEDED,
                            RUNTIME_ERROR,
                            COMPILE_ERROR,
                            SYSTEM_ERROR ->
                    true;
            default -> false;
        };
    }

    /** 将字符串转换为枚举，未知值返回 {@code null}。 */
    public static SubmissionStatus from(String value) {
        if (value == null) {
            return null;
        }
        try {
            return valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
