# 测试用例集 — 个人知识库管理系统

> 版本：1.0 | 日期：2026-03-10 | 作者：QA Agent
>
> 约定：
> - **HOST** = `http://localhost:8080`
> - 所有 curl 命令使用 `-s`（静默）+ `| jq .` 格式化输出；实际执行时可去掉 `| jq .`
> - 前置条件中的 `{noteId}`、`{tagId}`、`{categoryId}` 等占位符表示由前一步创建返回的真实 ID
> - **类型**：Unit = 单元测试；API = API 集成测试；E2E = Playwright 端到端测试
> - **优先级**：P0（必测）/ P1（重要）/ P2（补充）

---

## 一、笔记管理（Note）

| 用例ID | 模块 | 场景描述 | 前置条件 | 操作步骤 | 预期结果 | 类型 | 优先级 |
|--------|------|---------|---------|---------|---------|------|--------|
| TC-N-001 | 笔记管理 | 创建笔记（正向，含标题+Markdown内容+标签+分类） | 已存在标签 tagId=1（"Java"），已存在分类 categoryId=1（"技术"） | `curl -s -X POST HOST/api/notes -H "Content-Type: application/json" -d '{"title":"Spring Boot 入门","content":"## 简介\n这是一篇关于 Spring Boot 的笔记","categoryId":1,"tagIds":[1]}'` | HTTP 200；响应 `code=200`；`data.id` 不为 null；`data.title="Spring Boot 入门"`；`data.summary="## 简介\n这是一篇关于 Spring Boot 的笔记"`（内容 <=100 字符时等于 content）；`data.tags` 含 id=1 的标签；`data.categoryId=1` | API | P0 |
| TC-N-002 | 笔记管理 | 创建笔记（异常：标题为空，期望 400） | 无特殊前置条件 | `curl -s -X POST HOST/api/notes -H "Content-Type: application/json" -d '{"title":"","content":"内容"}'` | HTTP 200（统一响应包装）；响应 `code=400`；`message` 包含"标题"；`data=null` | API | P0 |
| TC-N-003 | 笔记管理 | 创建笔记（边界：标题恰好 200 字符，期望成功） | 无特殊前置条件 | 构造长度为 200 的标题字符串（如 200 个"a"），发送 POST `/api/notes`，body：`{"title":"aaa...a(200个)","content":"内容"}` | HTTP 200；响应 `code=200`；`data.title` 长度为 200 | API | P1 |
| TC-N-004 | 笔记管理 | 查询笔记列表（默认分页 page=1 size=20） | 数据库中至少有 1 条笔记 | `curl -s "HOST/api/notes?page=1&size=20"` | HTTP 200；响应 `code=200`；`data.page=1`；`data.size=20`；`data.records` 为数组；`data.total >= 1`；每条记录含 `id`、`title`、`summary`、`tags`、`updatedAt` 字段 | API | P0 |
| TC-N-005 | 笔记管理 | 查询笔记列表（按标签 tagId 筛选） | 笔记 A 关联 tagId=1，笔记 B 未关联该标签 | `curl -s "HOST/api/notes?tagId=1"` | 响应 `code=200`；`data.records` 只包含关联了 tagId=1 的笔记（笔记 A 在结果中，笔记 B 不在） | API | P0 |
| TC-N-006 | 笔记管理 | 查询笔记列表（按分类 categoryId 筛选） | 笔记 A 属于 categoryId=1，笔记 B 属于 categoryId=2 | `curl -s "HOST/api/notes?categoryId=1"` | 响应 `code=200`；`data.records` 只包含 categoryId=1 的笔记（笔记 A 在结果中，笔记 B 不在） | API | P0 |
| TC-N-007 | 笔记管理 | 查询笔记列表（tagId + categoryId 同时筛选，AND 关系） | 笔记 A：categoryId=1 且关联 tagId=1；笔记 B：categoryId=1 未关联 tagId=1；笔记 C：categoryId=2 且关联 tagId=1 | `curl -s "HOST/api/notes?tagId=1&categoryId=1"` | 响应 `code=200`；`data.records` 只包含笔记 A（同时满足两个条件）；笔记 B 和笔记 C 均不出现 | API | P0 |
| TC-N-008 | 笔记管理 | 查看笔记详情（正向） | 已创建笔记 noteId=1 | `curl -s "HOST/api/notes/1"` | HTTP 200；响应 `code=200`；`data.id=1`；`data.title`、`data.content`、`data.summary`、`data.tags`、`data.createdAt`、`data.updatedAt` 均存在且不为 null | API | P0 |
| TC-N-009 | 笔记管理 | 查看笔记详情（不存在，期望 404） | 数据库中不存在 id=99999 的笔记 | `curl -s "HOST/api/notes/99999"` | 响应 `code=404`；`message` 含"笔记不存在"；`data=null` | API | P0 |
| TC-N-010 | 笔记管理 | 更新笔记（正向） | 已创建笔记 noteId=1，标题为"原标题" | `curl -s -X PUT HOST/api/notes/1 -H "Content-Type: application/json" -d '{"title":"新标题","content":"新内容","tagIds":[]}'` | 响应 `code=200`；`data.title="新标题"`；`data.content="新内容"`；再次 GET `/api/notes/1` 返回新标题和内容 | API | P0 |
| TC-N-011 | 笔记管理 | 删除笔记（正向，删除后 GET 返回 404） | 已创建笔记 noteId=1 | 步骤1：`curl -s -X DELETE HOST/api/notes/1`；步骤2：`curl -s "HOST/api/notes/1"` | 步骤1：响应 `code=200`；步骤2：响应 `code=404`，`message` 含"笔记不存在" | API | P0 |
| TC-N-012 | 笔记管理 | 摘要（summary）取 content 前 100 字符 | 无 | 单元测试调用 `noteService.generateSummary(content)`，content 为长度 150 的字符串 | 返回值为 content 前 100 字符；追加验证：content=null 返回 ""；content="" 返回 ""；content 长度 <=100 时返回原值 | Unit | P0 |
| TC-N-013 | 笔记管理 | 删除笔记后 note_tag 关联行级联清除 | 已创建笔记 noteId=1 并关联 tagId=1，note_tag 表中存在对应行 | 步骤1：`curl -s -X DELETE HOST/api/notes/1`；步骤2：查询 note_tag 表中 note_id=1 的记录 | 步骤1：`code=200`；步骤2：note_tag 中无 note_id=1 的记录（ON DELETE CASCADE 生效） | API | P1 |

