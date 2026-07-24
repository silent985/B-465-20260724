# 多阶段构建 - 后端 Dockerfile
# Stage 1: 构建阶段
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# 配置阿里云 Maven 镜像加速
RUN mkdir -p /root/.m2 && \
    echo '<?xml version="1.0" encoding="UTF-8"?><settings><mirrors><mirror><id>aliyun</id><mirrorOf>central</mirrorOf><url>https://maven.aliyun.com/repository/central</url></mirror></mirrors></settings>' > /root/.m2/settings.xml

# 复制 pom.xml 并下载依赖（利用 Docker 缓存）
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源代码并构建
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: 运行阶段
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 创建非 root 用户
RUN addgroup -S lottery && adduser -S lottery -G lottery

# 创建日志目录
RUN mkdir -p /app/logs && chown -R lottery:lottery /app

# 从构建阶段复制 jar
COPY --from=builder /app/target/*.jar app.jar

# 切换到非 root 用户
USER lottery

# 暴露端口
EXPOSE 8000

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8000/actuator/health || exit 1

# 启动命令
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "-Djava.security.egd=file:/dev/./urandom", "app.jar"]
