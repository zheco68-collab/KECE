package sandbox

import (
	"os"
	"path/filepath"
	"strings"
	"testing"
)

// TestReadMemProbe 覆盖探测文件的解析：只有能读出正数才当作测到，其余一律 nil（不伪造成 0）。
func TestReadMemProbe(t *testing.T) {
	dir := t.TempDir()
	tests := []struct {
		name    string
		content string
		wantKb  int // -1 表示期望 nil
	}{
		{"正常数值", "70254592\n", 68608},
		{"无换行", "20480", 20},
		{"零", "0", -1},
		{"空文件", "", -1},
		{"非数字", "N/A", -1},
		{"负数", "-1024", -1},
		{"不足 1KB", "512", -1},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			path := filepath.Join(dir, strings.ReplaceAll(tt.name, " ", "_")+".mem")
			if err := os.WriteFile(path, []byte(tt.content), 0o644); err != nil {
				t.Fatal(err)
			}
			got := readMemProbe(path)
			if tt.wantKb < 0 {
				if got != nil {
					t.Fatalf("readMemProbe(%q) = %d, 期望 nil", tt.content, *got)
				}
				return
			}
			if got == nil {
				t.Fatalf("readMemProbe(%q) = nil, 期望 %d", tt.content, tt.wantKb)
			}
			if *got != tt.wantKb {
				t.Fatalf("readMemProbe(%q) = %d, 期望 %d", tt.content, *got, tt.wantKb)
			}
		})
	}

	t.Run("文件缺失", func(t *testing.T) {
		if got := readMemProbe(filepath.Join(dir, "不存在.mem")); got != nil {
			t.Fatalf("文件缺失时应返回 nil，得到 %d", *got)
		}
	})
}

// TestRunCommandWrapsMemoryProbe 守住包装命令的两处要点：程序退出码必须原样透出
// （否则 TLE/MLE/RE 会被误判），且探测写在程序之后。
func TestRunCommandWrapsMemoryProbe(t *testing.T) {
	for _, lang := range []string{"cpp", "java", "python"} {
		cmd := runCommand(lang, "1.000")
		if !strings.Contains(cmd, "code=$?") || !strings.HasSuffix(cmd, "exit $code") {
			t.Errorf("%s: 未保留程序退出码: %s", lang, cmd)
		}
		if !strings.Contains(cmd, memProbePath) {
			t.Errorf("%s: 未写回峰值内存: %s", lang, cmd)
		}
		if strings.Index(cmd, memProbePath) < strings.Index(cmd, "timeout") {
			t.Errorf("%s: 探测应先运行程序再读取: %s", lang, cmd)
		}
	}
	if got := runCommand("brainfuck", "1.000"); got != "exit 1" {
		t.Errorf("未知语言应直接失败，得到 %q", got)
	}
}
