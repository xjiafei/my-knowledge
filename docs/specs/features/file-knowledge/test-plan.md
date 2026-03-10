# 测试方案 — 文件类型知识（增量特性）

> 版本：1.0 | 日期：2026-03-10 | 作者：QA Agent

---

## 1. 测试目标

本测试方案针对"文件类型知识管理"增量特性，验证以下质量目标：

| 目标 | 验证内容 |
|------|---------|
| 文件上传正确性 | 支持格式校验、大小限制、UUID 重命名存储、元数据入库 |
| 文件列表接口契约 | 分页、类型筛选、标签筛选、关键词搜索响应格式正确 |
| 文件下载行为 | 流式传输，Content-Disposition 还原原始文件名，不存在时返回 404 |
| 文件删除一致性 | 数据库记录与磁盘文件同步删除，file_tag 关联级联清除 |
| 标签集成 | 文件打标签、按标签筛选文件；删除标签后 file_tag 关联自动清除 |
| 安全边界 | 路径穿越防护（存储使用 UUID）、MIME + 扩展名双重校验、恶意文件名处理 |
| 错误处理 | 非法格式/超大/空文件返回 400；不存在资源返回 404；磁盘不足返回 507 |
| E2E 用户旅程 | 上传 → 列表查看 → 筛选/搜索 → 下载 → 删除的完整流程可端到端完成 |

**不在测试目标内：**
- 文件内容预览（P2，本期不实现）
- 文件重命名（P2，本期不实现）
- 批量上传、批量删除（本期不实现）
- 文件内容全文搜索（本期不实现）
- 现有笔记/标签/分类功能的全量回归（由原有测试用例集覆盖，本测试方案只验证增量交叉点）
- 性能压测（单用户系统）
- 多浏览器兼容性（仅验证 Chromium）

---

## 2. 测试策略（三层）

### 2.1 单元测试

**范围：** `FileService` 全部 public 方法，重点覆盖：
- 文件格式校验逻辑（扩展名 + MIME 类型双重校验）
- 文件大小校验（>100MB 抛异常）
- 存储路径生成（`{year}/{month}/{uuid}.{ext}` 格式，含特殊字符文件名处理）
- 文件名安全处理（路径穿越字符剥除，如 `../`、`..\\`）
- 文件描述长度校验（>500 字符抛异常）
- 标签数量限制校验（>20 个抛异常）

**覆盖目标：** `FileService` 行覆盖率 >= 80%

**工具：** JUnit 5 + Mockito（mock FileRepository / TagRepository，不依赖真实数据库和磁盘）

**执行方式：**
```bash
cd /root/my-knowledge/knowledge-backend
mvn test -Dtest=FileServiceTest
mvn test jacoco:report   # 报告输出至 target/site/jacoco/
```

---

### 2.2 集成测试

**范围：** Controller + Service + Mapper 全栈，覆盖所有文件相关 API 端点的正向和异常路径。

**API 端点清单：**

| 端点 | 方法 | 说明 |
|------|------|------|
| `POST /api/files` | multipart/form-data | 上传文件（含描述、标签） |
| `GET /api/files` | query params | 文件列表（分页、类型筛选、标签筛选、关键词搜索） |
| `GET /api/files/{id}/download` | — | 文件下载（流式传输） |
| `DELETE /api/files/{id}` | — | 删除文件（元数据 + 磁盘文件） |
| `POST /api/files/{id}/tags` | JSON | 为文件添加标签 |
| `DELETE /api/files/{id}/tags/{tagId}` | — | 删除文件的某个标签关联 |

**工具：** Spring Boot Test（`@SpringBootTest + MockMvc`），文件上传使用 `MockMultipartFile`。

**环境：** H2 内存数据库（`spring.profiles.active=test`），磁盘文件操作使用临时目录（`@TempDir`）。

**H2 测试 profile 配置说明（`application-test.yml` 增量配置）：**

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
file:
  upload:
    dir: ${java.io.tmpdir}/knowledge-test-uploads
    max-size: 104857600   # 100MB
