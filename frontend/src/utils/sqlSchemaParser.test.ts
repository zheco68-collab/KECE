import { describe, it, expect } from 'vitest'
import { parseSchemaSql, parseSeedSql } from './sqlSchemaParser'

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
})
