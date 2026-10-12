package main

import (
	"fmt"
	"os"
	"os/signal"
	"syscall"
)

func main() {
	fmt.Println("KECE Judger - starting...")
	// TODO: Phase 1 Sprint 2 - implement worker: pull task, compile, run in sandbox, write result
	fmt.Println("KECE Judger - not implemented yet (Phase 1 Sprint 2)")

	// 阻塞等待退出信号，避免空实现进程立即退出后被 docker restart 循环拉起
	ch := make(chan os.Signal, 1)
	signal.Notify(ch, syscall.SIGINT, syscall.SIGTERM)
	<-ch
}
