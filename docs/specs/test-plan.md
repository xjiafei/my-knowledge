# 测试方案 — 个人知识库管理系统

> 版本：1.0 | 日期：2026-03-10 | 作者：QA Agent

---

## 1. 测试目标

本测试方案旨在验证以下质量目标：

| 目标 | 验证内容 |
|------|---------|
| 功能正确性 | 笔记 CRUD、标签管理、分类管理、全文搜索的核心业务逻辑符合需求规格 |
| 接口契约 | 所有 REST API 响应格式（code/message/data）与技术设计文档一致 |
| 错误处理 | 非法参数、不存在资源、业务冲突均返回正确的 HTTP 语义错误码（400/404/409） |
| 数据一致性 | 级联操作（删除标签解除关联、删除分类置空笔记 category_id）正确执行 |
| 边界行为 | 分类层级限制（最多 3 级）、标签重名校验、关键词长度限制（200 字符）等边界正确处理 |
| 摘要生成 | 笔记摘要取 content 前 100 字符，content 为 null/空时返回空字符串 |
| 安全基线 | 错误响应不暴露内部堆栈信息，服务器异常统一返回 500 通用提示 |
| E2E 用户旅程 | 新建笔记、全文搜索、标签筛选、分类管理等主要用户旅程可端到端完成 |

**不在测试目标内的质量属性：**
- 性能压测（单用户系统，无并发压力）
- 安全渗透测试（P0 阶段无认证）
- 多浏览器兼容性矩阵（仅验证 Chrome）

---

## 2. 测试范围

### 2.1 包含范围

| 层次 | 范围描述 |
|------|---------|
| Service 单元测试 | NoteService、TagService、CategoryService 全部 public 方法 |
| API 集成测试 | `/api/notes`（含 `/search`）、`/api/tags`、`/api/categories` 全部端点 |
| 数据库行为 | 级联删除、ON DELETE CASCADE/SET NULL、FULLTEXT 搜索（H2 中以 LIKE 降级） |
| E2E 用户流程 | 新建笔记、查看/编辑/删除笔记、标签管理、标签筛选、全文搜索、分类管理 |

### 2.2 不包含范围

| 排除项 | 原因 |
|--------|------|
| 前端组件单元测试 | 本期优先保证 API 层质量，前端组件测试留待后续迭代 |
| MySQL FULLTEXT 搜索精度 | H2 不支持 FULLTEXT，集成测试用 LIKE 验证接口契约；精度验证依赖手工测试 |
| 性能 / 压力测试 | 单用户系统，当前数据量下无必要 |
| 导出、统计等 P2 功能 | 本期未实现 |
| 移动端 / 多浏览器 | 需求中明确排除 |

---

## 3. 测试策略

### 3.1 单元测试

**范围：** Service 层（NoteService、TagService、CategoryService）所有 public 方法，含正向、异常、边界分支。

**覆盖目标：** 行覆盖率 >= 80%（通过 JaCoCo 统计，目标整体 >= 85%）。

**工具：** JUnit 5 + Mockito（mock Mapper 层，不依赖数据库）。

**关注点：**
- `NoteService.generateSummary`：content 为 null、空串、<=100 字符、>100 字符四个分支
- `NoteService.createNote`：categoryId 不存在时抛 404
- `NoteService.searchNotes`：空关键词抛 400；超 200 字符抛 400
- `TagService.createTag`：重名抛 409
- `CategoryService.calculateDepth`：父分类为 3 级时抛 400
- `CategoryService.buildTree`：空列表返回空树；多层级正确递归
- `CategoryService.deleteCategory`：递归收集子分类 ID 后批量删除 + 笔记置空

**执行方式：**
```bash
cd /root/my-knowledge/knowledge-backend
mvn test
mvn test jacoco:report   # 生成覆盖率报告至 target/site/jacoco/
```

---

### 3.2 API 集成测试

**范围：** Controller + Service + Mapper 全栈，覆盖所有端点的正向和异常路径。

**工具：** Spring Boot Test（`@SpringBootTest + MockMvc`）或 curl 命令行脚本。

**环境：** H2 内存数据库（`spring.profiles.active=test`），每个测试用例执行前通过 `@Sql` 或 `@BeforeEach` 初始化必要数据，测试后回滚或清库。

**H2 测试 profile 配置说明（`application-test.yml`）：**

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
  h2:
    console:
      enabled: false
