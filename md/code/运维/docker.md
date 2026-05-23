# Docker 学习笔记

---

## 1. Docker 是什么

Docker 是一个开源的**容器化平台**，用于构建、运行和分发容器化应用。

### 核心概念

| 概念 | 说明 |
|------|------|
| **镜像 (Image)** | 只读模板，包含运行应用所需的一切（代码、运行时、库、环境变量、配置文件） |
| **容器 (Container)** | 镜像的运行实例，相互隔离，可被创建、启动、停止、删除 |
| **仓库 (Registry)** | 存储和分发镜像的服务，如 Docker Hub、阿里云容器镜像服务 |
| **Dockerfile** | 定义镜像构建过程的文本文件 |
| **数据卷 (Volume)** | 持久化容器数据的机制，容器删除后数据不丢失 |

### Docker vs 虚拟机

| 特性    | Docker 容器      | 虚拟机              |
| ----- | -------------- | ---------------- |
| 虚拟化层级 | 操作系统级（共享宿主机内核） | 硬件级（完整 Guest OS） |
| 启动速度  | 秒级             | 分钟级              |
| 性能损耗  | 接近原生           | 较大（20%-50%）      |
| 镜像大小  | MB 级           | GB 级             |
| 隔离性   | 进程隔离，较弱        | 完全隔离，较强          |

### Docker 架构（CS 架构）

```
客户端 (CLI)  ──→  Docker 守护进程 (dockerd)  ──→  容器运行时 (containerd/runc)
                                    ↓
                           镜像 / 容器 / 网络 / 存储
```

- **docker CLI**：用户与 Docker 交互的命令行工具
- **Docker daemon (dockerd)**：后台服务，管理 Docker 对象
- **containerd**：管理容器生命周期的行业标准运行时
- **runc**：轻量级 OCI 容器运行时

---

## 2. 镜像操作

### 2.1 常用命令

```bash
# 搜索镜像
docker search <镜像名>
docker search nginx --filter=stars=100

# 拉取镜像
docker pull <镜像名>:<标签>
docker pull nginx:1.25
docker pull ubuntu:22.04

# 查看本地镜像
docker images
docker image ls

# 查看镜像详细信息
docker inspect <镜像名>

# 删除镜像
docker rmi <镜像名>:<标签>
docker image rm <镜像名>
docker image prune -a       # 清理所有无用镜像

# 导出/导入镜像（离线传输）
docker save -o nginx.tar nginx:1.25
docker load -i nginx.tar

# 构建镜像
docker build -t <镜像名>:<标签> <路径>
docker build -t myapp:1.0 .
docker build -t myapp:1.0 -f Dockerfile.dev .
```
| **命令**        | **说明**                       |
| -------------- | ------------------------------ |
| `docker pull`  | 拉取镜像                       |
| `docker push`  | 推送镜像到 Docker Registry     |
| `docker images`| 查看本地镜像                   |
| `docker rmi`   | 删除本地镜像                   |
| `docker run`   | 创建并运行容器（不能重复创建）   |
| `docker stop`  | 停止指定容器                   |
| `docker start` | 启动指定容器                   |
| `docker restart`| 重新启动容器                  |
| `docker rm`    | 删除指定容器                   |
| `docker ps`    | 查看容器                       |
| `docker logs`  | 查看容器运行日志               |
| `docker exec`  | 进入容器                       |
| `docker save`  | 保存镜像到本地压缩文件         |
| `docker load`  | 加载本地压缩文件到镜像         |
| `docker inspect`| 查看容器详细信息              |
### 2.2 镜像分层原理

Docker 镜像由多个**只读层**叠加组成：

```
┌─────────────────────┐
│   应用代码层         │  ← COPY . /app
├─────────────────────┤
│   npm install 层    │  ← RUN npm install
├─────────────────────┤
│   node:18-alpine    │  ← FROM node:18-alpine
└─────────────────────┘
```

- 每条 Dockerfile 指令创建一个新层
- 相同的层可以在不同镜像间共享，节省存储空间
- 容器启动时在最顶层添加一个**可写层**

### 2.3 镜像标签策略

```bash
# 语义化版本
docker build -t myapp:1.0.0 .
docker build -t myapp:1.0 .

# Git commit hash
docker build -t myapp:abc1234 .

# latest 标签（慎用，不可靠）
docker build -t myapp:latest .
```

---

