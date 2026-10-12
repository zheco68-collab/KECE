package compare

import "testing"

func TestEqual(t *testing.T) {
	tests := []struct {
		name     string
		actual   string
		expected string
		want     bool
	}{
		{"exact", "1 2 3\n", "1 2 3\n", true},
		{"trailing spaces", "1 2 3  \n", "1 2 3\n", true},
		{"crlf", "1\r\n2\r\n", "1\n2\n", true},
		{"trailing blank lines", "1\n2\n\n\n", "1\n2\n", true},
		{"leading blank lines", "\n\n1\n2", "1\n2", true},
		{"tab trailing", "42\t\n", "42\n", true},
		{"mismatch value", "1 2 3\n", "1 2 4\n", false},
		{"mismatch length", "1\n2\n", "1\n", false},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := Equal(tt.actual, tt.expected); got != tt.want {
				t.Fatalf("Equal(%q, %q) = %v, want %v", tt.actual, tt.expected, got, tt.want)
			}
		})
	}
}
