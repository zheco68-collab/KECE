package compare

import (
	"database/sql"
	"fmt"
	"math"
	"sort"
	"strconv"
	"strings"
)

// Column 描述结果集的一列。
type Column struct {
	Name string `json:"name"`
	Type string `json:"type"`
}

// ResultSet 为 SQL 查询结果集的标准表示，用于结构与数据比对。
type ResultSet struct {
	Columns []Column   `json:"columns"`
	Rows    [][]string `json:"rows"`
}

// Capture 从 *sql.Rows 读取全部数据，构造标准结果集。
// NULL 统一序列化为字符串 "NULL"（v1 已知限制：无法区分字面量 "NULL"）。
func Capture(rows *sql.Rows) (ResultSet, error) {
	names, err := rows.Columns()
	if err != nil {
		return ResultSet{}, err
	}
	types, err := rows.ColumnTypes()
	if err != nil {
		return ResultSet{}, err
	}
	cols := make([]Column, 0, len(names))
	for i := range names {
		cols = append(cols, Column{Name: names[i], Type: types[i].DatabaseTypeName()})
	}
	result := ResultSet{Columns: cols, Rows: [][]string{}}
	for rows.Next() {
		raw := make([]any, len(cols))
		ptrs := make([]any, len(cols))
		for i := range raw {
			ptrs[i] = &raw[i]
		}
		if err := rows.Scan(ptrs...); err != nil {
			return ResultSet{}, err
		}
		row := make([]string, len(cols))
		for i, v := range raw {
			row[i] = cellString(v)
		}
		result.Rows = append(result.Rows, row)
	}
	if err := rows.Err(); err != nil {
		return ResultSet{}, err
	}
	return result, nil
}

// cellString 将 Scan 得到的值序列化为字符串。
func cellString(v any) string {
	switch t := v.(type) {
	case nil:
		return "NULL"
	case []byte:
		return string(t)
	case string:
		return t
	default:
		return fmt.Sprintf("%v", v)
	}
}

// EqualResultSets 比对期望与实际结果集：结构（列数/列名/列型）+ 行数 + 数据（数值宽容差）+ 行序。
// 返回是否一致及首个差异的可读描述（一致时 diff 为空）。
func EqualResultSets(expected, actual ResultSet, orderSensitive bool) (bool, string) {
	if len(expected.Columns) != len(actual.Columns) {
		return false, fmt.Sprintf("列数不一致：期望 %d 列，实际 %d 列", len(expected.Columns), len(actual.Columns))
	}
	for i := range expected.Columns {
		if expected.Columns[i].Name != actual.Columns[i].Name {
			return false, fmt.Sprintf("第 %d 列列名不一致：期望 %q，实际 %q", i+1, expected.Columns[i].Name, actual.Columns[i].Name)
		}
		if normalizeType(expected.Columns[i].Type) != normalizeType(actual.Columns[i].Type) {
			return false, fmt.Sprintf("第 %d 列类型不一致：期望 %s，实际 %s", i+1, expected.Columns[i].Type, actual.Columns[i].Type)
		}
	}
	if len(expected.Rows) != len(actual.Rows) {
		return false, fmt.Sprintf("行数不一致：期望 %d 行，实际 %d 行", len(expected.Rows), len(actual.Rows))
	}

	expRows := expected.Rows
	actRows := actual.Rows
	if !orderSensitive {
		expRows = sortedCopy(expRows)
		actRows = sortedCopy(actRows)
	}
	for r := range expRows {
		if diff := compareRow(expRows[r], actRows[r]); diff != "" {
			return false, fmt.Sprintf("第 %d 行：%s", r+1, diff)
		}
	}
	return true, ""
}

// compareRow 逐格比对一行，返回首个差异描述（无差异返回空串）。
func compareRow(expected, actual []string) string {
	for i := range expected {
		if i >= len(actual) {
			return fmt.Sprintf("第 %d 列缺失", i+1)
		}
		if !cellEqual(expected[i], actual[i]) {
			return fmt.Sprintf("第 %d 列：期望 %q，实际 %q", i+1, expected[i], actual[i])
		}
	}
	return ""
}

// cellEqual 单格比对：数值宽容差，其余去除首尾空白后严格相等。
func cellEqual(a, b string) bool {
	if a == b {
		return true
	}
	if fa, ok := parseFloat(a); ok {
		if fb, ok2 := parseFloat(b); ok2 {
			scale := math.Max(1, math.Abs(fb))
			return math.Abs(fa-fb) <= 1e-9*scale
		}
	}
	return strings.TrimSpace(a) == strings.TrimSpace(b)
}

func parseFloat(s string) (float64, bool) {
	f, err := strconv.ParseFloat(strings.TrimSpace(s), 64)
	return f, err == nil
}

// normalizeType 宽松归一化列型：大写、去长度/精度修饰，并将常见等价类型族归并。
func normalizeType(t string) string {
	upper := strings.ToUpper(strings.TrimSpace(t))
	if i := strings.IndexByte(upper, '('); i >= 0 {
		upper = upper[:i]
	}
	upper = strings.TrimSpace(strings.TrimSuffix(strings.TrimSuffix(upper, " ZEROFILL"), " UNSIGNED"))
	switch upper {
	case "INTEGER":
		return "INT"
	case "BOOL", "BOOLEAN":
		return "TINYINT"
	}
	return upper
}

func sortedCopy(rows [][]string) [][]string {
	out := make([][]string, len(rows))
	copy(out, rows)
	sort.Slice(out, func(i, j int) bool { return rowKey(out[i]) < rowKey(out[j]) })
	return out
}

func rowKey(row []string) string {
	return strings.Join(row, "\x00")
}