---

## 二、搜索（Search）

| 用例ID | 模块 | 场景描述 | 前置条件 | 操作步骤 | 预期结果 | 类型 | 优先级 |
|--------|------|---------|---------|---------|---------|------|--------|
| TC-S-001 | 搜索 | 全文搜索关键词命中 title | 存在笔记，title 含"Spring Boot" | `curl -s "HOST/api/notes/search?q=Spring"` | 响应 `code=200`；`data.records` 非空；title 含"Spring Boot"的笔记出现在结果中 | API | P0 |
| TC-S-002 | 搜索 | 全文搜索关键词命中 content（title 不含关键词） | 存在笔记，title="Java 笔记"，content 含"Kubernetes" | `curl -s "HOST/api/notes/search?q=Kubernetes"` | 响应 `code=200`；title="Java 笔记"的笔记出现在搜索结果中（内容匹配） | API | P0 |
| TC-S-003 | 搜索 | 全文搜索空关键词，期望 400 | 无 | `curl -s "HOST/api/notes/search?q="` | 响应 `code=400`；`message` 含"不能为空"；`data=null` | API | P0 |
| TC-S-004 | 搜索 | 全文搜索关键词超 200 字符，期望 400 | 无 | 构造 201 个字符的关键词 `q` 参数：`curl -s "HOST/api/notes/search?q=<201字符字符串>"` | 响应 `code=400`；`message` 含"不能超过200"；`data=null` | API | P0 |
| TC-S-005 | 搜索 | 全文搜索无结果，返回空列表 | 数据库中无包含"xyznotexist123"的笔记 | `curl -s "HOST/api/notes/search?q=xyznotexist123"` | 响应 `code=200`；`data.records=[]`；`data.total=0` | API | P1 |
| TC-S-006 | 搜索 | 搜索结果分页（page=2&size=1） | 存在至少 2 篇含同一关键词"学习"的笔记 | `curl -s "HOST/api/notes/search?q=学习&page=2&size=1"` | 响应 `code=200`；`data.page=2`；`data.size=1`；`data.records` 长度为 1（第 2 条结果）；`data.total >= 2` | API | P1 |

---

## 三、标签管理（Tag）

