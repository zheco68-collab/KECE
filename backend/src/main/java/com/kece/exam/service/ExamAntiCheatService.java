package com.kece.exam.service;

import com.kece.common.exception.BusinessException;
import com.kece.common.response.ResultCode;
import com.kece.common.security.AuthContextHolder;
import com.kece.common.security.AuthUser;
import com.kece.exam.dto.SwitchCountVO;
import com.kece.exam.entity.Exam;
import com.kece.exam.entity.ExamSessionStatus;
import com.kece.exam.mapper.ExamMapper;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 考试防作弊基础：切屏计数（Redis 计数器，按考试+考生维度）。
 *
 * <p>仅考试进行中（ONGOING）计数；未开始/已结束/已取消时上报静默忽略并返回当前值， 避免到点自动交卷边界场景前端收到报错。计数器不设过期：监考数据量小，随考试数据生命周期保留。
 */
@Service
@RequiredArgsConstructor
public class ExamAntiCheatService {

    private static final String SWITCH_KEY_PREFIX = "exam:anti:switch:";

    private final ExamMapper examMapper;
    private final StringRedisTemplate redis;

    /** 上报切屏：ONGOING 时累加并返回最新计数；非进行中只读不加。 */
    public SwitchCountVO reportSwitch(Long examId, int delta) {
        AuthUser user = requireUser();
        Exam exam = getExamOrThrow(examId);
        ExamSessionStatus status = effectiveStatus(exam);
        if (status == ExamSessionStatus.CANCELLED) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该考试已取消");
        }
        String key = switchKey(examId, user.getId());
        long count = currentCount(key);
        if (status == ExamSessionStatus.ONGOING && delta > 0) {
            count = redis.opsForValue().increment(key, delta);
        }
        return SwitchCountVO.builder()
                .examId(examId)
                .userId(user.getId())
                .switchCount(count)
                .build();
    }

    /** 按当前时间计算真实状态（session_status 为惰性推进，此处只读判断不落库）。 */
    private ExamSessionStatus effectiveStatus(Exam exam) {
        if (ExamSessionStatus.CANCELLED.name().equals(exam.getSessionStatus())) {
            return ExamSessionStatus.CANCELLED;
        }
        LocalDateTime now = LocalDateTime.now();
        if (exam.getEndTime() != null && !now.isBefore(exam.getEndTime())) {
            return ExamSessionStatus.ENDED;
        }
        if (exam.getStartTime() != null && now.isBefore(exam.getStartTime())) {
            return ExamSessionStatus.UPCOMING;
        }
        return ExamSessionStatus.ONGOING;
    }

    /** 查询本人切屏计数（考试页/结果页展示用）。 */
    public SwitchCountVO mySwitchCount(Long examId) {
        AuthUser user = requireUser();
        getExamOrThrow(examId);
        return SwitchCountVO.builder()
                .examId(examId)
                .userId(user.getId())
                .switchCount(currentCount(switchKey(examId, user.getId())))
                .build();
    }

    /** 指定考生的切屏计数（enter/ranking 组装用，免鉴权，内部调用）。 */
    public long switchCountOf(Long examId, Long userId) {
        return currentCount(switchKey(examId, userId));
    }

    /** 批量取多个考生的切屏计数（排名组装用，避免逐行往返）。 */
    public Map<Long, Long> switchCountsOf(Long examId, List<Long> userIds) {
        Map<Long, Long> result = new HashMap<>();
        for (Long userId : userIds) {
            result.put(userId, currentCount(switchKey(examId, userId)));
        }
        return result;
    }

    private long currentCount(String key) {
        String value = redis.opsForValue().get(key);
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String switchKey(Long examId, Long userId) {
        return SWITCH_KEY_PREFIX + examId + ":" + userId;
    }

    private Exam getExamOrThrow(Long id) {
        Exam exam = examMapper.findById(id);
        if (exam == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "考试不存在");
        }
        return exam;
    }

    private AuthUser requireUser() {
        AuthUser user = AuthContextHolder.get();
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return user;
    }
}
