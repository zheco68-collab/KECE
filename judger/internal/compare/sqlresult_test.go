package compare

import "testing"

func rs(cols []Column, rows ...[]string) ResultSet {
	if rows == nil {
		rows = [][]string{}
	}
	return ResultSet{Columns: cols, Rows: rows}
}

var (
	colID   = Column{Name: "id", Type: "INT"}
	colName = Column{Name: "name", Type: "VARCHAR"}
	colAmt  = Column{Name: "amount", Type: "DECIMAL"}
)

func TestEqualResultSets(t *testing.T) {
	tests := []struct {
		name           string
		expected       ResultSet
		actual         ResultSet
		orderSensitive bool
		want           bool
	}{
		{
			name:     "完全一致",
			expected: rs([]Column{colID, colName}, []string{"1", "Alice"}, []string{"2", "Bob"}),
			actual:   rs([]Column{colID, colName}, []string{"1", "Alice"}, []string{"2", "Bob"}),
			want:     true,
		},
		{
			name:     "列数不一致",
			expected: rs([]Column{colID, colName}, []string{"1", "Alice"}),
			actual:   rs([]Column{colID}, []string{"1"}),
			want:     false,
		},
		{
			name:     "列名不一致",
			expected: rs([]Column{colID}, []string{"1"}),
			actual:   rs([]Column{{Name: "uid", Type: "INT"}}, []string{"1"}),
			want:     false,
		},
		{
			name:     "列型别名等价 INTEGER 与 INT",
			expected: rs([]Column{{Name: "id", Type: "INT"}}, []string{"1"}),
			actual:   rs([]Column{{Name: "id", Type: "INTEGER"}}, []string{"1"}),
			want:     true,
		},
		{
			name:     "列型带长度修饰等价",
			expected: rs([]Column{{Name: "name", Type: "VARCHAR"}}, []string{"a"}),
			actual:   rs([]Column{{Name: "name", Type: "VARCHAR(32)"}}, []string{"a"}),
			want:     true,
		},
		{
			name:     "行数不一致",
			expected: rs([]Column{colID}, []string{"1"}, []string{"2"}),
			actual:   rs([]Column{colID}, []string{"1"}),
			want:     false,
		},
		{
			name:     "浮点相对容差内视为相等",
			expected: rs([]Column{colAmt}, []string{"11000.00"}),
			actual:   rs([]Column{colAmt}, []string{"11000.000000001"}),
			want:     true,
		},
		{
			name:     "浮点超出容差为不等",
			expected: rs([]Column{colAmt}, []string{"11000.00"}),
			actual:   rs([]Column{colAmt}, []string{"11000.01"}),
			want:     false,
		},
		{
			name:           "行序敏感时顺序不同为不等",
			expected:       rs([]Column{colID}, []string{"1"}, []string{"2"}),
			actual:         rs([]Column{colID}, []string{"2"}, []string{"1"}),
			orderSensitive: true,
			want:           false,
		},
		{
			name:           "行序不敏感时顺序不同仍相等",
			expected:       rs([]Column{colID}, []string{"1"}, []string{"2"}),
			actual:         rs([]Column{colID}, []string{"2"}, []string{"1"}),
			orderSensitive: false,
			want:           true,
		},
		{
			name:     "NULL 与字面量 NULL 视为相同",
			expected: rs([]Column{colName}, []string{"NULL"}),
			actual:   rs([]Column{colName}, []string{"NULL"}),
			want:     true,
		},
		{
			name:     "空结果集相等",
			expected: rs([]Column{colID}),
			actual:   rs([]Column{colID}),
			want:     true,
		},
		{
			name:     "文本首尾空白忽略",
			expected: rs([]Column{colName}, []string{"Alice"}),
			actual:   rs([]Column{colName}, []string{" Alice "}),
			want:     true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, diff := EqualResultSets(tt.expected, tt.actual, tt.orderSensitive)
			if got != tt.want {
				t.Fatalf("EqualResultSets() = %v, want %v (diff=%q)", got, tt.want, diff)
			}
			if got && diff != "" {
				t.Fatalf("一致的比对不应返回差异描述，得到 %q", diff)
			}
			if !got && diff == "" {
				t.Fatal("不一致的比对应返回差异描述")
			}
		})
	}
}

func TestCellStringNormalizesNil(t *testing.T) {
	if got := cellString(nil); got != "NULL" {
		t.Fatalf("cellString(nil) = %q, want \"NULL\"", got)
	}
	if got := cellString([]byte("abc")); got != "abc" {
		t.Fatalf("cellString([]byte) = %q, want \"abc\"", got)
	}
}
