package sandbox

// 判题状态常量，与后端 SubmissionStatus 保持一致。
const (
	StatusAccepted            = "ACCEPTED"
	StatusWrongAnswer         = "WRONG_ANSWER"
	StatusTimeLimitExceeded   = "TIME_LIMIT_EXCEEDED"
	StatusMemoryLimitExceeded = "MEMORY_LIMIT_EXCEEDED"
	StatusRuntimeError        = "RUNTIME_ERROR"
	StatusCompileError        = "COMPILE_ERROR"
	StatusSystemError         = "SYSTEM_ERROR"
)

// 退出码约定：
//   - timeout 包装器触发超时（SIGKILL）返回 124；
//   - 容器被 cgroup OOM killer 杀死返回 137（128+9）。
const (
	exitOK   = 0
	exitTLE  = 124
	exitOOM  = 137
	exitKill = -1 // 判定机自身兜底超时（非容器退出码）
)

// ClassifyExit 根据运行退出码判定单用例结果。
// 返回空字符串表示进程正常结束（exit 0），需进一步比对输出。
func ClassifyExit(code int) string {
	switch code {
	case exitTLE, exitKill:
		return StatusTimeLimitExceeded
	case exitOOM:
		return StatusMemoryLimitExceeded
	case exitOK:
		return ""
	default:
		return StatusRuntimeError
	}
}
