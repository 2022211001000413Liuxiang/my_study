# 个人学习记录

Vue 3 + Spring Boot 的本地 Markdown 学习工作台。

## 目录

- `frontend/`: Vue 3 + TypeScript + Vite 前端
- `backend/`: Spring Boot REST API
- `md/`: Markdown 笔记与附件
- `.study-meta.json`: 收藏、状态、标签等元数据

## 开发

```bash
npm install
npm install --prefix frontend
npm run dev
```

前端默认运行在 `http://127.0.0.1:5173`，后端默认运行在 `http://127.0.0.1:4174`。

## 配置

从 `.env.example` 创建 `.env`：

```bash
STUDY_ADMIN_PASSWORD=change-me
PORT=4174
STUDY_MD_ROOT=md
STUDY_META_PATH=.study-meta.json
```

## 检查

```bash
npm run check
npm run build
```
