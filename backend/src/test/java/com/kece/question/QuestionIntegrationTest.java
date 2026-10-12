package com.kece.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.kece.AbstractIntegrationTest;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/** 算法题库接口集成测试。 */
class QuestionIntegrationTest extends AbstractIntegrationTest {

    private String teacherToken() throws Exception {
        return loginToken("teacher", "teacher123");
    }

    private long createQuestion(String token, String title) throws Exception {
        MvcResult result =
                mockMvc.perform(
                                post("/questions")
                                        .header("Authorization", "Bearer " + token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                objectMapper.writeValueAsString(
                                                        Map.of(
                                                                "title",
                                                                title,
                                                                "description",
                                                                "## 题目描述",
                                                                "difficulty",
                                                                "EASY"))))
                        .andExpect(status().isOk())
                        .andReturn();
        assertThat(code(result)).isEqualTo(200);
        return data(result).path("id").asLong();
    }

    @Test
    void teacherCreateListDetailDelete_shouldSucceed() throws Exception {
        String token = teacherToken();
        String title = "两数之和_" + UUID.randomUUID().toString().substring(0, 8);
        long id = createQuestion(token, title);

        // 列表可检索到
        MvcResult list = mockMvc.perform(get("/questions")).andExpect(status().isOk()).andReturn();
        assertThat(code(list)).isEqualTo(200);
        assertThat(data(list).path("total").asLong()).isGreaterThanOrEqualTo(1);

        // 详情
        MvcResult detail =
                mockMvc.perform(get("/questions/" + id)).andExpect(status().isOk()).andReturn();
        assertThat(data(detail).path("title").asText()).isEqualTo(title);
        assertThat(data(detail).path("difficulty").asText()).isEqualTo("EASY");

        // 新增样例测试用例
        MvcResult addCase =
                mockMvc.perform(
                                post("/questions/" + id + "/test-cases")
                                        .header("Authorization", "Bearer " + token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                objectMapper.writeValueAsString(
                                                        Map.of(
                                                                "input",
                                                                "1 2",
                                                                "expectedOutput",
                                                                "3",
                                                                "isSample",
                                                                1))))
                        .andExpect(status().isOk())
                        .andReturn();
        assertThat(code(addCase)).isEqualTo(200);

        // 详情应携带样例用例
        MvcResult detailWithCase =
                mockMvc.perform(get("/questions/" + id)).andExpect(status().isOk()).andReturn();
        JsonNode samples = data(detailWithCase).path("sampleTestCases");
        assertThat(samples.size()).isEqualTo(1);
        assertThat(samples.get(0).path("expectedOutput").asText()).isEqualTo("3");

        // 删除
        MvcResult deleteResult =
                mockMvc.perform(
                                delete("/questions/" + id)
                                        .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn();
        assertThat(code(deleteResult)).isEqualTo(200);
    }

    @Test
    void guestCreateQuestion_shouldReturnForbidden() throws Exception {
        MvcResult result =
                mockMvc.perform(
                                post("/questions")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                objectMapper.writeValueAsString(
                                                        Map.of(
                                                                "title",
                                                                "t",
                                                                "description",
                                                                "d",
                                                                "difficulty",
                                                                "EASY"))))
                        .andExpect(status().isOk())
                        .andReturn();
        assertThat(code(result)).isEqualTo(4002);
    }

    @Test
    void regularUserCreateQuestion_shouldReturnForbidden() throws Exception {
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
        String token = loginToken(username, "pass123456");

        MvcResult result =
                mockMvc.perform(
                                post("/questions")
                                        .header("Authorization", "Bearer " + token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                objectMapper.writeValueAsString(
                                                        Map.of(
                                                                "title",
                                                                "t",
                                                                "description",
                                                                "d",
                                                                "difficulty",
                                                                "EASY"))))
                        .andExpect(status().isOk())
                        .andReturn();
        assertThat(code(result)).isEqualTo(4003);
    }
}