## 3. Dockerfile 编写

### 3.1 基本指令

```dockerfile
# 基础镜像
FROM node:18-alpine

# 镜像元数据
LABEL maintainer="your@email.com"
LABEL version="1.0"
LABEL description="My Node.js App"

# 环境变量
ENV NODE_ENV=production
ENV APP_PORT=3000

# 工作目录（不存在则自动创建）
WORKDIR /app

# 复制文件（推荐先复制依赖文件，利用缓存）
COPY package*.json ./
RUN npm install --production

# 复制剩余代码
COPY . .

# 声明容器运行时监听的端口
EXPOSE 3000

# 健康检查（可选）
HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:3000/health || exit 1

# 启动命令
CMD ["node", "server.js"]
```

### 3.2 COPY vs ADD

| 指令     | 说明                                     |
| ------ | -------------------------------------- |
| `COPY` | 仅复制文件到镜像，推荐用于大多数场景                     |
| `ADD`  | 复制文件 + 自动解压 tar 包 + 支持 URL 下载，功能更多但更复杂 |

**建议**：除非需要自动解压，否则优先使用 `COPY`，行为更可预测。

### 3.3 CMD vs ENTRYPOINT

| 指令 | 说明 |
|------|------|
| `CMD` | 提供默认启动命令，可被 `docker run` 参数覆盖 |
| `ENTRYPOINT` | 配置容器启动时执行的命令，不容易被覆盖 |

```bash
# ENTRYPOINT + CMD 组合示例
# Dockerfile
ENTRYPOINT ["python"]
CMD ["app.py"]

# 运行
docker run myapp           # → python app.py
docker run myapp script.py # → python script.py（CMD 被覆盖）
```

### 3.4 多阶段构建（Multi-stage Build）

减小最终镜像体积的关键技术：

```dockerfile
# 构建阶段
FROM node:18 AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# 运行阶段
FROM node:18-alpine
WORKDIR /app
COPY --from=builder /app/dist ./dist
COPY --from=builder /app/node_modules ./node_modules
EXPOSE 3000
CMD ["node", "dist/index.js"]
```

好处：构建工具（编译器、开发依赖）不会进入最终镜像，镜像体积可减少 50%+。

### 3.5 Dockerfile 最佳实践

```dockerfile
# ✅ 好的做法
FROM node:18-alpine          # 使用 alpine 变体（体积小）
RUN apk add --no-cache curl  # 合并 RUN 命令减少层数
COPY package*.json ./        # 利用构建缓存
RUN npm ci --production      # npm ci 比 npm install 更快更可靠
USER node                    # 使用非 root 用户运行

# ❌ 避免的做法
# FROM node:18               # 不要用完整版（体积大）
# RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
# RUN npm install             # 不要用 npm install（可重复性差）
# COPY . .                    # 一次性复制所有文件会破坏缓存
# USER root                   # 不要用 root 运行应用
```

### 3.6 .dockerignore

类似 `.gitignore`，排除不需要的文件：

```
node_modules
.git
.env
*.md
Dockerfile
.dockerignore
```

---

## 4. 容器管理

### 4.1 运行容器
docker run-------创建容器
docker start-------启动容器
docker run -d------后台运行

```bash
# 基本运行
docker run <镜像名>
docker run nginx

# 后台运行（-d）
docker run -d nginx

# 自动删除（--rm）
docker run --rm nginx

# 端口映射（-p）
docker run -d -p 8080:80 nginx
# 格式: -p 宿主机端口:容器端口

# 多端口映射
docker run -d -p 80:80 -p 443:443 nginx

# 容器命名（--name）
docker run -d --name my-nginx nginx

# 交互式运行（-it）
docker run -it ubuntu bash

# 重启策略（--restart）
docker run -d --restart=always nginx
# 可选: no | on-failure | always | unless-stopped

# 环境变量（-e）
docker run -d -e MYSQL_ROOT_PASSWORD=123456 mysql:8

# 资源限制
docker run -d --memory=512m --cpus=1.5 nginx

# 挂载数据卷（-v）
docker run -d -v /host/path:/container/path nginx
```

### 4.2 容器生命周期

```bash
# 查看运行中的容器
docker ps

# 查看所有容器（含已停止）
docker ps -a

# 启动 / 停止 / 重启
docker start <容器名>
docker stop <容器名>
docker restart <容器名>

# 删除容器
docker rm <容器名>
docker container prune    # 删除所有已停止的容器

# 强制停止
docker kill <容器名>

# 暂停 / 恢复
docker pause <容器名>
docker unpause <容器名>
```

