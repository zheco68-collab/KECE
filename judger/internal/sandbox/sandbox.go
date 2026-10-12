// Package sandbox 基于 Docker CLI 提供受限的编译与运行执行环境。
package sandbox

import (
	"bytes"
	"context"
	"errors"
	"fmt"
	"os/exec"
	"time"
)

const (
	backstopBuffer = 30 * time.Second
	maxOutputBytes = 64 << 10
	maxStderrBytes = 8 << 10
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

	start := time.Now()
	args := r.dockerArgs(memoryMb, ws, r.imageFor(lang), shell)
	stdout, stderr, code, err := runDocker(timeoutCtx, args)
	res.Elapsed = time.Since(start)
	res.Stdout = truncate(stdout, maxOutputBytes)
	res.Stderr = truncate(stderr, maxStderrBytes)
	res.ExitCode = code
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

func runCommand(lang, sec string) string {
	switch lang {
	case "cpp":
		return fmt.Sprintf("timeout -s KILL %s ./main < /work/input.txt", sec)
	case "java":
		return fmt.Sprintf("timeout -s KILL %s java -cp /work Main < /work/input.txt", sec)
	case "python":
		return fmt.Sprintf("timeout -s KILL %s python3 /work/main.py < /work/input.txt", sec)
	default:
		return "exit 1"
	}
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
