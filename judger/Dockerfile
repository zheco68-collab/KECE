# Judger runtime image (Go)
FROM golang:1.22-alpine AS build
WORKDIR /app
COPY go.mod ./
RUN go mod download || true
COPY . .
RUN CGO_ENABLED=0 go build -o /judger ./cmd/judger || true

FROM alpine:3.19
RUN apk add --no-cache docker-cli
COPY --from=build /judger /usr/local/bin/judger
ENTRYPOINT ["judger"]
