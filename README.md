# 个人知识库管理系统

> 由 AIFD v2 框架初始化。

## 技术栈
Spring Boot + Vue + MySQL

## 快速开始

### 后端
```bash
cd backend
mvn spring-boot:run
```

### 前端
```bash
cd frontend
npm install
npm run dev
```

### Docker 一键启动
```bash
cp .env.example .env  # 编辑环境变量
docker-compose up -d
```

## 目录结构
```
├── .claude/              # Claude Code 配置
│   ├── agents/           # Agent 角色定义
│   ├── skills/           # 可复用任务能力
│   ├── hooks/            # 自动检查规则
│   └── commands/         # 自定义命令
├── docs/
│   ├── specs/            # 需求、产品、技术设计文档
│   └── knowledges/       # 编码规范、领域知识
├── backend/              # 后端代码（Spring Boot）
├── frontend/             # 前端代码（Vue3）
├── testing/              # 测试报告
└── workspace/            # 流水线状态
```

## 研发流程
本项目使用 AIFD v2 全流程研发框架：
1. 需求分析 → 架构师审批
2. 产品设计 → 架构师审批
3. 技术设计 → 架构师审批
4. 编码实现 + 测试验证 → 自动完成