| 用例ID | 模块 | 场景描述 | 前置条件 | 操作步骤 | 预期结果 | 类型 | 优先级 |
|--------|------|---------|---------|---------|---------|------|--------|
| TC-T-001 | 标签管理 | 创建标签（正向） | 数据库中不存在名为"Docker"的标签 | `curl -s -X POST HOST/api/tags -H "Content-Type: application/json" -d '{"name":"Docker"}'` | 响应 `code=200`；`data.id` 不为 null；`data.name="Docker"`；`data.noteCount=0` | API | P0 |
| TC-T-002 | 标签管理 | 创建标签（重名，期望 409） | 已存在名为"Java"的标签 | `curl -s -X POST HOST/api/tags -H "Content-Type: application/json" -d '{"name":"Java"}'` | 响应 `code=409`；`message` 含"已存在"；`data=null` | API | P0 |
| TC-T-003 | 标签管理 | 创建标签（名称为空，期望 400） | 无 | `curl -s -X POST HOST/api/tags -H "Content-Type: application/json" -d '{"name":""}'` | 响应 `code=400`；`message` 含"不能为空"或参数校验错误提示；`data=null` | API | P0 |
| TC-T-004 | 标签管理 | 查询标签列表（结果按 noteCount 降序） | 存在标签 A（noteCount=5）和标签 B（noteCount=2） | `curl -s "HOST/api/tags"` | 响应 `code=200`；`data` 为数组；标签 A 排在标签 B 前面（noteCount 降序）；每条含 `id`、`name`、`noteCount`、`createdAt` 字段 | API | P1 |
| TC-T-005 | 标签管理 | 更新标签（正向） | 已存在 tagId=1，名称为"Java" | `curl -s -X PUT HOST/api/tags/1 -H "Content-Type: application/json" -d '{"name":"Java 17"}'` | 响应 `code=200`；`data.name="Java 17"`；再次 GET `/api/tags` 列表中出现"Java 17"，不出现"Java" | API | P0 |
| TC-T-006 | 标签管理 | 删除标签（正向，笔记的该标签关联解除） | 已存在 tagId=1，已有笔记 noteId=1 关联该标签 | 步骤1：`curl -s -X DELETE HOST/api/tags/1`；步骤2：`curl -s "HOST/api/notes/1"` | 步骤1：`code=200`；步骤2：`data.tags` 中不含 id=1 的标签（ON DELETE CASCADE 清除 note_tag）；笔记本身仍存在 | API | P0 |

---

## 四、分类管理（Category）

| 用例ID | 模块 | 场景描述 | 前置条件 | 操作步骤 | 预期结果 | 类型 | 优先级 |
|--------|------|---------|---------|---------|---------|------|--------|
| TC-C-001 | 分类管理 | 创建一级分类（parentId 为 null） | 无特殊前置 | `curl -s -X POST HOST/api/categories -H "Content-Type: application/json" -d '{"name":"技术","parentId":null}'` | 响应 `code=200`；`data.name="技术"`；`data.parentId=null`；`data.id` 不为 null | API | P0 |
| TC-C-002 | 分类管理 | 创建二级分类（指定有效 parentId） | 已存在一级分类 categoryId=1（"技术"） | `curl -s -X POST HOST/api/categories -H "Content-Type: application/json" -d '{"name":"前端","parentId":1}'` | 响应 `code=200`；`data.name="前端"`；`data.parentId=1` | API | P0 |
| TC-C-003 | 分类管理 | 创建三级分类（验证层级允许，depth=3 时 parentId 深度为 2） | 已有一级分类 id=1（"技术"），已有二级分类 id=2（"前端"，parentId=1） | `curl -s -X POST HOST/api/categories -H "Content-Type: application/json" -d '{"name":"Vue","parentId":2}'` | 响应 `code=200`；`data.name="Vue"`；`data.parentId=2`；分类树 GET `/api/categories` 返回三层嵌套结构 | API | P0 |
| TC-C-004 | 分类管理 | 创建四级分类（超出层级，期望 400） | 已有三级分类 id=3（"Vue"，parentId=2，depth=3） | `curl -s -X POST HOST/api/categories -H "Content-Type: application/json" -d '{"name":"Vue3组件","parentId":3}'` | 响应 `code=400`；`message` 含"不能超过3级"；`data=null` | API | P0 |
| TC-C-005 | 分类管理 | 查询分类树（返回层级结构） | 已有三级分类树：技术 → 前端 → Vue | `curl -s "HOST/api/categories"` | 响应 `code=200`；`data` 为数组；根节点"技术"的 `children` 含"前端"；"前端"的 `children` 含"Vue"；每个节点含 `id`、`name`、`parentId`、`noteCount`、`children` 字段 | API | P0 |
| TC-C-006 | 分类管理 | 更新分类名称（正向） | 已存在 categoryId=1，名称为"技术" | `curl -s -X PUT HOST/api/categories/1 -H "Content-Type: application/json" -d '{"name":"技术栈"}'` | 响应 `code=200`；`data.name="技术栈"`；GET `/api/categories` 中该节点名称为"技术栈" | API | P0 |
| TC-C-007 | 分类管理 | 删除父分类（有子分类，验证子分类 parentId 置 NULL） | 一级分类 id=1（"技术"），二级分类 id=2（"前端"，parentId=1），三级分类 id=3（"Vue"，parentId=2） | 步骤1：`curl -s -X DELETE HOST/api/categories/1`；步骤2：`curl -s "HOST/api/categories"` | 步骤1：`code=200`；步骤2：分类列表中不含 id=1、id=2、id=3（所有子孙分类均被级联删除） | API | P0 |
| TC-C-008 | 分类管理 | 删除分类（有关联笔记，笔记 category_id 置 NULL） | 已有分类 id=1，笔记 noteId=1 的 categoryId=1 | 步骤1：`curl -s -X DELETE HOST/api/categories/1`；步骤2：`curl -s "HOST/api/notes/1"` | 步骤1：`code=200`；步骤2：`data.categoryId=null`；笔记本身仍存在，其他字段不变 | API | P0 |

