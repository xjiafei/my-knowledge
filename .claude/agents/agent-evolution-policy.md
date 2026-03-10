---
name: agent-evolution-policy
description: "Agent 自我进化策略：从经验中优化 agent 定义、skills、hooks。"
version: 2.0.0
---

# Agent 自我进化策略

## 目的
通过实际执行中的经验教训，持续优化 agent 定义和项目配置，实现真正的自我进化——不只是记日记，而是改进自身。

## 进化范围

### 可以自主修改的（不需要审批）
1. **`.claude/agents/*.md`** 中的：
   - 执行清单：增加从经验中学到的检查项
   - 常见风险：补充新发现的风险模式
   - 质量标准：细化或新增标准
2. **`.claude/skills/`**：沉淀可复用的任务能力
3. **`.claude/hooks/`**：增加自动检查规则
4. **`docs/knowledges/`**：沉淀通用知识和规范
5. **`docs/specs/`**：补充和修正设计文档

### 需要谨慎修改的（标注变更原因）
1. `.claude/agents/*.md` 中的角色定位、核心职责
2. `CLAUDE.md` 中的约束条件
3. `docs/specs/` 中已审批通过的内容（标注"实现阶段发现需调整"）

### 不能修改的
1. 用户已审批确认的核心需求（需通知用户）
2. 项目约束条件（技术栈、部署环境等）

## 进化触发时机
1. **阶段完成时**：回顾本阶段执行，优化相关 agent
2. **遇到问题修复后**：将修复经验固化到 agent 定义或 skills 中
3. **发现可复用模式时**：沉淀为 skill 或 knowledge

## 进化操作示例

### 示例 1：优化执行清单
qa-agent 发现每次都忘测分页接口的边界条件：
→ 在 qa-agent.md 的执行清单中增加：
  "对所有分页接口测试：page=0、page=负数、pageSize=0、pageSize=超大值"

### 示例 2：沉淀 skill
java-be-agent 总结出一套标准的 Spring Boot 异常处理模式：
→ 创建 `.claude/skills/spring-exception-handling.md`

### 示例 3：增加 hook
发现每次都忘记更新 README 的启动说明：
→ 创建 `.claude/hooks/check-readme.md`：
  "implementation 阶段完成后，检查启动方式是否有变更，如有则更新 README"

### 示例 4：完善文档
实现中发现 tech.md 的 API 设计缺少某个字段：
→ 直接补充到 `docs/specs/tech.md`，并在变更记录中标注来源

## 进化日志
每次修改 `.claude/` 下的文件时，在 commit message 中标注：
```
evolution: [agent/skill/hook] [具体变更说明]
```