```

**测试数据准备：**
- 每个测试类的 `@BeforeEach` 清理 uploads 临时目录
- 通过 `MockMultipartFile` 构造测试文件（内容为随机字节，文件名和 Content-Type 按用例设置）
- 预置标签数据通过 `@Sql` 脚本或 `tagRepository.save()` 注入

**验证重点：**
- 响应结构：统一 `{ code, message, data }` 格式；列表响应含 `{ records, total, page, size }`
- HTTP 状态码（400/404/507 等）与响应体 code 字段保持语义一致
- `message` 字段不含堆栈信息（不含 `Exception`、`at com.`）
- 文件上传后磁盘路径可寻（`{upload-dir}/{year}/{month}/{uuid}.{ext}` 存在）
- 文件删除后磁盘文件已被清除

**执行方式：**
```bash
cd /root/my-knowledge/knowledge-backend
mvn test -Dtest=FileControllerTest
```

---

### 2.3 E2E 测试（Playwright）

**范围：** 覆盖以下 P0/P1 用户旅程。

| 旅程 | 关键路径 |
|------|---------|
| 上传文件 | 文件管理页 → 点击"上传文件" → 选择文件 → 填写描述/标签 → 点击"上传文件" → 列表刷新显示新文件 |
| 筛选和搜索 | 切换类型 Tab → 列表更新；输入搜索关键词 → 防抖过滤；选择标签筛选 → 筛选生效 |
| 下载文件 | 点击下载图标 → 浏览器触发下载（验证无 404 Toast） |
| 删除文件 | 点击删除图标 → 确认对话框 → 确认删除 → 文件从列表消失 |
| 空态展示 | 无文件时显示空状态提示 |

**工具：** Playwright（TypeScript），采用 Page Object 模式。

**E2E 测试目录结构（增量）：**
```
testing/e2e/
├── pages/
│   └── FileListPage.ts          # 文件管理页 Page Object（新增）
└── tests/
    └── file.spec.ts             # 文件管理 E2E 用例（新增）
