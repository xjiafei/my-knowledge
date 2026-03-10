# 测试执行结果

执行时间：2026-03-10
测试环境：后端 Spring Boot 3.2.x + H2（MySQL兼容模式），前端 Vue3 + Vite
执行方式：curl API 集成测试 + JUnit 5 单元测试 + Playwright E2E

---

## 汇总

| 测试类型 | 用例数 | 通过 | 失败 | 通过率 |
|---------|--------|------|------|--------|
| 单元测试（JUnit 5） | 31 | 31 | 0 | 100% |
| API 集成测试 | 35 | 35 | 0 | 100% |
| E2E 测试（Playwright） | 9 | 9 | 0 | 100% |
| **合计** | **75** | **75** | **0** | **100%** |

> TC-S-004（搜索关键词超200字符应返回400）在初测时返回500，原因：GlobalExceptionHandler 缺少 ConstraintViolationException 处理器。已修复并重测通过。

---

## 单元测试明细（JUnit 5 + Mockito）

| 测试类 | 用例数 | 结果 |
|--------|--------|------|
| NoteServiceTest | 14 | PASS |
| CategoryServiceTest | 9 | PASS |
| TagServiceTest | 8 | PASS |
| **合计** | **31** | **全部通过** |

---

## API 集成测试明细

### 笔记管理（TC-N）

| 用例ID | 场景描述 | 结果 | 备注 |
|--------|---------|------|------|
| TC-N-001 | 创建笔记（正向，含Markdown内容） | PASS | code=200 |
| TC-N-002 | 创建笔记（标题为空，期望400） | PASS | code=400 |
| TC-N-003 | 创建笔记（标题200字符上限） | PASS | code=200 |
| TC-N-004 | 查询笔记列表（默认分页） | PASS | code=200 |
| TC-N-004b | 分页响应字段完整（records/total/page/size） | PASS | 字段齐全 |
| TC-N-005 | 按tagId筛选列表 | PASS | total>0 |
| TC-N-006 | 按categoryId筛选列表 | PASS | total>0 |
| TC-N-007 | 标签+分类同时筛选（AND关系） | PASS | 交集正确 |
| TC-N-008 | 查看笔记详情（正向） | PASS | code=200 |
| TC-N-009 | 查看笔记详情（不存在，期望404） | PASS | code=404 |
| TC-N-010 | 更新笔记（正向） | PASS | code=200 |
| TC-N-011 | 删除笔记后GET返回404 | PASS | code=404 |
| TC-N-012 | 笔记摘要取content前100字符 | PASS | len=100 |
| TC-N-013 | 删笔记后note_tag级联清除 | PASS | noteCount正确减少 |

### 搜索（TC-S）

| 用例ID | 场景描述 | 结果 | 备注 |
|--------|---------|------|------|
| TC-S-001 | 搜索命中title（keyword=Spring） | PASS | code=200, total>0 |
| TC-S-001b | 搜索结果不为空 | PASS | total=1 |
| TC-S-002 | 搜索命中content（keyword=Composition） | PASS | total>0 |
| TC-S-003 | 空关键词（期望400） | PASS | code=400 |
| TC-S-004 | 超200字符关键词（期望400）| PASS | code=400（修复后） |
| TC-S-005 | 无结果（返回空列表） | PASS | code=200, total=0 |
| TC-S-005b | 搜索无结果total=0 | PASS | total=0 |
| TC-S-006 | 搜索结果含分页字段（page/size） | PASS | 字段齐全 |

### 标签管理（TC-T）

| 用例ID | 场景描述 | 结果 | 备注 |
|--------|---------|------|------|
| TC-T-001 | 创建标签（正向） | PASS | code=200 |
| TC-T-002 | 创建标签（重名，期望409） | PASS | code=409 |
| TC-T-003 | 创建标签（名称为空，期望400） | PASS | code=400 |
| TC-T-004 | 标签列表按noteCount降序 | PASS | 顺序正确 |
| TC-T-005 | 更新标签（正向） | PASS | code=200 |
| TC-T-006 | 删除标签后笔记解除关联 | PASS | tags=[] |

