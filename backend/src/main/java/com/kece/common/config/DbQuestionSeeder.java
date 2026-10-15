package com.kece.common.config;

import com.kece.database.entity.DbQuestion;
import com.kece.database.entity.DbTestCase;
import com.kece.database.mapper.DbQuestionMapper;
import com.kece.database.mapper.DbTestCaseMapper;
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
 * 数据库题示例数据播种：首次启动时写入一组基于 emp 经典 schema 的 SELECT/DML 题（含测试点）， 便于直接试用与验收。
 *
 * <p>受 {@code kece.demo-data.enabled} 控制（默认关、仅 dev profile 开）；在 {@link DataInitializer} 播种完 teacher
 * 账号后执行（{@code @Order(4)}）。
 *
 * <p>幂等策略：题目按标题精确查重，重复启动不产生重复数据；被出题人改动或删除的题不会被重新写回。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DbQuestionSeeder {

    private final DbQuestionMapper questionMapper;
    private final DbTestCaseMapper testCaseMapper;
    private final UserMapper userMapper;

    @Value("${kece.demo-data.enabled:false}")
    private boolean demoDataEnabled;

    @Order(4)
    @Bean
    public ApplicationRunner seedDbQuestions() {
        return args -> {
            if (!demoDataEnabled) {
                log.info("演示数据开关已关闭（kece.demo-data.enabled=false），跳过数据库题示例播种");
                return;
            }
            User teacher = userMapper.findByUsername("teacher");
            if (teacher == null) {
                log.warn("未找到 teacher 账号，跳过数据库题示例数据播种");
                return;
            }
            int created = 0;
            for (Seed seed : SEEDS) {
                created += ensureQuestion(seed, teacher.getId());
            }
            if (created > 0) {
                log.info("已播种 {} 道数据库题示例", created);
            }
        };
    }

    private int ensureQuestion(Seed seed, Long authorId) {
        if (questionMapper.findByTitle(seed.title()) != null) {
            return 0;
        }
        DbQuestion question = new DbQuestion();
        question.setTitle(seed.title());
        question.setDescription(seed.description());
        question.setDialect("MYSQL");
        question.setType(seed.type());
        question.setSchemaSql(seed.schemaSql());
        question.setReferenceSql(seed.referenceSql());
        question.setVerifySql(seed.verifySql());
        question.setOrderSensitive(seed.orderSensitive() ? 1 : 0);
        question.setStatus(1); // 示例题一律公开
        question.setCreatedBy(authorId);
        questionMapper.insert(question);
        int order = 0;
        for (Case c : seed.cases()) {
            DbTestCase testCase = new DbTestCase();
            testCase.setQuestionId(question.getId());
            testCase.setCaseOrder(order++);
            testCase.setSeedSql(c.seedSql());
            testCase.setExpectedOutput(c.expectedOutput());
            testCase.setIsSample(c.sample() ? 1 : 0);
            testCaseMapper.insert(testCase);
        }
        return 1;
    }

    /** 标题为查重键，务必保持稳定；改动标题等于新增一道题。 */
    private record Seed(
            String title,
            String description,
            String type,
            String schemaSql,
            String referenceSql,
            String verifySql,
            boolean orderSensitive,
            List<Case> cases) {}

    private record Case(String seedSql, String expectedOutput, boolean sample) {
        Case(String seedSql, boolean sample) {
            this(seedSql, null, sample);
        }
    }

    /** emp 经典 schema：三类题共用，测试点只带数据。 */
    private static final String EMP_SCHEMA =
            "CREATE TABLE emp (id INT PRIMARY KEY, name VARCHAR(32), dept VARCHAR(16), salary DECIMAL(10,2));";

    private static final String EMP_SEED_1 =
            "INSERT INTO emp VALUES (1,'Alice','RD',10000.00),(2,'Bob','RD',12000.00),(3,'Cara','HR',9000.00);";
    private static final String EMP_SEED_2 =
            "INSERT INTO emp VALUES (1,'Dan','RD',8000.00),(2,'Eve','HR',9500.00),(3,'Finn','HR',11000.00),(4,'Gail','OPS',7000.00);";

    private static final List<Seed> SEEDS =
            List.of(
                    new Seed(
                            "查询研发部员工",
                            """
                            ## 题目描述

                            员工表 `emp` 结构如下：

                            ```sql
                            CREATE TABLE emp (id INT PRIMARY KEY, name VARCHAR(32), dept VARCHAR(16), salary DECIMAL(10,2));
                            ```

                            编写 SQL 查询**研发部（dept = 'RD'）**所有员工的 `id` 与 `name`，列顺序为 `id, name`。

                            ## 输出要求

                            结果集与标准答案按行无序比对。
                            """,
                            "SELECT",
                            EMP_SCHEMA,
                            "SELECT id, name FROM emp WHERE dept = 'RD'",
                            null,
                            false,
                            List.of(
                                    new Case(
                                            EMP_SEED_1,
                                            "{\"columns\":[{\"name\":\"id\",\"type\":\"INT\"},{\"name\":\"name\",\"type\":\"VARCHAR\"}],\"rows\":[[\"1\",\"Alice\"],[\"2\",\"Bob\"]]}",
                                            true),
                                    new Case(EMP_SEED_2, false))),
                    new Seed(
                            "统计各部门平均薪资",
                            """
                            ## 题目描述

                            员工表 `emp` 结构如下：

                            ```sql
                            CREATE TABLE emp (id INT PRIMARY KEY, name VARCHAR(32), dept VARCHAR(16), salary DECIMAL(10,2));
                            ```

                            编写 SQL 统计每个部门的平均薪资，输出两列：部门 `dept`、平均薪资 `avg_salary`（保留两位小数），
                            并按部门名**升序**排列。

                            ## 输出要求

                            结果集按行有序比对（ORDER BY dept）。
                            """,
                            "SELECT",
                            EMP_SCHEMA,
                            "SELECT dept, ROUND(AVG(salary), 2) AS avg_salary FROM emp GROUP BY dept ORDER BY dept",
                            null,
                            true,
                            List.of(
                                    new Case(
                                            EMP_SEED_1,
                                            "{\"columns\":[{\"name\":\"dept\",\"type\":\"VARCHAR\"},{\"name\":\"avg_salary\",\"type\":\"DECIMAL\"}],\"rows\":[[\"HR\",\"9000.00\"],[\"RD\",\"11000.00\"]]}",
                                            true),
                                    new Case(EMP_SEED_2, false))),
                    new Seed(
                            "给研发部员工涨薪 10%",
                            """
                            ## 题目描述

                            员工表 `emp` 结构如下：

                            ```sql
                            CREATE TABLE emp (id INT PRIMARY KEY, name VARCHAR(32), dept VARCHAR(16), salary DECIMAL(10,2));
                            ```

                            编写一条 UPDATE 语句，将**研发部（dept = 'RD'）**所有员工的薪资上调 10%。

                            ## 判分方式

                            执行你的语句后，系统用校验查询比对整表数据。
                            """,
                            "DML",
                            EMP_SCHEMA,
                            "UPDATE emp SET salary = salary * 1.1 WHERE dept = 'RD'",
                            "SELECT id, name, dept, salary FROM emp ORDER BY id",
                            true,
                            List.of(
                                    new Case(
                                            EMP_SEED_1,
                                            "{\"columns\":[{\"name\":\"id\",\"type\":\"INT\"},{\"name\":\"name\",\"type\":\"VARCHAR\"},{\"name\":\"dept\",\"type\":\"VARCHAR\"},{\"name\":\"salary\",\"type\":\"DECIMAL\"}],\"rows\":[[\"1\",\"Alice\",\"RD\",\"11000.00\"],[\"2\",\"Bob\",\"RD\",\"13200.00\"],[\"3\",\"Cara\",\"HR\",\"9000.00\"]]}",
                                            true),
                                    new Case(EMP_SEED_2, false))));
}
