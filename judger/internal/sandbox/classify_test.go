package sandbox

import "testing"

func TestClassifyExit(t *testing.T) {
	tests := []struct {
		code int
		want string
	}{
		{0, ""},
		{124, StatusTimeLimitExceeded},
		{137, StatusMemoryLimitExceeded},
		{-1, StatusTimeLimitExceeded},
		{1, StatusRuntimeError},
		{139, StatusRuntimeError},
	}
	for _, tt := range tests {
		if got := ClassifyExit(tt.code); got != tt.want {
			t.Fatalf("ClassifyExit(%d) = %q, want %q", tt.code, got, tt.want)
		}
	}
}