### 分类管理（TC-C）

| 用例ID | 场景描述 | 结果 | 备注 |
|--------|---------|------|------|
| TC-C-001 | 创建一级分类 | PASS | code=200 |
| TC-C-002 | 创建二级分类（指定parentId） | PASS | code=200 |
| TC-C-003 | 创建三级分类（期望允许） | PASS | code=200 |
| TC-C-004 | 创建四级分类（超出层级，期望400） | PASS | code=400 |
| TC-C-005 | 查询分类树（返回层级结构） | PASS | code=200 |
| TC-C-005b | 分类树含children字段 | PASS | 字段存在 |
| TC-C-006 | 更新分类名称（正向） | PASS | code=200 |
| TC-C-007 | 删除父分类后子分类parentId置NULL | PASS | parentId=null |
| TC-C-008 | 删除分类后笔记categoryId置NULL | PASS | categoryId=null |

### 非功能（TC-NFR）

| 用例ID | 场景描述 | 结果 | 备注 |
|--------|---------|------|------|
| TC-NFR-001 | 响应格式含code/message/data字段 | PASS | 字段齐全 |
| TC-NFR-002 | 笔记列表默认updatedAt降序 | PASS | 顺序正确 |
| TC-NFR-003 | 标签均含noteCount字段 | PASS | 字段存在 |

---

---

## E2E 测试明细（Playwright + Chromium）

| 用例ID | 场景描述 | 结果 | 耗时 |
|--------|---------|------|------|
| TC-001 | 首页加载 - 验证页面标题和基础元素 | PASS | 0.9s |
| TC-001b | 首页 - 页面主体区域可见（笔记列表或空状态） | PASS | 2.8s |
| TC-002 | 创建笔记 - 新建、填写内容、保存 | PASS | 1.1s |
| TC-003 | 笔记列表 - 创建后在首页显示 | PASS | 2.9s |
| TC-004 | 搜索功能 - 输入关键词搜索并验证结果 | PASS | 2.4s |
| TC-004b | 搜索功能 - 使用已知关键词"Test"搜索有结果 | PASS | 2.8s |
| TC-005 | 标签管理 - 创建新标签并验证显示 | PASS | 3.0s |
| TC-006 | 分类管理 - 创建新分类并验证树形显示 | PASS | 2.9s |
| TC-007 | 删除笔记 - 确认删除并验证从列表消失 | PASS | 4.6s |

总耗时：24.4s（9 tests using 1 worker）

### E2E 测试修复记录（初轮失败7个，修复后全部通过）

| 问题 | 原因 | 修复 |
|------|------|------|
| TC-001/002 严格模式 | `getByRole('button',{name:'新建笔记'})` 匹配 header + main 两处按钮 | 缩小到 `.app-header` 范围 |
| TC-005/006/007 严格模式 | `.el-message--success, [class*="el-message"]` 匹配消息根节点及其子元素 | 改为 `.el-message--success` |
| TC-003/004b 无数据 | H2 内存库重启后数据清空 | 添加 `beforeAll` 预建种子笔记 |
| TC-006 严格模式 | `.tree-panel, .el-tree` 匹配包装容器和树组件两个元素 | 改为 `.el-tree` |

---

## 缺陷记录

| 缺陷ID | 用例 | 描述 | 状态 |
|--------|------|------|------|
| BUG-001 | TC-S-004 | GlobalExceptionHandler 缺少 ConstraintViolationException 处理，导致 @Validated @RequestParam 校验失败返回 500 | 已修复 |

---

## 构建验证

| 项目 | 命令 | 结果 |
|------|------|------|
| 后端编译 | mvn compile | BUILD SUCCESS |
| 后端测试 | mvn test | 31/31 PASS |
| 前端构建 | npm run build | BUILD SUCCESS（0 errors） |
