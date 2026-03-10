# 文档资产治理规范（DOC_GOVERNANCE.md）

## 目的
确保项目知识资产在研发全流程中持续沉淀、可追溯、不流失。

## 一、文档变更规范

所有文档的新增或修改建议包含变更记录：

| 字段 | 说明 | 示例 |
|------|------|------|
| 来源 | 触发变更的阶段/agent | tech 阶段 / arch-agent |
| 变更说明 | 做了什么改动 | 新增用户认证 API 设计 |
| 影响范围 | 哪些下游文档/代码受影响 | backend Controller 层 |

建议在文档末尾维护变更日志：
```markdown
## 变更记录
| 日期 | 来源 | 变更说明 | 影响范围 |
|------|------|---------|---------|
```

## 二、文档沉淀指引

Claude Code 在执行过程中，应主动检查以下沉淀需求：

1. **specs 更新**：本阶段是否产生了需要更新到 `docs/specs/` 的内容？
2. **knowledges 沉淀**：是否有通用知识/规范/模式需要沉淀到 `docs/knowledges/`？
3. **README 更新**：项目运行方式是否有变化？
4. **实现偏差**：实现中发现设计文档有误，应直接修正并标注来源

## 三、增量特性合并规则

增量特性（`docs/specs/featureXXX-specs/`）交付后：

1. **合并时机**：特性测试通过后
2. **合并方式**：
   - requirements.md：将特性需求追加到全量文档对应模块下
   - product.md：将特性页面/交互追加到全量文档
   - tech.md：将特性 API/数据库变更合并到全量文档
3. **冲突处理**：以最新特性为准，标注冲突点
4. **追溯保留**：`featureXXX-specs/` 目录保留不删除
5. **合并标记**：在全量 specs 变更记录中注明 "merged from featureXXX"

## 四、知识沉淀触发条件

以下场景应触发知识沉淀到 `docs/knowledges/`：
- 发现可复用的编码模式 → `knowledges/standards/`
- 发现领域业务规则 → `knowledges/domain/`
- 总结出文档/设计模板 → `knowledges/templates/`