---

## 五、非功能（NFR）

| 用例ID | 模块 | 场景描述 | 前置条件 | 操作步骤 | 预期结果 | 类型 | 优先级 |
|--------|------|---------|---------|---------|---------|------|--------|
| TC-NFR-001 | 通用 | 列表 API 响应格式校验（含 code/message/data/total/page/size） | 数据库中至少有 1 条笔记 | `curl -s "HOST/api/notes" \| jq 'keys'` 以及 `jq '.data \| keys'` | 顶层 key 包含 `code`、`message`、`data`；`data` 中 key 包含 `records`、`total`、`page`、`size`；`code=200`；`message="success"` | API | P0 |
| TC-NFR-002 | 笔记管理 | 笔记列表默认按 updatedAt 降序排列 | 存在笔记 A（较早更新）和笔记 B（较晚更新） | `curl -s "HOST/api/notes"` | `data.records[0]` 为笔记 B（updatedAt 更晚）；`data.records[1]` 为笔记 A；即默认降序排列 | API | P1 |
| TC-NFR-003 | 标签管理 | 标签 noteCount 统计正确性 | 已有标签 tagId=1，已有 3 篇笔记均��联该标签 | `curl -s "HOST/api/tags" \| jq '.data[] \| select(.id==1)'` | 返回标签对象中 `noteCount=3` | API | P1 |
| TC-NFR-004 | 安全 | 错误响应不暴露内部堆栈信息 | 无 | `curl -s "HOST/api/notes/99999"` | 响应 `code=404`；`message` 字段不含 "Exception"、"at com."、"StackTrace" 等堆栈关键词；`data=null` | API | P0 |
| TC-NFR-005 | 安全 | 服务内部错误返回 500 通用信息（不暴露堆栈） | 模拟数据库不可达（unit test mock 抛 RuntimeException） | 单元测试：mock NoteMapper.selectNoteDetail 抛 RuntimeException，调用 GlobalExceptionHandler | 响应 `code=500`；`message` 为通用错误信息（如"服务器内部错误"）；不含堆栈信息 | Unit | P0 |

---

## 六、E2E 测试用例（Playwright）

