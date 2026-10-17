package com.kece.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kece.objective.dto.AnswerResultVO;
import com.kece.objective.entity.ObjectiveQuestion;
import com.kece.objective.entity.ObjectiveSubmission;
import com.kece.objective.mapper.ObjectiveQuestionMapper;
import com.kece.objective.mapper.ObjectiveSubmissionMapper;
import com.kece.objective.service.ObjectiveGrader;
import com.kece.user.entity.User;
import com.kece.user.mapper.UserMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * 客观题示例数据播种：首次启动时写入一组计算机网络例题与演示作答，便于直接试用与验收。
 *
 * <p>受 {@code kece.demo-data.enabled} 控制（默认关、仅 dev profile 开）；由 {@link DataInitializer} 播种完 teacher
 * 账号后执行（{@code @Order(2)}），避免新库首次启动找不到出题人而跳过。
 *
 * <p>幂等策略：题目按题干精确查重，作答按「用户+题目」查重，重复启动不产生重复数据； 后续被出题人改动或删除也不会被重新写回（只在缺失时补种）。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ObjectiveQuestionSeeder {

    private final ObjectiveQuestionMapper questionMapper;
    private final ObjectiveSubmissionMapper submissionMapper;
    private final ObjectiveGrader grader;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    @Value("${kece.demo-data.enabled:false}")
    private boolean demoDataEnabled;

    @Order(2)
    @Bean
    public ApplicationRunner seedObjectiveQuestions() {
        return args -> {
            if (!demoDataEnabled) {
                log.info("演示数据开关已关闭（kece.demo-data.enabled=false），跳过客观题示例播种");
                return;
            }
            User teacher = userMapper.findByUsername("teacher");
            if (teacher == null) {
                log.warn("未找到 teacher 账号，跳过客观题示例数据播种");
                return;
            }
            int created = 0;
            for (Seed q : SEEDS) {
                created += ensureQuestion(q, teacher.getId());
            }
            if (created > 0) {
                log.info("已播种 {} 道客观题示例（计算机网络）", created);
            }
            int submissions = ensureDemoSubmissions(teacher.getId());
            if (submissions > 0) {
                log.info("已播种 {} 条客观题示例作答（演示提交）", submissions);
            }
        };
    }

    /** 演示作答：给种子题目补少量作答记录，让后台提交监控在全新 dev 库也有数据。 */
    private int ensureDemoSubmissions(Long userId) {
        int created = 0;
        for (DemoSubmission demo : DEMO_SUBMISSIONS) {
            created += ensureDemoSubmission(userId, demo);
        }
        return created;
    }

    private int ensureDemoSubmission(Long userId, DemoSubmission demo) {
        ObjectiveQuestion question = questionMapper.findByContent(demo.content());
        if (question == null) {
            return 0;
        }
        if (!submissionMapper.findPage(userId, question.getId(), null, 0, 1).isEmpty()) {
            return 0;
        }
        AnswerResultVO result = grader.grade(question, demo.answer());
        ObjectiveSubmission submission = new ObjectiveSubmission();
        submission.setUserId(userId);
        submission.setQuestionId(question.getId());
        submission.setAnswer(demo.answer());
        submission.setCorrect(result.isCorrect() ? 1 : 0);
        submission.setScore(result.getScore());
        submissionMapper.insert(submission);
        return 1;
    }

    private int ensureQuestion(Seed seed, Long authorId) {
        if (questionMapper.findByContent(seed.content()) != null) {
            return 0;
        }
        ObjectiveQuestion question = new ObjectiveQuestion();
        question.setType(seed.type());
        question.setContent(seed.content());
        question.setOptions(writeOptions(seed.options()));
        question.setAnswer(seed.answer());
        question.setMatchType(seed.matchType());
        question.setScore(seed.score());
        question.setCreatedBy(authorId);
        question.setStatus(1); // 示例题一律公开
        questionMapper.insert(question);
        return 1;
    }

    private String writeOptions(List<String> options) {
        if (options == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(options);
        } catch (Exception e) {
            log.warn("示例题选项序列化失败: {}", e.getMessage());
            return null;
        }
    }

    /** 题干为查重键，务必保持稳定；改动题干等于新增一道题。 */
    private record Seed(
            String type,
            String content,
            List<String> options,
            String answer,
            String matchType,
            int score) {}

    private static final List<Seed> SEEDS =
            List.of(
                    new Seed(
                            "SINGLE",
                            "在 OSI 七层参考模型中，负责为两台主机的进程之间提供**端到端可靠传输**的是哪一层？",
                            List.of("A. 网络层", "B. 数据链路层", "C. 传输层", "D. 会话层"),
                            "C",
                            "EXACT",
                            5),
                    new Seed(
                            "SINGLE",
                            "TCP 建立连接采用三次握手。下列哪项是**第二次握手**报文中置位的标志位？",
                            List.of("A. 仅 SYN", "B. SYN 和 ACK", "C. 仅 ACK", "D. FIN 和 ACK"),
                            "B",
                            "EXACT",
                            5),
                    new Seed(
                            "SINGLE",
                            "某主机 IP 为 `192.168.1.130`，子网掩码为 `255.255.255.192`，该主机所在子网的**广播地址**是？",
                            List.of(
                                    "A. 192.168.1.191",
                                    "B. 192.168.1.255",
                                    "C. 192.168.1.129",
                                    "D. 192.168.1.128"),
                            "A",
                            "EXACT",
                            10),
                    new Seed(
                            "MULTIPLE",
                            "下列哪些协议工作在 **TCP/IP 应用层**？（多选，漏选得一半分）",
                            List.of("A. HTTP", "B. FTP", "C. TCP", "D. DNS"),
                            "A,B,D",
                            "EXACT",
                            10),
                    new Seed(
                            "MULTIPLE",
                            "与 UDP 相比，**TCP** 具备下列哪些特性？（多选，漏选得一半分）",
                            List.of("A. 面向连接的传输", "B. 提供可靠传输", "C. 支持一对多广播", "D. 提供流量控制"),
                            "A,B,D",
                            "EXACT",
                            10),
                    new Seed("FILL", "IPv4 地址由多少位二进制数组成？（只填数字）", null, "32", "EXACT", 5),
                    new Seed(
                            "FILL",
                            "传输层中提供**无连接**、不保证可靠交付服务的协议名称是什么？（填协议英文缩写，不区分大小写）",
                            null,
                            "UDP",
                            "IGNORE_CASE",
                            5),
                    new Seed(
                            "JUDGE",
                            "HTTP 协议是无状态协议，在同一连接中多次请求之间默认不保留状态。",
                            List.of("对", "错"),
                            "对",
                            "EXACT",
                            5),
                    new Seed(
                            "JUDGE",
                            "TCP 协议建立连接需要三次握手，断开连接也只需要三次挥手。",
                            List.of("对", "错"),
                            "错",
                            "EXACT",
                            5));

    /** 演示作答：content 与 {@link #SEEDS} 的题干逐字节一致，作为查重锚点。 */
    private record DemoSubmission(String content, String answer) {}

    private static final List<DemoSubmission> DEMO_SUBMISSIONS =
            List.of(
                    new DemoSubmission("在 OSI 七层参考模型中，负责为两台主机的进程之间提供**端到端可靠传输**的是哪一层？", "C"),
                    new DemoSubmission("TCP 建立连接采用三次握手。下列哪项是**第二次握手**报文中置位的标志位？", "A"),
                    new DemoSubmission("下列哪些协议工作在 **TCP/IP 应用层**？（多选，漏选得一半分）", "A,B"),
                    new DemoSubmission("IPv4 地址由多少位二进制数组成？（只填数字）", "32"));
}
