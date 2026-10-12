package com.kece.objective;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.kece.AbstractIntegrationTest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/** 客观题接口集成测试。 */
class ObjectiveQuestionIntegrationTest extends AbstractIntegrationTest {

    private String teacherToken() throws Exception {
        return loginToken("teacher", "teacher123");
    }

    private long createQuestion(String token, Map<String, Object> body) throws Exception {
        MvcResult result =
                mockMvc.perform(
                                post("/objective-questions")
                                        .header("Authorization", "Bearer " + token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(body)))
                        .andExpect(status().isOk())
                        .andReturn();
        assertThat(code(result)).isEqualTo(200);
        return data(result).path("id").asLong();
    }

    private String registerStudent() throws Exception {
        String username = "stu_" + UUID.randomUUID().toString().substring(0, 8);
        mockMvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                Map.of(
                                                        "username",
                                                        username,
                                                        "password",
                                                        "pass123456"))))
                .andExpect(status().isOk());
        return loginToken(username, "pass123456");
    }

    private JsonNode submit(long questionId, String token, String answer) throws Exception {
        MvcResult result =
                mockMvc.perform(
                                post("/objective-questions/" + questionId + "/submit")
                                        .header("Authorization", "Bearer " + token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                objectMapper.writeValueAsString(
                                                        Map.of("answer", answer))))
                        .andExpect(status().isOk())
                        .andReturn();
        assertThat(code(result)).isEqualTo(200);
        return data(result);
    }

    @Test
    void singleChoiceGrading_shouldSucceed() throws Exception {
        String token = teacherToken();
        long id =
                createQuestion(
                        token,
                        Map.of(
                                "type",
                                "SINGLE",
                                "content",
                                "1 + 1 = ?",
                                "options",
                                List.of("A. 1", "B. 2", "C. 3", "D. 4"),
                                "answer",
                                "B",
                                "score",
                                5));

        String student = registerStudent();
        JsonNode correct = submit(id, student, "B");
        assertThat(correct.path("correct").asBoolean()).isTrue();
        assertThat(correct.path("score").asInt()).isEqualTo(5);

        JsonNode wrong = submit(id, student, "A");
        assertThat(wrong.path("correct").asBoolean()).isFalse();
        assertThat(wrong.path("score").asInt()).isEqualTo(0);
    }

    @Test
    void multipleChoiceMissingSelection_shouldHalfScore() throws Exception {
        String token = teacherToken();
        long id =
                createQuestion(
                        token,
                        Map.of(
                                "type",
                                "MULTIPLE",
                                "content",
                                "以下哪些是编程语言？",
                                "options",
                                List.of("A. Java", "B. Python", "C. 汉语", "D. 英语"),
                                "answer",
                                "A,B",
                                "score",
                                5));

        String student = registerStudent();
        JsonNode full = submit(id, student, "A,B");
        assertThat(full.path("correct").asBoolean()).isTrue();
        assertThat(full.path("score").asInt()).isEqualTo(5);

        // 漏选 B，应得一半分（向上取整 3 分）
        JsonNode half = submit(id, student, "A");
        assertThat(half.path("correct").asBoolean()).isFalse();
        assertThat(half.path("score").asInt()).isEqualTo(3);

        // 含错误选项不得分
        JsonNode wrong = submit(id, student, "A,C");
        assertThat(wrong.path("score").asInt()).isEqualTo(0);
    }

    @Test
    void fillBlankIgnoreCase_shouldSucceed() throws Exception {
        String token = teacherToken();
        long id =
                createQuestion(
                        token,
                        Map.of(
                                "type",
                                "FILL",
                                "content",
                                "Java 的关键字 ____ 表示继承。",
                                "answer",
                                "extends",
                                "matchType",
                                "IGNORE_CASE",
                                "score",
                                5));

        String student = registerStudent();
        JsonNode correct = submit(id, student, "Extends");
        assertThat(correct.path("correct").asBoolean()).isTrue();
        assertThat(correct.path("score").asInt()).isEqualTo(5);
    }

    @Test
    void detailShouldHideAnswerForRegularUser() throws Exception {
        String token = teacherToken();
        long id =
                createQuestion(
                        token,
                        Map.of(
                                "type",
                                "SINGLE",
                                "content",
                                "2 + 2 = ?",
                                "options",
                                List.of("A. 3", "B. 4"),
                                "answer",
                                "B",
                                "score",
                                5));

        // 游客/普通用户看不到答案
        MvcResult guestDetail = mockMvc.perform(get("/objective-questions/" + id)).andReturn();
        assertThat(data(guestDetail).path("answer").isNull()).isTrue();

        // 出题人可见答案
        MvcResult teacherDetail =
                mockMvc.perform(
                                get("/objective-questions/" + id)
                                        .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn();
        assertThat(data(teacherDetail).path("answer").asText()).isEqualTo("B");
    }
}
