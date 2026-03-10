# Hooks 约定

用于定义阶段执行中的自动检查。

建议最小集合：
- pre-stage-check.md：阶段开始前输入完整性检查
- post-stage-doc-check.md：阶段结束后 docs 沉淀检查
- post-stage-memory-check.md：阶段结束后 agent-memory 回写检查

说明：是否启用由 Claude Code 在执行中自主判断，AIFD 以质量门禁结果进行最终判定。