### 4.3 进入容器

```bash
# 进入正在运行的容器（推荐）
docker exec -it <容器名> bash
docker exec -it <容器名> sh    # Alpine 没有 bash

# 进入容器（已停止的容器不能 exec）
docker attach <容器名>          # 不推荐，退出会停止容器
```

### 4.4 查看日志

```bash
# 查看容器日志
docker logs <容器名>

# 实时跟踪日志（类似 tail -f）
docker logs -f <容器名>

# 查看最近 100 行
docker logs --tail 100 <容器名>

# 查看指定时间后的日志
docker logs --since 2024-01-01T00:00:00 <容器名>
```

### 4.5 容器与宿主机文件互传

```bash
# 从宿主机复制到容器
docker cp ./file.txt <容器名>:/app/file.txt

# 从容器复制到宿主机
docker cp <容器名>:/app/file.txt ./file.txt
```

---

## 5. 网络

### 5.1 网络模式

| 模式 | 说明 | 使用场景 |
|------|------|----------|
| **bridge**（默认） | Docker 创建虚拟网桥，容器通过网桥通信 | 大多数场景 |
| **host** | 容器直接使用宿主机网络栈 | 对网络性能要求极高 |
| **none** | 容器无网络，完全隔离 | 安全敏感场景 |
| **overlay** | 跨主机容器通信 | Docker Swarm / 集群 |
| **macvlan** | 容器拥有独立 MAC 地址 | 需要直接接入物理网络 |

### 5.2 网络操作

```bash
# 查看网络
docker network ls

# 创建自定义网络
docker network create my-network
docker network create --driver bridge --subnet 172.20.0.0/16 my-network

# 将容器加入网络
docker network connect my-network <容器名>

# 将容器移出网络
docker network disconnect my-network <容器名>

# 查看网络详情
docker network inspect my-network

# 删除网络
docker network rm my-network
docker network prune     # 删除所有未使用的网络
```

### 5.3 容器间通信

使用自定义网络后，容器间可通过 **容器名** 直接通信：

```bash
# 创建网络
docker network create app-net

# 运行 MySQL
docker run -d --name mysql --network app-net \
  -e MYSQL_ROOT_PASSWORD=123456 mysql:8

# 运行应用，通过容器名连接数据库
docker run -d --name myapp --network app-net \
  -e DB_HOST=mysql \
  -e DB_PORT=3306 myapp:1.0

# myapp 容器内可直接用 "mysql" 作为主机名访问数据库
```

---

## 6. 数据持久化

### 6.1 数据卷 (Volume)

Docker 管理的持久化存储，数据存储在 Docker 管理的目录中。

```bash
# 创建数据卷
docker volume create my-vol

# 查看所有数据卷
docker volume ls

# 查看数据卷详情
docker volume inspect my-vol

# 使用数据卷运行容器
docker run -d -v my-vol:/app/data nginx

# 匿名卷（Docker 自动生成名称）
docker run -d -v /app/data nginx

# 删除数据卷
docker volume rm my-vol
docker volume prune    # 删除所有未使用的数据卷
```

### 6.2 绑定挂载 (Bind Mount)

将宿主机目录直接挂载到容器中：

```bash
# 挂载宿主机目录
docker run -d -v /host/path:/container/path nginx

# 只读挂载
docker run -d -v /host/path:/container/path:ro nginx

# Windows 路径
docker run -d -v /c/Users/aa/project:/app nginx
```

### 6.3 Volume vs Bind Mount

| 特性   | Volume             | Bind Mount |
| ---- | ------------------ | ---------- |
| 管理方式 | Docker 管理          | 手动管理       |
| 性能   | 更好（OS 管理）          | 取决于宿主机文件系统 |
| 可移植性 | 好                  | 依赖宿主机路径    |
| 备份   | `docker volume` 命令 | 直接操作宿主机文件  |
| 推荐场景 | 生产环境数据持久化          | 开发时代码热加载   |

### 6.4 tmpfs 挂载

数据存储在内存中，容器停止后消失：

```bash
docker run -d --tmpfs /app/temp nginx
```

适用场景：敏感信息临时存储、缓存等。

---

## 7. Docker Compose

