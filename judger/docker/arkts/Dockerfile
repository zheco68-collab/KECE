# ArkTS (TypeScript compatible) Sandbox Image for KECE Judger
FROM node:20-alpine

# 安装 TypeScript 编译器和 Node 类型定义，加速编译与语法严格检查
RUN npm install -g typescript @types/node

# 创建并设定沙箱默认工作目录
WORKDIR /work

CMD ["sh"]

