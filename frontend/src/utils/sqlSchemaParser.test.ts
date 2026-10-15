import { describe, it, expect } from 'vitest'
import { parseSchemaSql, parseSeedSql, parseExpectedOutput } from './sqlSchemaParser'

describe('sqlSchemaParser', () => {
  it('parses single table schema with inline primary key', () => {
    const ddl = 'CREATE TABLE emp (id INT PRIMARY KEY, name VARCHAR(32), dept VARCHAR(16), salary DECIMAL(10,2));'
    const tables = parseSchemaSql(ddl)
    expect(tables).toHaveLength(1)
    expect(tables[0].tableName).toBe('emp')
    expect(tables[0].columns).toHaveLength(4)
    expect(tables[0].columns[0]).toEqual({
      name: 'id',
      type: 'INT',
      isPrimaryKey: true,
      isNullable: false,
      extra: 'PRIMARY KEY',
      comment: undefined,
    })
    expect(tables[0].columns[3].type).toBe('DECIMAL(10,2)')
  })

  it('parses table level primary key and not null', () => {
    const ddl = `
      CREATE TABLE \`users\` (
        \`user_id\` BIGINT AUTO_INCREMENT,
        \`username\` VARCHAR(50) NOT NULL,
        \`email\` VARCHAR(100) DEFAULT 'none',
        PRIMARY KEY (\`user_id\`)
      );
    `
    const tables = parseSchemaSql(ddl)
    expect(tables).toHaveLength(1)
    expect(tables[0].tableName).toBe('users')
    expect(tables[0].columns[0].isPrimaryKey).toBe(true)
    expect(tables[0].columns[1].isNullable).toBe(false)
  })

  it('parses seed insert data with implicit schema columns', () => {
    const ddl = 'CREATE TABLE emp (id INT PRIMARY KEY, name VARCHAR(32), dept VARCHAR(16), salary DECIMAL(10,2));'
    const seed = "INSERT INTO emp VALUES (1,'Alice','RD',10000.00),(2,'Bob','RD',12000.00),(3,'Cara','HR',9000.00);"
    const schemas = parseSchemaSql(ddl)
    const seeds = parseSeedSql(seed, schemas)

    expect(seeds).toHaveLength(1)
    expect(seeds[0].tableName).toBe('emp')
    expect(seeds[0].columns).toEqual(['id', 'name', 'dept', 'salary'])
    expect(seeds[0].rows).toHaveLength(3)
    expect(seeds[0].rows[0]).toEqual(['1', 'Alice', 'RD', '10000.00'])
    expect(seeds[0].rows[1]).toEqual(['2', 'Bob', 'RD', '12000.00'])
  })

  it('parses expected output in JSON and markdown table formats', () => {
    const jsonRaw = '{"columns":[{"name":"id","type":"INT"},{"name":"name","type":"VARCHAR"}],"rows":[["1","Alice"],["2","Bob"]]}'
    const res1 = parseExpectedOutput(jsonRaw)
    expect(res1).not.toBeNull()
    expect(res1?.columns).toHaveLength(2)
    expect(res1?.columns[0].name).toBe('id')
    expect(res1?.rows).toHaveLength(2)
    expect(res1?.rows[0]).toEqual(['1', 'Alice'])

    const mdRaw = `
      | dept | avg_salary |
      | :--- | :--- |
      | HR | 9000.00 |
      | RD | 11000.00 |
    `
    const res2 = parseExpectedOutput(mdRaw)
    expect(res2).not.toBeNull()
    expect(res2?.columns.map((c) => c.name)).toEqual(['dept', 'avg_salary'])
    expect(res2?.rows).toHaveLength(2)
    expect(res2?.rows[0]).toEqual(['HR', '9000.00'])
  })

  it('parses multiple tables and merges multi-statement seed inserts', () => {
    const multiDdl = `
      -- 部门表
      CREATE TABLE dept (
        id INT PRIMARY KEY,
        dept_name VARCHAR(32) NOT NULL COMMENT '部门名称'
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

      -- 员工表带外键约束
      CREATE TABLE emp (
        id INT PRIMARY KEY,
        name VARCHAR(32) NOT NULL,
        dept_id INT,
        salary DECIMAL(10,2) DEFAULT 0.00,
        CONSTRAINT fk_dept FOREIGN KEY (dept_id) REFERENCES dept(id)
      ) ENGINE=InnoDB;
    `
    const tables = parseSchemaSql(multiDdl)
    expect(tables).toHaveLength(2)
    expect(tables[0].tableName).toBe('dept')
    expect(tables[0].columns).toHaveLength(2)
    expect(tables[1].tableName).toBe('emp')
    expect(tables[1].columns).toHaveLength(4)

    const multiSeed = `
      -- 初始化部门
      INSERT INTO dept VALUES (1, '研发部'), (2, '运营部');
      -- 分两次插入员工
      INSERT INTO emp VALUES (1, '张三', 1, 12000.00);
      INSERT INTO emp VALUES (2, '李四', 1, 15000.00), (3, '王五', 2, 9000.00);
    `
    const seeds = parseSeedSql(multiSeed, tables)
    expect(seeds).toHaveLength(2)
    expect(seeds[0].tableName).toBe('dept')
    expect(seeds[0].rows).toHaveLength(2)
    expect(seeds[1].tableName).toBe('emp')
    expect(seeds[1].columns).toEqual(['id', 'name', 'dept_id', 'salary'])
    expect(seeds[1].rows).toHaveLength(3) // 自动合并为 3 条数据
  })
})