```

> 注意：H2 不支持 MySQL FULLTEXT 语法，全文搜索 SQL 需在测试环境降级为 `LIKE '%keyword%'`，通过 `@Profile("test")` 注入测试版 NoteMapper 实现或在 XML 中用条件判断。

**验证重点：**
- 响应结构：`{ code, message, data }` 或分页 `{ records, total, page, size }`
- HTTP 状态码（虽然业务 code 字段也携带语义，需同时验证 HTTP 200 + body code 字段）
- 错误信息字段 `message` 包含业务描述，不含堆栈信息

---

### 3.3 E2E 测试

**范围：** 覆盖以下 P0 用户旅程。

| 旅程 | 关键路径 |
|------|---------|
| 新建笔记 | 首页 → 点击"新建笔记" → 填写标题+内容+标签+分类 → 保存 → 详情页显示内容 |
| 搜索笔记 | 顶部搜索框输入关键词 → 搜索结果页 → 结果高亮 → 点击进入详情页 |
| 标签筛选 | 侧边栏点击标签 → 列表刷新为该标签笔记 → 清除筛选恢复全部 |
| 编辑/删除笔记 | 详情页点击编辑 → 修改保存 → 返回详情页确认更新；详情页点击删除确认 → 返回列表 |
| 分类管理 | 分类管理页创建父/子分类 → 验证树结构 → 删除父分类子分类置空 |

**工具：** Playwright（TypeScript），采用 Page Object 模式封装各页面操作。

**环境要求：**
- 后端服务运行于 `localhost:8080`，使用独立测试数据库（或在测试前通过 API 初始化干净数据）
- 前端开发服务运行于 `localhost:5173`
- 浏览器：Chromium（Playwright 内置）

**E2E 测试目录结构：**
```
testing/e2e/
├── pages/
│   ├── NoteListPage.ts
│   ├── NoteEditPage.ts
│   ├── NoteDetailPage.ts
│   ├── TagManagePage.ts
│   ├── CategoryManagePage.ts
│   └── SearchResultPage.ts
└── tests/
    ├── note.spec.ts
    ├── search.spec.ts
    ├── tag.spec.ts
    └── category.spec.ts
```

**执行方式：**
```bash
cd /root/my-knowledge
npx playwright test                        # 运行全部 E2E
npx playwright test tests/note.spec.ts     # 单个文件
npx playwright test --headed               # 可视化运行
npx playwright test --repeat-each=3        # Flaky 检测
npx playwright show-report                 # 查看 HTML 报告
```

---

## 4. 测试环境

### 4.1 后端（H2 集成测试环境）

```bash
# 方式一：Maven Surefire 自动使用 test profile
cd /root/my-knowledge/knowledge-backend
mvn test -Dspring.profiles.active=test

# 方式二：手动运行后端服务用于 E2E（连接真实 MySQL 测试库）
cd /root/my-knowledge/knowledge-backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
# 服务地址：http://localhost:8080
```

### 4.2 前端开发服务（E2E 环境）

```bash
cd /root/my-knowledge/knowledge-frontend
npm install
npm run dev
# 服务地址：http://localhost:5173（代理 /api → localhost:8080）
```

### 4.3 数据初始化

E2E 测试执行前，通过以下脚本向 API 写入基础测试数据（确保测试用例的前置条件满足）：

```bash
# 创建基础标签
curl -s -X POST http://localhost:8080/api/tags \
  -H "Content-Type: application/json" \
  -d '{"name":"Java"}' | jq .

# 创建基础分类
curl -s -X POST http://localhost:8080/api/categories \
  -H "Content-Type: application/json" \
  -d '{"name":"技术","parentId":null}' | jq .
```

### 4.4 环境依赖版本

| 组件 | 版本 |
|------|------|
| Java | 17 |
| Maven | 3.8+ |
| Node.js | 18+ |
| Playwright | 1.40+ |
| MySQL（E2E 环境） | 8.0+ |
| H2（单元/集成测试） | 内置 Spring Boot 依赖 |

---

## 5. 质量标准

| 指标 | 目标值 | 验证方式 |
|------|--------|---------|
| 单元测试通过率 | 100% | `mvn test` 无 FAILURE |
| 单元测试行覆盖率（Service 层） | >= 80% | JaCoCo 报告 |
| API 集成测试：端点覆盖率 | 所有端点至少 1 个正向 + 1 个异常用例 | 用例矩阵检查 |
| E2E 测试：P0 场景通过率 | 100% | Playwright 报告 |
| 错误响应不含堆栈 | 必须满足 | 接口测试断言 message 不含 "Exception" / "at com." |
| Flaky 测试 | 0（或明确标记 fixme） | `--repeat-each=3` 检测 |

---

## 6. 缺陷分级

| 级别 | 定义 | 示例 | 处理要求 |
|------|------|------|---------|
| **P0** 阻塞上线 | 核心功能无法使用，数据丢失，安全漏洞 | 创建笔记报 500；删除操作数据未删除；响应体暴露堆栈 | 发现即修，当日修复并回归 |
| **P1** 本版本修复 | 功能不符合需求规格，但有 workaround | 级联删除后 note_tag 未清除；搜索空关键词返回 200 | 本迭代内修复，下次测试循环回归 |
| **P2** 下版本 | 体验问题，或低频边界场景 | 错误 message 描述不够友好；排序参数非法时未返回提示 | 记录 backlog，下版本修复 |

---

## 7. 测试执行流程

```
1. 单元测试（mvn test）
      ↓ 全部通过
2. API 集成测试（MockMvc / curl）
      ↓ 全部通过
3. 启动后端 + 前端服务
4. E2E 测试（Playwright）
      ↓ 全部通过
5. Flaky 检测（--repeat-each=3）
      ↓ 无 flaky 或标记 fixme
6. 覆盖率验证（jacoco:report）
      ↓ Service 层行覆盖 >= 80%
7. 输出测试报告至 testing/reports/
```

---

## 8. 测试报告输出

测试报告存放路径：`/root/my-knowledge/testing/reports/`

报告格式参见 `testing/reports/test-report-YYYYMMDD.md`，包含：
- 各层测试通过/失败数量汇总
- 覆盖率数据
- 失败用例列表（含严重级别）
- 风险评估

结构化结果同步输出至 `workspace/test-result.json`（供闭环流程读取）。
