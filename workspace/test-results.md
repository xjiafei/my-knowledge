# 测试执行结果

执行时间：2026-03-10
测试环境：后端 Spring Boot 3.2.x + H2（MySQL兼容模式），前端 Vue3 + Vite
执行方式：curl API 集成测试 + JUnit 5 单元测试

---

## 汇总

| 测试类型 | 用例数 | 通过 | 失败 | 通过率 |
|---------|--------|------|------|--------|
| 单元测试（JUnit 5） | 31 | 31 | 0 | 100% |
| API 集成测试 | 35 | 35 | 0 | 100% |
| **合计** | **66** | **66** | **0** | **100%** |

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
