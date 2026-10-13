package com.kece.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kece.objective.entity.ObjectiveQuestion;
import com.kece.objective.mapper.ObjectiveQuestionMapper;
import com.kece.user.entity.User;
import com.kece.user.mapper.UserMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 客观题示例数据播种：首次启动时写入一组计算机网络例题，便于直接试用与验收。
 *
 * <p>幂等策略：按题干精确查重，已存在则跳过，因此重复启动不会产生重复题目， 后续被出题人改动或删除也不会被重新写回（只在缺失时补种）。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ObjectiveQuestionSeeder {

    private final ObjectiveQuestionMapper questionMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    @Bean
    public ApplicationRunner seedObjectiveQuestions() {
        return args -> {
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
        };
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
                            5));
}
