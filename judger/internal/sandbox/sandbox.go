// Package sandbox 基于 Docker CLI 提供受限的编译与运行执行环境。
package sandbox

import (
	"bytes"
	"context"
	"errors"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"strconv"
	"strings"
	"time"
)

const (
	backstopBuffer = 30 * time.Second
	maxOutputBytes = 64 << 10
	maxStderrBytes = 8 << 10
	// 峰值内存探测文件：容器在工作目录（挂载为 /work）内写回，宿主机按同名路径读取。
	memProbeName = ".memory"
	memProbePath = "/work/" + memProbeName
)

// Runner 通过宿主机 Docker daemon 执行沙箱容器。
type Runner struct {
	Workspace       string
	CompileTimeout  time.Duration
	CompileMemoryMb int
	ImageCPP        string
	ImageJava       string
	ImagePython     string
}

// RunResult 为单次运行的原始结果。
type RunResult struct {
	Stdout   string
	Stderr   string
	ExitCode int
	Elapsed  time.Duration
	// MemoryKb 为本次运行的峰值内存（含运行时基线）；宿主机/内核不支持探测时为 nil。
	MemoryKb *int
}

// Compile 编译源码；Python 等解释型语言直接返回成功。
// stderr 为编译日志，ok 表示编译是否成功。
func (r *Runner) Compile(ctx context.Context, lang, ws string) (stderr string, ok bool, err error) {
	shell, need := compileCommand(lang)
	if !need {
		return "", true, nil
	}
	timeoutCtx, cancel := context.WithTimeout(ctx, r.CompileTimeout)
	defer cancel()

	args := r.dockerArgs(r.CompileMemoryMb, ws, r.imageFor(lang), shell)
	_, stderr, code, err := runDocker(timeoutCtx, args)
	if err != nil {
		if ctx.Err() != nil {
			return "", false, ctx.Err()
		}
		if timeoutCtx.Err() != nil {
			return "compile timed out", false, nil
		}
		return "", false, fmt.Errorf("docker compile: %w", err)
	}
	return truncate(stderr, maxStderrBytes), code == 0, nil
}

// Run 以给定输入运行编译产物，返回标准输出、退出码与耗时。
func (r *Runner) Run(ctx context.Context, lang, ws, input string, timeLimitMs, memoryMb int) (RunResult, error) {
	res := RunResult{}
	sec := fmt.Sprintf("%.3f", float64(timeLimitMs)/1000.0)
	shell := runCommand(lang, sec)

	timeoutCtx, cancel := context.WithTimeout(ctx, time.Duration(timeLimitMs)*time.Millisecond+backstopBuffer)
	defer cancel()

	// 探测文件写在本次提交的工作目录里（该目录即容器内的 /work），须与挂载源同一路径。
	// 先清掉上一用例残留：本次读不到即代表「测不到」，不可退化成 0。
	memPath := filepath.Join(ws, memProbeName)
	_ = os.Remove(memPath)

	start := time.Now()
	args := r.dockerArgs(memoryMb, ws, r.imageFor(lang), shell)
	stdout, stderr, code, err := runDocker(timeoutCtx, args)
	res.Elapsed = time.Since(start)
	res.Stdout = truncate(stdout, maxOutputBytes)
	res.Stderr = truncate(stderr, maxStderrBytes)
	res.ExitCode = code
	res.MemoryKb = readMemProbe(memPath)
	if err != nil {
		switch {
		case ctx.Err() != nil:
			return res, ctx.Err()
		case timeoutCtx.Err() != nil:
			res.ExitCode = exitKill
			return res, nil
		default:
			return res, fmt.Errorf("docker run: %w", err)
		}
	}
	return res, nil
}

// dockerArgs 构造沙箱容器启动参数，落实网络隔离、Cgroups 资源限制与权限收敛。
func (r *Runner) dockerArgs(memoryMb int, ws, image, shellCmd string) []string {
	return []string{
		"run", "--rm",
		"--network", "none",
		"--memory", fmt.Sprintf("%dm", memoryMb),
		"--memory-swap", fmt.Sprintf("%dm", memoryMb),
		"--pids-limit", "64",
		"--cpus", "1",
		"--cap-drop", "ALL",
		"--security-opt", "no-new-privileges",
		"-v", ws + ":/work",
		"-w", "/work",
		image,
		"sh", "-c", shellCmd,
	}
}

func (r *Runner) imageFor(lang string) string {
	switch lang {
	case "cpp":
		return r.ImageCPP
	case "java":
		return r.ImageJava
	case "python":
		return r.ImagePython
	default:
		return ""
	}
}

func compileCommand(lang string) (string, bool) {
	switch lang {
	case "cpp":
		return "g++ -O2 -std=c++17 -o main main.cpp", true
	case "java":
		return "javac Main.java", true
	default:
		return "", false
	}
}

// runCommand 构造容器内的运行命令。
//
// 程序退出后顺带把容器 cgroup 的峰值内存（cgroup v2 memory.peak）写回工作目录，宿主机据此上报
// 逐用例内存。`; code=$?` 先存下程序的退出码、末尾 `exit $code` 原样退出，TLE/MLE/RE 的判定不受影响；
// 内核不支持 memory.peak 时写入失败（重定向到 /dev/null），读数留空而不是伪装成 0。
func runCommand(lang, sec string) string {
	var run string
	switch lang {
	case "cpp":
		run = fmt.Sprintf("timeout -s KILL %s ./main < /work/input.txt", sec)
	case "java":
		run = fmt.Sprintf("timeout -s KILL %s java -cp /work Main < /work/input.txt", sec)
	case "python":
		run = fmt.Sprintf("timeout -s KILL %s python3 /work/main.py < /work/input.txt", sec)
	default:
		return "exit 1"
	}
	return fmt.Sprintf("%s; code=$?; cat /sys/fs/cgroup/memory.peak > %s 2>/dev/null; exit $code", run, memProbePath)
}

// readMemProbe 读取容器写回的峰值内存（KB）。文件缺失（内核无 memory.peak）或内容非法时返回 nil。
func readMemProbe(path string) *int {
	data, err := os.ReadFile(path)
	if err != nil {
		return nil
	}
	bytes, err := strconv.ParseInt(strings.TrimSpace(string(data)), 10, 64)
	if err != nil || bytes <= 0 {
		return nil
	}
	kb := int(bytes / 1024)
	if kb <= 0 {
		return nil
	}
	return &kb
}

// runDocker 执行 docker 子命令并返回输出、退出码。
// 仅在 docker 命令本身失败（如 daemon 不可用）时返回错误；容器非零退出不算错误。
func runDocker(ctx context.Context, args []string) (stdout, stderr string, exitCode int, err error) {
	cmd := exec.CommandContext(ctx, "docker", args...)
	var outBuf, errBuf bytes.Buffer
	cmd.Stdout = &outBuf
	cmd.Stderr = &errBuf
	err = cmd.Run()
	stdout = outBuf.String()
	stderr = errBuf.String()
	if err != nil {
		var exitErr *exec.ExitError
		if errors.As(err, &exitErr) {
			return stdout, stderr, exitErr.ExitCode(), nil
		}
		return stdout, stderr, -1, err
	}
	return stdout, stderr, 0, nil
}

func truncate(s string, n int) string {
	if len(s) <= n {
		return s
	}
	return s[:n]
}
