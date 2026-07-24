# 多阶段构建 - 前端 Dockerfile
# Stage 1: 构建阶段
FROM node:20-alpine AS builder

WORKDIR /app

# 设置 npm 镜像源加速
RUN npm config set registry https://registry.npmmirror.com

# 复制 package.json 并安装依赖
COPY package*.json ./
RUN npm install --legacy-peer-deps

# 复制源代码并构建
COPY . .
RUN npm run build

# Stage 2: 运行阶段
FROM nginx:alpine

# 复制构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制 nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 暴露端口
EXPOSE 80

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=10s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
