# CLAUDE.md

## 项目概述
- 项目：个人知识库管理系统
- 技术栈：Spring Boot + Vue + MySQL
- 业务域：个人知识管理（笔记、标签、搜索）

## 当前任务
完成个人知识库管理系统的全流程交付。

核心功能：
1. 笔记/文章管理（CRUD、Markdown 编辑、富文本预览）
2. 标签/分类体系（多标签、层级分类）
3. 全文搜索
4. Markdown 编辑器

## 文件路径指引
- 需求/设计文档：docs/specs/
- 编码规范：docs/knowledges/standards/
- Agent 定义：.claude/agents/
- 项目记忆：workspace/memory.md
- 检查清单：workspace/checklist.json
- 自检日志：workspace/review-log.md

## 约束
- 完成后自行运行构建和测试验证
- 遵循 docs/knowledges/standards/ 下的编码规范

## 审批检查点
完成需求分析（requirements.md）、产品设计（product.md）、技术设计（tech.md）后，
**必须执行 Review Loop 自检循环**（见下方），自检通过后输出文档摘要并停止，等待架构师审批。
审批通过后会通过 --resume 继续会话。
技术设计审批通过后，进入实现阶段并执行自动闭环。

## Review Loop 自检循环（文档阶段）

每个需要审批的阶段（需求分析、产品设计、技术设计）完成文档初稿后，你必须执行以下自检循环：

### Step 1: 加载检查清单
从 `workspace/checklist.json` 读取当前阶段的检查清单。

### Step 2: 逐项检查
对照检查清单的每一项 `checkItems`，审查你的文档：
- 如果该项满足 `criteria`，标记 `"passes": true`
- 如果不满足，标记 `"passes": false`，记录具体缺陷

### Step 3: 修正未通过项
对所有 `passes: false` 的项，修改文档使其达标。

### Step 4: 记录自检日志
将本轮检查结果追加写入 `workspace/review-log.md`，格式：

```markdown
### Iteration {N} — {stage}
- {id}: ✅/❌ {说明，如修正了什么}
```

### Step 5: 判断是否继续
- 如果迭代次数 < `minIterations`（默认 2）→ **即使全部通过也必须继续**，回到 Step 2
- 如果迭代次数 >= `minIterations` 且所有项 `passes: true` → 更新 `workspace/checklist.json` → 退出循环，输出文档摘要
- 如果仍有未通过项且迭代次数 < `maxIterations` → 回到 Step 2
- 如果达到 `maxIterations` 仍有未通过项 → 更新 `workspace/checklist.json`（保留未通过状态）→ 退出循环，输出文档摘要并列出未通过项

### 多轮自检策略
- **第 1 轮**：以文档作者视角检查，对照 checklist 逐项验证
- **第 2 轮**：切换为**架构师的挑剔视角**，重新审查所有项。尝试找出第 1 轮遗漏的问题：
  - 验收标准是否真的可测试？还是写得模糊？
  - 非功能需求的数字是否合理？有没有拍脑袋？
  - 边界和排除项是否足够明确？会不会有歧义？
  - 术语是否真的一致？有没有同义词混用？
- **第 3 轮（如需）**：修复第 2 轮发现的问题后再次全量检查
- 连续 2 轮全部通过才能退出循环

### 重要约束
- 每轮迭代必须重新审查所有项（避免修 A 破 B）
- 第 2 轮不能敷衍通过，必须用批判性思维真正寻找问题
- 修正时优先补充内容，不要删减已有合理内容
- review-log.md 是追加写入，不要覆盖之前的记录
- 检查要严格：模糊的描述不算通过，必须具体可验证

## 实现-验收自动闭环（实现阶段）

你是编排者（orchestrator），负责调度各 subagent 完成编码、评审、测试和验收的闭环。你不亲自写代码。

### 角色分工
- 后端开发：java-be-agent（Spring Boot）
- 前端开发：vue-fe-agent（Vue3）
- 代码评审：code-reviewer
- 架构验收：arch-agent
- 测试验证：qa-agent
- 产品验收：pm-agent

### 闭环流程
1. 调度 java-be-agent 完成后端 → 调度 vue-fe-agent 完成前端
2. 调度 code-reviewer 评审 → 有问题调度开发 agent 修复 → 重新评审（最多 3 次）
3. 调度 arch-agent 验收 → 有问题调度开发 agent 修复 → 重新验收（最多 3 次）
4. 启动服务 + Playwright → 调度 qa-agent 测试 → 有 Bug 调度开发 agent 修复 → 重新测试（最多 3 次）
5. 调度 pm-agent 产品验收 → 有问题调度开发 agent 修复 → 回到步骤 2 重跑（最多 5 轮）
6. 全部通过 → 输出 workspace/final-report.json → 退出

### 兜底规则
- 单角色循环上限 3 次，全流程上限 5 轮
- 达到上限输出 final-report.json 标注未通过项后退出

## 用户偏好
- API 命名 RESTful 风格
- 文档简洁不冗长
- 先搭可跑通骨架再补细节

## 项目记忆
（新项目重新开始，暂无记忆）
