# .claude 目录协作规范

本目录遵循"高质量定义 + 自主调用"原则：

1. `agents/`：定义专业角色（YAML frontmatter: name/description/tools/model/version + 详细说明）
2. `skills/`：定义可复用任务能力
3. `hooks/`：定义自动检查规则
4. `commands/`：定义可复用命令入口
5. `settings.json`：工具权限配置

## 执行原则
- Claude Code 根据任务目标自主识别并调用上述能力，不需要外部指定调用顺序
- 每个 agent 有明确的"何时调用 / 何时不用"边界
- Claude Code 有权在执行中优化 agent/skill/hook 定义（参见 agent-evolution-policy.md）

## Agent 定义质量要求
- YAML frontmatter：name、description、tools、model、version
- 正文包含：角色定位、调用边界、质量检查清单（按严重级别分层）、诊断命令、代码示例、执行清单、交付标准
- 动态业务内容通过 `<!-- DYNAMIC_INJECT_START/END -->` 块注入
