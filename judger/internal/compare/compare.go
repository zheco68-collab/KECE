// Package compare 提供判题输出的标准化与比较逻辑。
package compare

import "strings"

// Equal 判断程序输出与期望输出是否一致。
// 忽略每行行尾空白以及首尾空行，容忍 \r\n 与 \n 差异。
func Equal(actual, expected string) bool {
	return normalize(actual) == normalize(expected)
}

// normalize 将输出按行拆分、去除每行行尾空白，并去除首尾空行后重新拼接。
func normalize(s string) string {
	lines := strings.Split(strings.ReplaceAll(s, "\r\n", "\n"), "\n")
	for i, line := range lines {
		lines[i] = strings.TrimRight(line, " \t")
	}
	start := 0
	for start < len(lines) && lines[start] == "" {
		start++
	}
	end := len(lines)
	for end > start && lines[end-1] == "" {
		end--
	}
	return strings.Join(lines[start:end], "\n")
}