| 用例ID | 模块 | 场景描述 | 前置条件 | 操作步骤 | 预期结果 | 类型 | 优先级 |
|--------|------|---------|---------|---------|---------|------|--------|
| TC-E2E-001 | 笔记管理 | 新建笔记完整流程（E2E） | 后端和前端服务均已启动；数据库中已有标签"Java"和分类"技术" | 1. 访问 `http://localhost:5173`；2. 点击"新建笔记"按钮；3. 填写标题"E2E测试笔记"；4. 在 Markdown 编辑器中输入 `## 标题\n内容`；5. 选择标签"Java"；6. 选择分类"技术"；7. 点击"保存" | 跳转到笔记详情页；页面标题为"E2E测试笔记"；Markdown 内容渲染为 HTML（`<h2>标题</h2>`）；Toast 提示"保存成功" | E2E | P0 |
| TC-E2E-002 | 搜索 | 全文搜索流程（E2E） | 已有笔记，title 含"Spring Boot" | 1. 在顶部搜索框输入"Spring"；2. 按回车；3. 等待搜索结果页加载；4. 点击第一条结果 | 跳转到搜索结果页；结果列表非空；标题中"Spring"被 `<mark>` 高亮；点击后跳转到笔记详情页 | E2E | P0 |
| TC-E2E-003 | 标签筛选 | 标签筛选 + 清除筛选流程（E2E） | 笔记列表页有多条笔记；侧边栏有标签"Java"且至少 1 条笔记关联该标签 | 1. 访问首页；2. 点击侧边栏"Java"标签；3. 验证列表刷新；4. 点击"清除筛选"按钮 | 步骤3：列表仅显示含"Java"标签的笔记，顶部显示筛选标识；步骤4：列表恢复全部笔记，筛选标识消失 | E2E | P0 |
| TC-E2E-004 | 笔记管理 | 删除笔记确认流程（E2E） | 已有至少 1 条笔记 | 1. 访问首页；2. 点击某笔记卡片的"删除"图标；3. 确认对话框弹出；4. 点击"确认删除" | 对话框出现；确认后笔记从列表中消失；Toast 提示删除成功（或列表数量减少 1） | E2E | P0 |
| TC-E2E-005 | 分类管理 | 创建三级分类树并在笔记编辑页选择（E2E） | 分类管理页为空 | 1. 访问 `/categories`；2. 创建一级分类"技术"；3. 以"技术"为父分类创建"前端"；4. 以"前端"为父分类创建"Vue"；5. 访问 `/notes/new`；6. 在分类下拉中选择"Vue" | 分类管理页树形结构正确展示三级；新建笔记页分类选择中可选到"Vue"；保存后笔记详情页显示分类"Vue" | E2E | P1 |

---

## 七、用例覆盖矩阵

| API 端点 | 覆盖的用例 |
|---------|---------|
| POST /api/notes | TC-N-001, TC-N-002, TC-N-003 |
| GET /api/notes | TC-N-004, TC-N-005, TC-N-006, TC-N-007, TC-NFR-001, TC-NFR-002 |
| GET /api/notes/{id} | TC-N-008, TC-N-009 |
| PUT /api/notes/{id} | TC-N-010 |
| DELETE /api/notes/{id} | TC-N-011, TC-N-013 |
| GET /api/notes/search | TC-S-001, TC-S-002, TC-S-003, TC-S-004, TC-S-005, TC-S-006 |
| GET /api/tags | TC-T-004, TC-NFR-003 |
| POST /api/tags | TC-T-001, TC-T-002, TC-T-003 |
| PUT /api/tags/{id} | TC-T-005 |
| DELETE /api/tags/{id} | TC-T-006 |
| GET /api/categories | TC-C-005 |
| POST /api/categories | TC-C-001, TC-C-002, TC-C-003, TC-C-004 |
| PUT /api/categories/{id} | TC-C-006 |
| DELETE /api/categories/{id} | TC-C-007, TC-C-008 |
| E2E 流程 | TC-E2E-001 ~ TC-E2E-005 |
| Unit（Service 层） | TC-N-012, TC-NFR-005 |

---

## 八、边界用例覆盖确认

| 边界类型 | 覆盖用例 |
|---------|---------|
| Null/Undefined | TC-N-012（content=null），TC-C-001（parentId=null） |
| 空值 | TC-N-002（title=""），TC-T-003（name=""），TC-S-003（q=""） |
| 非法类型 | TC-N-004（page/size 参数由 @Min/@Max 校验） |
| 边界值 | TC-N-003（title=200字符），TC-S-004（q=201字符），TC-C-003（depth=3），TC-C-004（depth超限） |
| 错误路径 | TC-N-009（404 不存在），TC-T-002（409 重名），TC-NFR-004（错误不暴露堆栈） |
| 并发 | 单用户系统无并发场景，不设专项并发用例 |
| 大数据量 | TC-S-006（分页验证），TC-N-004（size=20 边界） |
| 特殊字符 | TC-N-001（Markdown 特殊字符 `##`、`\n`） |
