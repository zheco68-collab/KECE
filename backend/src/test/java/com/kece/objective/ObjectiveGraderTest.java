package com.kece.objective;

import static org.assertj.core.api.Assertions.assertThat;

import com.kece.objective.dto.AnswerResultVO;
import com.kece.objective.entity.ObjectiveQuestion;
import com.kece.objective.service.ObjectiveGrader;
import org.junit.jupiter.api.Test;

/** 客观题判分器单元测试（纯逻辑）。 */
class ObjectiveGraderTest {

    private final ObjectiveGrader grader = new ObjectiveGrader();

    private ObjectiveQuestion question(String type, String answer, String matchType, int score) {
        ObjectiveQuestion q = new ObjectiveQuestion();
        q.setType(type);
        q.setAnswer(answer);
        q.setMatchType(matchType);
        q.setScore(score);
        return q;
    }

    @Test
    void singleChoice_exactMatchIsFullScore() {
        AnswerResultVO result = grader.grade(question("SINGLE", "B", null, 5), "B");
        assertThat(result.isCorrect()).isTrue();
        assertThat(result.getScore()).isEqualTo(5);
    }

    @Test
    void multipleChoice_missingOneIsHalfScore() {
        AnswerResultVO result = grader.grade(question("MULTIPLE", "A,B,C", null, 5), "A,C");
        assertThat(result.isCorrect()).isFalse();
        assertThat(result.getScore()).isEqualTo(3);
    }

    @Test
    void multipleChoice_wrongOptionIsZero() {
        AnswerResultVO result = grader.grade(question("MULTIPLE", "A,B", null, 5), "A,D");
        assertThat(result.getScore()).isZero();
    }

    @Test
    void fill_exactMatch() {
        AnswerResultVO result = grader.grade(question("FILL", "java", "EXACT", 5), "java");
        assertThat(result.isCorrect()).isTrue();
    }

    @Test
    void fill_ignoreCaseMatch() {
        AnswerResultVO result =
                grader.grade(question("FILL", "extends", "IGNORE_CASE", 5), "Extends");
        assertThat(result.isCorrect()).isTrue();
    }

    @Test
    void fill_regexMatch() {
        AnswerResultVO result = grader.grade(question("FILL", "\\d{4}", "REGEX", 5), "2026");
        assertThat(result.isCorrect()).isTrue();
    }

    @Test
    void judge_correctAnswerIsFullScore() {
        AnswerResultVO result = grader.grade(question("JUDGE", "对", null, 5), "对");
        assertThat(result.isCorrect()).isTrue();
        assertThat(result.getScore()).isEqualTo(5);

        AnswerResultVO result2 = grader.grade(question("JUDGE", "错", null, 5), "错");
        assertThat(result2.isCorrect()).isTrue();
        assertThat(result2.getScore()).isEqualTo(5);
    }

    @Test
    void judge_wrongAnswerIsZero() {
        AnswerResultVO result = grader.grade(question("JUDGE", "对", null, 5), "错");
        assertThat(result.isCorrect()).isFalse();
        assertThat(result.getScore()).isZero();
    }

    @Test
    void judge_tolerantFormats() {
        AnswerResultVO result = grader.grade(question("JUDGE", "对", null, 5), "TRUE");
        assertThat(result.isCorrect()).isTrue();

        AnswerResultVO result2 = grader.grade(question("JUDGE", "错", null, 5), "F");
        assertThat(result2.isCorrect()).isTrue();
    }
}
