// Package model 定义判题机与后端之间的任务、状态数据结构。
// 字段名与后端 DTO（Task、SubmissionVO 等）严格保持一致。
package model

// Task 为后端 RPUSH 到 judge:queue 的判题任务。
// Type 为 "sql" 时走数据库判题路径（ProcessSQL），否则走代码沙箱路径。
type Task struct {
	SubmissionID  int64             `json:"submissionId"`
	QuestionID    int64             `json:"questionId"`
	Type          string            `json:"type"`
	Dialect       string            `json:"dialect"`
	Language      string            `json:"language"`
	CodeContent   map[string]string `json:"codeContent"`
	TimeLimitMs   int               `json:"timeLimitMs"`
	MemoryLimitMb int               `json:"memoryLimitMb"`
	TestCases     []TestCase        `json:"testCases"`
	// 数据库任务专用：建表语句与标准答案在题目级，所有测试点共用。
	SchemaSql      string `json:"schemaSql"`
	ReferenceSql   string `json:"referenceSql"`
	VerifySql      string `json:"verifySql"`
	OrderSensitive bool   `json:"orderSensitive"`
}

// TestCase 为单个测试点。
// 代码任务仅使用 CaseID/Input/ExpectedOutput；数据库任务只用 SeedSql（该测试点的一套数据）。
type TestCase struct {
	CaseID         int64  `json:"caseId"`
	Input          string `json:"input"`
	ExpectedOutput string `json:"expectedOutput"`
	// 数据库任务专用：该测试点的初始数据。
	SeedSql string `json:"seedSql"`
}

// Progress 描述判题进度，对应后端 ProgressVO。
type Progress struct {
	TotalCases    int `json:"totalCases"`
	FinishedCases int `json:"finishedCases"`
}

// Result 为终态判题结果，对应后端 SubmissionResultVO。
// 使用指针以支持 JSON null（与 Jackson 行为一致）。
type Result struct {
	Score           *int         `json:"score"`
	TimeUsedMs      *int         `json:"timeUsedMs"`
	MemoryUsedKb    *int         `json:"memoryUsedKb"`
	CompileErrorLog *string      `json:"compileErrorLog"`
	TestCasesReport []CaseResult `json:"testCasesReport"`
}

// CaseResult 为单个测试点的结果，对应后端 TestCaseResultVO。
type CaseResult struct {
	CaseID         int64   `json:"caseId"`
	Status         string  `json:"status"`
	TimeMs         *int    `json:"timeMs"`
	MemoryKb       *int    `json:"memoryKb"`
	ExpectedOutput *string `json:"expectedOutput"`
	ActualOutput   *string `json:"actualOutput"`
}

// SubmissionVO 为写入 sub:status:{id} 的完整状态对象，对应后端 SubmissionVO。
type SubmissionVO struct {
	SubmissionID int64     `json:"submissionId"`
	Status       string    `json:"status"`
	Progress     *Progress `json:"progress"`
	Result       *Result   `json:"result"`
}