### 7.1 简介

Docker Compose 用于定义和运行**多容器应用**，通过 YAML 文件管理。

### 7.2 docker-compose.yml 示例

```yaml
version: '3.8'

services:
  # Web 应用
  web:
    build:
      context: .
      dockerfile: Dockerfile
    ports:
      - "3000:3000"
    environment:
      - NODE_ENV=production
      - DB_HOST=db
    depends_on:
      db:
        condition: service_healthy
    volumes:
      - ./src:/app/src    # 开发时热加载
    networks:
      - app-net
    restart: unless-stopped

  # 数据库
  db:
    image: mysql:8
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_PASSWORD}
      MYSQL_DATABASE: myapp
    volumes:
      - db-data:/var/lib/mysql
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
    ports:
      - "3306:3306"
    networks:
      - app-net
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  # Redis
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    networks:
      - app-net
    restart: unless-stopped

# 数据卷
volumes:
  db-data:

# 网络
networks:
  app-net:
    driver: bridge
```

### 7.3 Compose 命令

```bash
# 启动所有服务（后台）
docker compose up -d

# 启动并重新构建镜像
docker compose up -d --build

# 查看服务状态
docker compose ps

# 查看日志
docker compose logs -f
docker compose logs -f web    # 指定服务

# 停止服务
docker compose stop

# 停止并删除容器、网络
docker compose down

# 停止并删除容器、网络、数据卷
docker compose down -v

# 进入容器
docker compose exec web bash

# 运行一次性命令
docker compose run --rm web npm test

# 重新构建镜像
docker compose build

# 扩缩容
docker compose up -d --scale web=3

# 查看容器资源使用
docker compose top
```

### 7.4 Compose 环境变量

```bash
# 方式 1: .env 文件（与 docker-compose.yml 同目录）
DB_PASSWORD=123456
REDIS_PORT=6379

# 方式 2: 环境变量文件
# docker-compose.yml
services:
  db:
    env_file:
      - ./db.env

# db.env
MYSQL_ROOT_PASSWORD=123456
MYSQL_DATABASE=myapp

# 方式 3: 命令行指定
DB_PASSWORD=secret docker compose up -d
```

---

## 8. Dockerfile 高级用法

### 8.1 ARG 与 ENV

```dockerfile
# ARG：构建时的变量（不保留在最终镜像中）
ARG NODE_VERSION=18
FROM node:${NODE_VERSION}-alpine

# ENV：运行时的变量（保留在最终镜像中）
ENV NODE_ENV=production
```

| 指令 | 作用阶段 | 是否保留在镜像中 |
|------|----------|------------------|
| `ARG` | 构建时 | 否 |
| `ENV` | 运行时 | 是 |

### 8.2 构建缓存优化

```dockerfile
FROM node:18-alpine
WORKDIR /app

# ✅ 先复制依赖声明文件
COPY package.json package-lock.json ./
RUN npm ci --production

# ✅ 再复制源代码（依赖不变时跳过 npm ci）
COPY . .

CMD ["node", "index.js"]
```

### 8.3 使用 BuildKit

```bash
# 启用 BuildKit（Docker 18.09+ 默认启用）
DOCKER_BUILDKIT=1 docker build -t myapp .

# 前端缓存（加速 npm install）
docker build --progress=plain --no-cache .
```

---

## 9. 常用场景与实战

### 9.1 Nginx 反向代理

```bash
docker run -d \
  --name nginx-proxy \
  -p 80:80 \
  -v ./nginx.conf:/etc/nginx/nginx.conf:ro \
  -v ./html:/usr/share/nginx/html:ro \
  nginx:alpine
```

### 9.2 MySQL 数据库

```bash
docker run -d \
  --name mysql-db \
  -p 3306:3306 \
  -v mysql-data:/var/lib/mysql \
  -e MYSQL_ROOT_PASSWORD=123456 \
  -e MYSQL_DATABASE=myapp \
  -e MYSQL_USER=dev \
  -e MYSQL_PASSWORD=dev123 \
  mysql:8
```

### 9.3 Redis 缓存

```bash
docker run -d \
  --name redis \
  -p 6379:6379 \
  -v redis-data:/data \
  redis:7-alpine \
  redis-server --appendonly yes
```

### 9.4 Node.js 开发环境（带热重载）