```

**选择器策略（优先级）：**
1. `data-testid`（首选，不受 UI 重构影响）
2. `getByRole` + name（语义化，次选）
3. `getByPlaceholder` / `getByText`（慎用于文本易变场景）
4. 禁止使用 CSS 类选择器和 XPath

**等待策略：**
```typescript
// 等待 API 响应而非硬等待
await page.waitForResponse(resp =>
    resp.url().includes('/api/files') && resp.status() === 200
);
// 等待元素可见
await page.locator('[data-testid="file-table"]').waitFor({ state: 'visible' });
// 禁止硬等待
// await page.waitForTimeout(3000);  ← 禁止
```

**环境要求：**
- 后端服务运行于 `localhost:8080`，使用独立测试数据库（E2E 执行前通过 API 初始化干净数据）
- 前端开发服务运行于 `localhost:5173`（代理 `/api` → `localhost:8080`）
- `uploads/` 目录可写，E2E 测试前清空
- 浏览器：Chromium（Playwright 内置）

**执行方式：**
```bash
cd /root/my-knowledge
npx playwright test tests/file.spec.ts          # 单文件运行
npx playwright test tests/file.spec.ts --headed # 可视化运行
npx playwright test --repeat-each=3            # Flaky 检测
npx playwright show-report                     # 查看 HTML 报告
```

---

## 3. 测试环境要求

### 3.1 软件依赖版本

| 组件 | 版本 |
|------|------|
| Java | 17 |
| Maven | 3.8+ |
| Node.js | 18+ |
| Playwright | 1.40+ |
| MySQL（E2E 环境） | 8.0+ |
| H2（集成测试） | 内置 Spring Boot 依赖 |

### 3.2 基础设施前置条件

| 条件 | 说明 |
|------|------|
| 数据库已初始化 | 执行 DDL 脚本，`knowledge_file` 表、`file_tag` 表已创建 |
| `uploads/` 目录可写 | 运行后端服务的进程对该目录有读写权限 |
| 磁盘可用空间 | E2E 测试至少保留 500MB 用于上传测试文件 |
| 后端服务已启动（E2E） | `localhost:8080` 可达，`/api/files` 端点就绪 |
| 前端服务已启动（E2E） | `localhost:5173` 可达，代理配置正确 |

### 3.3 测试数据准备策略

**单元测试：** 全部通过 Mock 注入，无需真实文件和数据库。

**集成测试：**
- 使用 `MockMultipartFile` 构造各类型测试文件（PDF、DOCX、PNG、TXT 等）
- 文件内容为最小有效字节（可为全零字节，仅验证接口行为而非文件内容）
- 临时目录通过 JUnit 5 `@TempDir` 管理，测试结束自动清理
- 数据库状态通过 `@Transactional` + 回滚保证隔离性

**E2E 测试：**
- 准备若干真实测试文件（`testing/fixtures/`）：
  - `sample.pdf`（< 1MB）
  - `sample.docx`（< 1MB）
  - `sample.png`（< 1MB）
  - `large-file.bin`（101MB，用于超大文件测试）
  - `empty.txt`（0 字节，用于空文件测试）
  - `malicious.exe.pdf`（扩展名伪装测试用）
- `global-setup.ts` 中通过 API 预置测试标签（"技术"、"设计"）
- `global-teardown.ts` 中清理通过 API 创建的所有文件记录和磁盘文件

**清理策略：**
- 集成测试：每个测试方法通过 `@AfterEach` 删除上传的临时文件和数据库记录
- E2E 测试：每个 `test` 块在 `afterEach` 中调用 DELETE API 清理本次测试上传的文件

---

## 4. 测试工具选型

| 工具 | 用途 | 版本 |
|------|------|------|
| JUnit 5 | 后端单元测试框架 | 5.x（Spring Boot 内置） |
| Mockito | Mock 依赖（FileRepository、TagRepository、文件系统操作） | 5.x（Spring Boot 内置） |
| Spring Boot Test + MockMvc | API 集成测试，支持 multipart 上传 | 3.2.x |
| JaCoCo | 后端代码覆盖率统计 | Maven 插件 |
| H2 Database | 集成测试内存数据库（替代 MySQL） | 2.x |
| Playwright (TypeScript) | E2E UI 自动化，含文件下载验证 | 1.40+ |

---

## 5. 质量标准（Pass/Fail 标准）

| 指标 | 目标值 | 验证方式 |
|------|--------|---------|
| 单元测试通过率 | 100% | `mvn test` 无 FAILURE |
| `FileService` 行覆盖率 | >= 80% | JaCoCo 报告 |
| API 集成测试：端点覆盖 | 所有端点至少 1 个正向 + 1 个异常用例 | 用例矩阵检查 |
| E2E 测试：P0 场景通过率 | 100% | Playwright 报告 |
| Flaky 测试 | 0（或明确标记 `test.fixme`） | `--repeat-each=3` 检测 |
| 安全边界：路径穿越防护 | 必须满足 | 集成测试断言存储路径不含 `../` |
| 错误响应不含堆栈 | 必须满足 | 断言 `message` 不含 `Exception`/`at com.` |
| 文件删除后磁盘文件不可达 | 必须满足 | 集成测试删除后验证文件路径不存在 |

---

## 6. 缺陷分级

| 级别 | 定义 | 文件功能示例 | 处理要求 |
|------|------|------------|---------|
| **P0** 阻塞上线 | 核心功能失效、数据丢失、安全漏洞 | 上传成功但磁盘文件未写入；删除后数据库记录残留；路径穿越攻击可行；MIME 伪装未被拦截 | 发现即修，当日修复并回归 |
| **P1** 本版本修复 | 功能不符合需求，但有 workaround | 同名文件错误覆盖原文件；删除标签后 file_tag 未级联清除；分页 total 计算有误 | 本迭代内修复 |
| **P2** 下版本 | 体验问题，低频边界 | 文件大小显示精度不准确；错误 message 描述不够友好 | 记录 backlog |

---

## 7. 与现有测试方案的关系

| 现有测试方案 | 关系 | 说明 |
|------------|------|------|
| `docs/specs/test-plan.md`（基础测试方案） | 独立补充 | 本方案只覆盖文件功能增量；笔记/标签/分类的回归由原测试方案负责 |
| TC-T-006（删除标签解除 note_tag 关联） | 扩展 | 删除标签时同样需要级联清除 file_tag；在本方案 TC-F-030 中验证 |
| TC-E2E-001 ~ 005（现有 E2E 旅程） | 不影响 | 文件管理页为独立路由 `/files`，不改动现有 E2E 流程 |

---

## 8. 测试执行流程

```
1. 单元测试（mvn test -Dtest=FileServiceTest）
      ↓ 全部通过
2. API 集成测试（mvn test -Dtest=FileControllerTest）
      ↓ 全部通过
3. 覆盖率验证（mvn jacoco:report）
      ↓ FileService 行覆盖 >= 80%
4. 启动后端 + 前端服务
5. E2E 测试（npx playwright test tests/file.spec.ts）
      ↓ 全部通过
6. Flaky 检测（npx playwright test tests/file.spec.ts --repeat-each=3）
      ↓ 无 flaky 或标记 fixme
7. 输出测试报告至 testing/reports/test-report-file-YYYYMMDD.md
8. 输出结构化结果至 workspace/test-result.json
```

---

## 9. 测试报告输出

- 测试报告路径：`/root/my-knowledge/testing/reports/`
- 报告文件命名：`test-report-file-YYYYMMDD.md`
- 结构化结果：`workspace/test-result.json`（供闭环流程读取）

报告包含：各层测试通过/失败数量、覆盖率数据、失败用例列表（含严重级别）、风险评估。
