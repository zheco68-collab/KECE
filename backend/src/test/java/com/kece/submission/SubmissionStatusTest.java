package com.kece.submission;

import static org.assertj.core.api.Assertions.assertThat;

import com.kece.submission.enums.SubmissionStatus;
import org.junit.jupiter.api.Test;

/** 评测状态机枚举单元测试。 */
class SubmissionStatusTest {

    @Test
    void nonTerminalStatuses_shouldRequirePolling() {
        assertThat(SubmissionStatus.PENDING.isTerminal()).isFalse();
        assertThat(SubmissionStatus.COMPILING.isTerminal()).isFalse();
        assertThat(SubmissionStatus.JUDGING.isTerminal()).isFalse();
        assertThat(SubmissionStatus.AI_REVIEWING.isTerminal()).isFalse();
    }

    @Test
    void terminalStatuses_shouldStopPolling() {
        assertThat(SubmissionStatus.ACCEPTED.isTerminal()).isTrue();
        assertThat(SubmissionStatus.WRONG_ANSWER.isTerminal()).isTrue();
        assertThat(SubmissionStatus.TIME_LIMIT_EXCEEDED.isTerminal()).isTrue();
        assertThat(SubmissionStatus.MEMORY_LIMIT_EXCEEDED.isTerminal()).isTrue();
        assertThat(SubmissionStatus.RUNTIME_ERROR.isTerminal()).isTrue();
        assertThat(SubmissionStatus.COMPILE_ERROR.isTerminal()).isTrue();
        assertThat(SubmissionStatus.SYSTEM_ERROR.isTerminal()).isTrue();
    }

    @Test
    void from_shouldReturnNullForUnknownValue() {
        assertThat(SubmissionStatus.from(null)).isNull();
        assertThat(SubmissionStatus.from("UNKNOWN")).isNull();
        assertThat(SubmissionStatus.from("ACCEPTED")).isEqualTo(SubmissionStatus.ACCEPTED);
    }
}