```yaml
# docker-compose.dev.yml
version: '3.8'

services:
  app:
    build:
      context: .
      dockerfile: Dockerfile.dev
    ports:
      - "3000:3000"
    volumes:
      - ./src:/app/src    # 代码改动自动重启
      - /app/node_modules # 排除 node_modules
    environment:
      - NODE_ENV=development
```

```dockerfile
# Dockerfile.dev
FROM node:18-alpine
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
CMD ["npm", "run", "dev"]
```

---

## 10. 镜像优化与安全

### 10.1 减小镜像体积

```bash
# 查看镜像各层大小
docker history <镜像名>

# 使用 dive 工具分析
docker run --rm -it \
  -v /var/run/docker.sock:/var/run/docker.sock \
  wagoodman/dive <镜像名>
```

**优化策略：**
- 使用 `alpine` 或 `slim` 基础镜像
- 多阶段构建
- 减少 `RUN` 指令数量（合并命令）
- 使用 `.dockerignore` 排除不必要文件
- 清理包管理器缓存（`--no-cache` / `rm -rf /var/lib/apt/lists/*`）

### 10.2 安全实践

```dockerfile
# 使用非 root 用户
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# 不安装不必要的包
RUN apk add --no-cache --virtual .build-deps \
    gcc musl-dev \
    && npm install \
    && apk del .build-deps

# 扫描镜像漏洞
docker scout cves <镜像名>
```

**安全清单：**
- ✅ 使用官方镜像
- ✅ 使用非 root 用户运行
- ✅ 定期更新基础镜像
- ✅ 不在镜像中存储密钥
- ✅ 使用 `.dockerignore`
- ✅ 启用 Docker Content Trust

---

## 11. 调试与排查

### 11.1 常用调试命令

```bash
# 查看容器进程
docker top <容器名>

# 查看容器资源使用
docker stats
docker stats <容器名>

# 查看容器详情
docker inspect <容器名>

# 查看容器文件系统变更
docker diff <容器名>

# 查看容器端口映射
docker port <容器名>

# 查看容器元数据
docker inspect --format='{{.NetworkSettings.IPAddress}}' <容器名>
```

### 11.2 常见问题排查

```bash
# 容器启动后立即退出
docker logs <容器名>           # 查看退出原因
docker inspect <容器名>        # 查看 State.ExitCode

# 无法连接到容器服务
docker exec -it <容器名> sh    # 进入容器检查
curl localhost:3000            # 在容器内测试服务

# 端口冲突
docker ps -a                   # 查看占用端口的容器
docker stop <占用端口的容器>

# 磁盘空间不足
docker system df               # 查看 Docker 磁盘使用
docker system prune -a         # 清理所有未使用资源（谨慎）
```

---

## 12. Docker 与 CI/CD

### 12.1 在 GitHub Actions 中使用 Docker

```yaml
# .github/workflows/build.yml
name: Build and Push Docker Image

on:
  push:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3
      
      - name: Login to Docker Hub
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKER_HUB_USER }}
          password: ${{ secrets.DOCKER_HUB_TOKEN }}
      
      - name: Build and push
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: youruser/myapp:latest
          cache-from: type=gha
          cache-to: type=gha,mode=max
```

---

## 13. 常用命令速查表

| 分类 | 命令 |
|------|------|
| **镜像** | `docker build -t name .` / `docker pull img` / `docker images` / `docker rmi img` |
| **容器** | `docker run -d -p 80:80 name` / `docker ps -a` / `docker stop/start/rm container` |
| **日志** | `docker logs -f container` |
| **进入** | `docker exec -it container bash` |
| **网络** | `docker network create/ls/inspect/rm` |
| **数据卷** | `docker volume create/ls/inspect/rm` |
| **Compose** | `docker compose up -d` / `docker compose down` / `docker compose ps` / `docker compose logs -f` |
| **清理** | `docker system df` / `docker system prune -a` |
| **复制** | `docker cp src container:dest` |
| **构建** | `docker buildx build --platform linux/amd64 -t name .` |

---

## 14. 学习资源

- [Docker 官方文档](https://docs.docker.com/)
- [Docker Hub](https://hub.docker.com/)
- [Play with Docker](https://labs.play-with-docker.com/) — 在线练习
- [Docker Compose 文件规范](https://docs.docker.com/compose/compose-file/)
- [Dockerfile 最佳实践](https://docs.docker.com/build/building/best-practices/)
