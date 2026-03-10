# 技术设计 — 个人知识库管理系统

## 1. 系统架构

### 架构图

```
┌──────────────────────────────────────────────────────┐
│                    浏览器 (Chrome/Firefox/Edge)        │
│  ┌────────────────────────────────────────────────┐   │
│  │         Vue 3 + Vite SPA                       │   │
│  │  Pages / Components / Stores / Composables     │   │
│  └──────────────────┬─────────────────────────────┘   │
└─────────────────────┼────────────────────────────────┘
                      │ HTTP (axios)
                      ▼
┌──────────────────────────────────────────────────────┐
│              Nginx（生产环境）                         │
│   静态资源托管 + /api/* 反向代理 → localhost:8080      │
└──────────────────┬───────────────────────────────────┘
                   ▼
┌──────────────────────────────────────────────────────┐
│           Spring Boot 3.x（端口 8080）                │
│  ┌────────────┐  ┌────────────┐  ┌──────────────┐   │
│  │ Controller │→ │  Service   │→ │  Mapper      │   │
│  │ (REST API) │  │ (业务逻辑) │  │ (MyBatis-Plus)│  │
│  └────────────┘  └────────────┘  └──────┬───────┘   │
└─────────────────────────────────────────┼────────────┘
                                          ▼
                                 ┌────────────────┐
                                 │  MySQL 8.0     │
                                 │（端口 3306）    │
                                 │  FULLTEXT+ngram│
                                 └────────────────┘
```

### 模块划分

| 模块 | 职责 |
|------|------|
| note | 笔记 CRUD、全文搜索、分页排序筛选；直接操作 NoteTagMapper 维护标签关联（无跨模块 Service 调用） |
| tag | 标签 CRUD、关联笔记数统计 |
| category | 分类 CRUD、树形查询、级联删除 |
| common | 统一响应封装、全局异常处理、分页工具 |

### 技术栈总览

| 层次 | 技术 | 版本 |
|------|------|------|
| 运行环境 | Java | 17（LTS，Spring Boot 3.x 最低要求） |
| 后端框架 | Spring Boot | 3.2.x |
| ORM | MyBatis-Plus | 3.5.x |
| 数据库 | MySQL | 8.0+ |
| 全文搜索 | MySQL FULLTEXT + ngram | 内置 |
| 前端框架 | Vue 3 (Composition API) | 3.4.x |
| 构建工具 | Vite | 5.x |
| 状态管理 | Pinia | 2.x |
| HTTP 客户端 | axios | 1.x |
| Markdown 渲染 | marked.js | 12.x |
| 代码高亮 | highlight.js | 11.x |
| XSS 防护 | DOMPurify | 3.x |
| UI 组件库 | Element Plus | 2.x |

## 2. 技术选型（Trade-Off 分析）

### 决策 1：ORM — MyBatis-Plus vs JPA

| 维度 | MyBatis-Plus | JPA/Hibernate |
|------|-------------|---------------|
| SQL 控制力 | 高，可写原生 SQL（FULLTEXT 查询需要） | 低，复杂 SQL 需 @NativeQuery |
| 学习曲线 | 低 | 中（懒加载、N+1 问题） |
| 内置分页 | IPage 开箱即用 | 需要 Pageable + 自定义 |
| 代码量 | 少，BaseMapper 提供通用 CRUD | 少，但复杂查询需 Specification |

**结论：** 选 MyBatis-Plus。本项目全文搜索需要 `MATCH AGAINST` 原生 SQL，MyBatis-Plus 的 SQL 控制力更合适，且内置分页插件直接满足列表分页需求。

### 决策 2：全文搜索 — MySQL FULLTEXT vs Elasticsearch

| 维度 | MySQL FULLTEXT | Elasticsearch |
|------|---------------|---------------|
| 部署复杂度 | 零，MySQL 内置 | 高，需独立部署 + 数据同步 |
| 中文支持 | ngram parser（精度一般） | ik 分词器（精度高） |
| 性能（万级数据） | 足够 | 远超需求 |
| 运维成本 | 零 | 高 |

**结论：** 选 MySQL FULLTEXT。单用户系统数据量上限约 1 万篇，MySQL FULLTEXT + ngram 足够。引入 ES 是过度设计。若后期中文搜索精度不满足，可降级为 LIKE 模糊匹配。

### 决策 3：Markdown 渲染 — marked.js vs markdown-it

| 维度 | marked.js | markdown-it |
|------|-----------|-------------|
| 包体积 | ~40KB | ~100KB |
| GFM 支持 | 内置 | 需插件 |
| API 简洁度 | `marked(text)` 一行调用 | 需实例化配置 |
| 插件生态 | 较少 | 丰富 |

**结论：** 选 marked.js。本项目只需标准 Markdown + GFM（表格、代码块），marked.js 体积小、API 简洁，搭配 highlight.js 做代码高亮、DOMPurify 做 XSS 过滤即可满足需求。

### 决策 4：UI 组件库 — Element Plus vs 无组件库

**结论：** 选 Element Plus。提供 el-tree（分类树）、el-table（标签管理）、el-pagination（分页器）、el-dialog（确认框）等开箱即用组件，显著减少开发量。单用户系统不需要极致定制化 UI。

### 决策 5：状态管理 — Pinia vs Vuex

**结论：** 选 Pinia。Vue 3 官方推荐，API 更简洁，无 mutations 概念，TypeScript 支持更好，降低样板代码。

## 3. 数据库设计

### ER 关系

```
Note *---* Tag        （多对多，通过 note_tag）
Note *---1 Category   （多对一，category_id 外键）
Category *---1 Category （自引用，parent_id 实现层级）
```

### 建表 SQL

```sql
-- 分类表
CREATE TABLE category (
    id         BIGINT       AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    name       VARCHAR(100) NOT NULL                  COMMENT '分类名称',
    parent_id  BIGINT       NULL                      COMMENT '父分类ID，NULL为顶级分类',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_parent_id (parent_id),
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES category(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分类表';

-- 标签表
CREATE TABLE tag (
    id         BIGINT      AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    name       VARCHAR(50) NOT NULL UNIQUE            COMMENT '标签名称，全局唯一',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签表';

-- 笔记表
CREATE TABLE note (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    title       VARCHAR(200) NOT NULL                  COMMENT '笔记标题',
    content     LONGTEXT                               COMMENT 'Markdown原文内容',
    summary     VARCHAR(200)                           COMMENT '内容摘要，Markdown原文前100个字符',
    category_id BIGINT       NULL                      COMMENT '所属分类ID，外键',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                    COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_category_id (category_id),
    INDEX idx_updated_at  (updated_at),
    INDEX idx_created_at  (created_at),
    INDEX idx_title       (title),
    FULLTEXT INDEX ft_note_title_content (title, content) WITH PARSER ngram,
    CONSTRAINT fk_note_category FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记表';

-- 笔记-标签关联表
CREATE TABLE note_tag (
    note_id BIGINT NOT NULL COMMENT '笔记ID',
    tag_id  BIGINT NOT NULL COMMENT '标签ID',

    PRIMARY KEY (note_id, tag_id),
    INDEX idx_tag_id (tag_id),
    CONSTRAINT fk_notetag_note FOREIGN KEY (note_id) REFERENCES note(id) ON DELETE CASCADE,
    CONSTRAINT fk_notetag_tag  FOREIGN KEY (tag_id)  REFERENCES tag(id)  ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记-标签关联表';
```

### 索引策略

| 表 | 索引名 | 类型 | 用途 |
|----|--------|------|------|
| note | ft_note_title_content | FULLTEXT (ngram) | 全文搜索 title+content |
| note | idx_category_id | B-Tree | 按分类筛选 |
| note | idx_updated_at | B-Tree | 按更新时间排序 |
| note | idx_created_at | B-Tree | 按创建时间排序 |
| note | idx_title | B-Tree | 按标题字母排序（ORDER BY title） |
| note_tag | idx_tag_id | B-Tree | 按标签查笔记 |
| category | idx_parent_id | B-Tree | 树形查询子分类 |

### MySQL 全文搜索配置

```ini
[mysqld]
ngram_token_size=2
```

`ngram_token_size=2` 即最小分词 2 个字符，支持中文搜索。

## 4. API 设计规格

### 统一响应格式

```json
// 成功（单对象）
{ "code": 200, "message": "success", "data": {} }

// 成功（分页）
{ "code": 200, "message": "success", "data": { "records": [], "total": 100, "page": 1, "size": 20 } }

// 错误
{ "code": 400, "message": "标题不能为空", "data": null }
```

### 错误码表

| code | 含义 |
|------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 404 | 资源不存在 |
| 409 | 资源冲突（如标签名重复） |
| 500 | 服务器内部错误（不暴露堆栈） |

---

### 4.1 笔记 API

#### GET /api/notes — 笔记列表

查询参数：

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| page | int | 1 | 页码 |
| size | int | 20 | 每页条数 |
| sort | string | updatedAt | 排序字段：updatedAt / createdAt / title |
| order | string | desc | 排序方向：asc / desc |
| tagId | long | - | 按标签 ID 筛选（可选） |
| categoryId | long | - | 按分类 ID 筛选（可选） |

响应示例：

```json
{
  "code": 200, "message": "success",
  "data": {
    "records": [{
      "id": 1, "title": "Spring Boot 入门",
      "summary": "Spring Boot 是一个快速开发框架...",
      "categoryId": 2, "categoryName": "后端",
      "tags": [{ "id": 1, "name": "Java" }],
      "createdAt": "2026-03-01T10:00:00",
      "updatedAt": "2026-03-09T14:00:00"
    }],
    "total": 50, "page": 1, "size": 20
  }
}
```

错误：`400` 参数非法。

#### POST /api/notes — 创建笔记

请求体：

```json
{ "title": "Spring Boot 入门", "content": "## 简介\nSpring Boot 是...", "categoryId": 2, "tagIds": [1, 3] }
```

响应：`200`，data 为创建后完整笔记对象（含 id）。

错误：`400` 标题为空；`404` categoryId 不存在。

#### GET /api/notes/search — 全文搜索

> **路由说明：** Spring MVC 静态路径 `/api/notes/search` 优先级高于路径变量 `/api/notes/{id}`，两者不冲突。

查询参数：`q`（必填，最长 200 字符）、`page`（默认 1）、`size`（默认 20）。

响应格式同分页列表，`total` 为全量匹配结果总数，结果按相关度降序排列。

错误：`400` 关键词为空。

#### GET /api/notes/{id} — 笔记详情

响应示例：

```json
{
  "code": 200, "message": "success",
  "data": {
    "id": 1, "title": "Spring Boot 入门",
    "content": "## 简介\nSpring Boot 是...",
    "summary": "Spring Boot 是一个快速开发框架...",
    "categoryId": 2, "categoryName": "后端",
    "tags": [{ "id": 1, "name": "Java" }],
    "createdAt": "2026-03-01T10:00:00",
    "updatedAt": "2026-03-09T14:00:00"
  }
}
```

错误：`404` 笔记不存在。

#### PUT /api/notes/{id} — 更新笔记

请求体同 POST。响应为更新后完整笔记对象。

错误：`400` 标题为空；`404` 笔记不存在。

#### DELETE /api/notes/{id} — 删除笔记

响应：`200`，data 为 null。错误：`404` 笔记不存在。

---

### 4.2 标签 API

#### GET /api/tags — 标签列表（全量，不分页，按 noteCount 降序）

```json
{
  "code": 200, "message": "success",
  "data": [
    { "id": 1, "name": "Java", "noteCount": 15, "createdAt": "2026-03-01T10:00:00" }
  ]
}
```

#### POST /api/tags — 创建标签

请求体：`{ "name": "Docker" }`。响应为创建后标签对象。

错误：`400` 名称为空；`409` 标签名已存在。

#### PUT /api/tags/{id} — 更新标签

请求体：`{ "name": "Docker Compose" }`。

错误：`404` 标签不存在；`409` 新名称已存在。

#### DELETE /api/tags/{id} — 删除标签

级联解除与笔记的关联（note_tag ON DELETE CASCADE）。

错误：`404` 标签不存在。

---

### 4.3 分类 API

#### GET /api/categories — 分类树（全量，树形结构）

```json
{
  "code": 200, "message": "success",
  "data": [{
    "id": 1, "name": "技术", "parentId": null, "noteCount": 5,
    "children": [{ "id": 2, "name": "前端", "parentId": 1, "noteCount": 3, "children": [] }]
  }]
}
```

#### POST /api/categories — 创建分类

请求体：`{ "name": "后端", "parentId": 1 }`（parentId 为 null 则创建顶级分类）。

错误：`400` 名称为空或层级超过 3 级；`404` parentId 不存在。

#### PUT /api/categories/{id} — 更新分类名称

请求体：`{ "name": "后端开发" }`。错误：`404` 分类不存在。

#### DELETE /api/categories/{id} — 删除分类（级联删除子分类）

级联删除所有子分类，相关笔记 category_id 置 NULL（事务保证）。

错误：`404` 分类不存在。

## 5. 后端项目结构

```
knowledge-backend/
├── pom.xml
└── src/main/
    ├── java/com/knowledge/
    │   ├── KnowledgeApplication.java         # 启动类
    │   ├── common/
    │   │   ├── Result.java                   # 统一响应封装 {code, message, data}
    │   │   ├── PageResult.java               # 分页响应 {records, total, page, size}
    │   │   ├── GlobalExceptionHandler.java   # 全局异常 @RestControllerAdvice
    │   │   └── BusinessException.java        # 业务异常类（含 code）
    │   ├── controller/
    │   │   ├── NoteController.java           # 笔记 REST API，@Valid 校验
    │   │   ├── TagController.java            # 标签 REST API
    │   │   └── CategoryController.java       # 分类 REST API
    │   ├── service/
    │   │   ├── NoteService.java              # 笔记业务：CRUD、搜索、摘要生成、标签关联
    │   │   ├── TagService.java               # 标签业务：CRUD、重复校验
    │   │   └── CategoryService.java          # 分类业务：树构建、层级校验、级联删除
    │   ├── mapper/
    │   │   ├── NoteMapper.java               # BaseMapper + 自定义 FULLTEXT 搜索
    │   │   ├── TagMapper.java                # BaseMapper + noteCount 统计查询
    │   │   ├── CategoryMapper.java           # BaseMapper + 子分类递归查询
    │   │   └── NoteTagMapper.java            # 关联表操作
    │   ├── entity/
    │   │   ├── Note.java                     # 笔记实体（@TableName, @TableId）
    │   │   ├── Tag.java                      # 标签实体
    │   │   ├── Category.java                 # 分类实体（含 parentId）
    │   │   └── NoteTag.java                  # 关联实体
    │   └── dto/
    │       ├── NoteDTO.java                  # 笔记响应（含 tags 列表、categoryName）
    │       ├── NoteCreateRequest.java        # 创建请求（@NotBlank title）
    │       ├── NoteUpdateRequest.java        # 更新请求
    │       ├── TagDTO.java                   # 标签响应（含 noteCount）
    │       ├── CategoryDTO.java             # 分类响应（含 children 递归）
    │       └── CategoryCreateRequest.java    # 创建分类请求
    └── resources/
        ├── application.yml                   # 主配置（profile 激活）
        ├── application-dev.yml               # 开发环境：MySQL localhost，debug 日志
        ├── application-prod.yml              # 生产环境：关闭堆栈，info 日志
        └── mapper/NoteMapper.xml             # FULLTEXT 搜索 SQL
```

### 关键类职责

| 类 | 职责 |
|----|------|
| NoteService | 创建时生成 summary（content 前 100 字符）；保存时同步 note_tag 关联；搜索调用 FULLTEXT |
| CategoryService | 全量查询分类列表 → 内存递归构建树；删除时递归收集子分类 ID → 事务内批量删除 + 笔记 category_id 置空 |
| GlobalExceptionHandler | BusinessException → 业务错误码；其他异常 → 500（不暴露堆栈） |

## 6. 前端项目结构

```
knowledge-frontend/
├── index.html
├── package.json
├── vite.config.js                         # 开发代理：/api → localhost:8080
├── .env.development                       # VITE_API_BASE_URL=http://localhost:5173/api
├── .env.production                        # VITE_API_BASE_URL=/api
└── src/
    ├── main.js                            # 入口：app + router + pinia + ElementPlus
    ├── App.vue                            # 根组件：AppHeader + AppSidebar + router-view
    ├── router/index.js                    # 7 条路由定义
    ├── api/
    │   ├── request.js                     # axios 实例（baseURL、响应拦截、统一错误处理）
    │   ├── note.js                        # getNotes / getNote / createNote / updateNote / deleteNote / searchNotes
    │   ├── tag.js                         # getTags / createTag / updateTag / deleteTag
    │   └── category.js                    # getCategories / createCategory / updateCategory / deleteCategory
    ├── stores/
    │   ├── noteStore.js                   # 笔记列表、筛选条件（tagId/categoryId）、排序、分页
    │   ├── tagStore.js                    # 标签列表状态
    │   └── categoryStore.js               # 分类树状态
    ├── pages/
    │   ├── NoteListPage.vue               # 首页
    │   ├── NoteDetailPage.vue             # 详情页
    │   ├── NoteEditPage.vue               # 编辑页（新建/编辑）
    │   ├── TagManagePage.vue              # 标签管理
    │   ├── CategoryManagePage.vue         # 分类管理
    │   └── SearchResultPage.vue           # 搜索结果
    ├── components/
    │   ├── AppHeader.vue                  # 顶部导航（Logo、搜索框、新建按钮）
    │   ├── AppSidebar.vue                 # 侧边栏（CategoryTree + TagCloud）
    │   ├── NoteCard.vue                   # 笔记卡片（标题、摘要、时间、标签、操作按钮）
    │   ├── MarkdownEditor.vue             # textarea 封装，v-model 双向绑定
    │   ├── MarkdownPreview.vue            # marked + hljs + DOMPurify，v-html 展示
    │   ├── TagCloud.vue                   # 按 noteCount 加权字体大小，点击筛选
    │   ├── CategoryTree.vue               # el-tree 封装，点击节点触发筛选
    │   ├── TagSelect.vue                  # 标签多选（el-select，从 tagStore 取数据）
    │   ├── Pagination.vue                 # el-pagination 封装
    │   └── EmptyState.vue                 # 空状态（插图 + 提示文字 + 操作按钮）
    └── composables/
        ├── useMarkdown.js                 # marked 配置（gfm+hljs）+ DOMPurify 过滤
        └── usePagination.js               # 分页状态复用（page/size/total）
```

## 7. 关键技术方案

### 7.1 全文搜索实现

**SQL 模板（NoteMapper.xml）：**

```sql
SELECT n.id, n.title, n.summary, n.updated_at,
       MATCH(n.title, n.content) AGAINST(#{keyword} IN BOOLEAN MODE) AS relevance
FROM note n
WHERE MATCH(n.title, n.content) AGAINST(#{keyword} IN BOOLEAN MODE)
ORDER BY relevance DESC
LIMIT #{offset}, #{size}
```

**关键词高亮（前端）：** 搜索结果返回后，JS 正则匹配关键词，包裹 `<mark>` 标签：

```javascript
function highlight(text, keyword) {
  if (!keyword || !text) return text
  const escaped = keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return text.replace(new RegExp(escaped, 'gi'), '<mark>$&</mark>')
}
```

### 7.2 Markdown 实时预览

**渲染流程：**

```
textarea (v-model: content)
  → watch(content, debounce 300ms)
  → marked(content, { gfm: true, highlight: hljs })
  → DOMPurify.sanitize(html)
  → v-html 渲染到预览区
```

**marked 配置（useMarkdown.js）：**

> marked.js v5+ 已弃用 `setOptions`，使用 `marked.use()` 扩展方式。

```javascript
import { marked } from 'marked'
import { markedHighlight } from 'marked-highlight'
import hljs from 'highlight.js'
import DOMPurify from 'dompurify'

// 使用 marked.use() 注入代码高亮扩展（marked v5+ 规范方式）
marked.use(markedHighlight({
  langPrefix: 'hljs language-',
  highlight(code, lang) {
    const language = hljs.getLanguage(lang) ? lang : 'plaintext'
    return hljs.highlight(code, { language }).value
  }
}))

marked.use({ gfm: true, breaks: true })

export function renderMarkdown(text) {
  return DOMPurify.sanitize(marked(text || ''))
}
```

### 7.3 分类树构建与级联删除

**树构建（CategoryService.java）：**

```java
public List<CategoryDTO> buildTree(List<Category> all) {
    Map<Long, CategoryDTO> map = all.stream()
        .collect(Collectors.toMap(Category::getId, this::toDTO));
    List<CategoryDTO> roots = new ArrayList<>();
    for (CategoryDTO dto : map.values()) {
        if (dto.getParentId() == null) {
            roots.add(dto);
        } else {
            CategoryDTO parent = map.get(dto.getParentId());
            if (parent != null) parent.getChildren().add(dto);
        }
    }
    return roots;
}
```

**级联删除（事务）：**

```
@Transactional
deleteCategory(id):
  1. 递归收集所有子分类 ID（含自身）
  2. UPDATE note SET category_id = NULL WHERE category_id IN (ids)
  3. DELETE FROM category WHERE id IN (ids)
```

### 7.4 摘要冗余存储

笔记保存时后端截取 content 前 100 字符存入 summary 字段，列表接口返回 summary 而不返回完整 content，减少数据传输量：

```java
String summary = content == null ? "" :
    content.length() <= 100 ? content : content.substring(0, 100);
```

## 8. 安全设计

| 威胁 | 防护措施 | 实现层 |
|------|---------|--------|
| XSS | DOMPurify 过滤 Markdown 渲染后 HTML | 前端组件 |
| SQL 注入 | MyBatis-Plus `#{}` 参数化查询 | 后端 Mapper |
| 堆栈泄露 | GlobalExceptionHandler 返回通用错误信息 | 后端 |
| 非法参数 | Controller 层 @Valid + @NotBlank 校验 | 后端 |
| CORS | Spring Boot 配置允许前端开发端口跨域 | 后端 |

> 单用户系统 P0 阶段无认证需求，不设 JWT/Session。

## 9. 需求-设计追溯矩阵

| 用户故事 | 优先级 | 后端 API | 前端页面/组件 |
|---------|--------|---------|--------------|
| US-001 创建笔记 | P0 | POST /api/notes | NoteEditPage（新建模式） |
| US-002 编辑笔记 | P0 | PUT /api/notes/{id} | NoteEditPage（编辑模式） |
| US-003 删除笔记 | P0 | DELETE /api/notes/{id} | NoteListPage / NoteDetailPage（确认框） |
| US-004 笔记列表 | P0 | GET /api/notes | NoteListPage + NoteCard + Pagination |
| US-005 Markdown 预览 | P0 | GET /api/notes/{id} | MarkdownEditor + MarkdownPreview |
| US-006 全文搜索 | P0 | GET /api/notes/search | AppHeader + SearchResultPage |
| US-007 标签管理 | P0 | GET/POST/PUT/DELETE /api/tags | TagManagePage |
| US-008 标签筛选 | P0 | GET /api/notes?tagId= | NoteListPage + TagCloud |
| US-009 分类管理 | P1 | GET/POST/PUT/DELETE /api/categories | CategoryManagePage + CategoryTree |
| US-010 笔记排序 | P1 | GET /api/notes?sort=&order= | NoteListPage（排序下拉框） |
| US-011 标签云 | P1 | GET /api/tags | AppSidebar + TagCloud |

## 10. 风险识别与应对

| 风险 | 概率 | 影响 | 应对策略 |
|------|------|------|---------|
| MySQL FULLTEXT 中文搜索精度低 | 高 | 中 | ngram_token_size=2；不满足时降级为 LIKE 模糊匹配 |
| 大文件 Markdown 渲染卡顿 | 中 | 低 | content 上限 100KB；编辑器防抖 300ms |
| 分类级联删除数据丢失 | 低 | 高 | 删除前确认弹框明确提示影响范围；@Transactional 保证一致性 |
| 前端依赖版本不兼容 | 低 | 中 | 锁定 package.json 版本，提交 lock 文件 |
| ngram 索引空间偏大 | 中 | 低 | 单用户万级数据量下可接受 |
| marked.js API 变更（setOptions 弃用） | 中 | 中 | 使用 marked.use() + marked-highlight 扩展方式，见第 7.2 节示例 |

## 11. 部署方案

### 本地开发环境

```bash
# 数据库
mysql -u root -p < schema.sql      # 初始化表结构

# 后端
cd knowledge-backend
mvn spring-boot:run                 # http://localhost:8080

# 前端
cd knowledge-frontend
npm install && npm run dev          # http://localhost:5173（代理 /api → 8080）
```

Vite 开发代理配置（`vite.config.js`）：

```javascript
export default defineConfig({
  server: {
    port: 5173,
    proxy: { '/api': { target: 'http://localhost:8080', changeOrigin: true } }
  }
})
```

### 生产部署

```
浏览器 ─── :80 ──→ Nginx ─── /api/* ──→ Spring Boot :8080 ──→ MySQL :3306
                      │
                   /  → dist/（Vue 静态文件）
```

```bash
# 后端打包
mvn clean package -DskipTests
java -jar target/knowledge-backend.jar --spring.profiles.active=prod

# 前端打包
npm run build   # 产出 dist/ 目录
```

Nginx 核心配置：

```nginx
server {
    listen 80;
    root /var/www/knowledge/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;  # SPA history 模式支持
    }

    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

环境配置文件说明：

| 文件 | 用途 |
|------|------|
| application-dev.yml | MySQL localhost:3306，debug 日志，CORS 允许 5173 |
| application-prod.yml | MySQL 生产地址，info 日志，关闭堆栈输出 |
| .env.development | VITE_API_BASE_URL=/api（走 Vite proxy） |
| .env.production | VITE_API_BASE_URL=/api（走 Nginx 反代） |

### 日志策略

| 环境 | 日志级别 | 输出目标 | 格式 |
|------|---------|---------|------|
| 开发（dev） | DEBUG | 控制台 | 默认 Spring 格式 |
| 生产（prod） | INFO | 文件（logs/knowledge.log） | JSON 结构化日志 |

生产日志配置（application-prod.yml）：

```yaml
logging:
  level:
    root: INFO
    com.knowledge: INFO
  file:
    name: logs/knowledge.log
    max-size: 100MB
    max-history: 30        # 保留 30 天
  pattern:
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

不记录内部异常堆栈到响应体（由 GlobalExceptionHandler 截断），但完整堆栈写入日志文件供排查。

### 监控策略

本项目为单用户本地部署，采用轻量监控方案：

- **健康检查：** 引入 Spring Boot Actuator，暴露 `/actuator/health` 端点，返回服务状态（UP/DOWN）
- **基础指标：** 暴露 `/actuator/info`（版本信息）和 `/actuator/metrics`（JVM 内存、HTTP 请求计数）
- **Actuator 配置（application-prod.yml）：**

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: never    # 不暴露内部细节
```

- **告警：** 单用户系统无需自动告警，通过日志文件人工排查
- **数据库监控：** 通过 `SHOW STATUS` / `SHOW PROCESSLIST` 手动诊断（单用户无并发压力）

## 12. 架构决策记录（ADR）

### ADR-001: 全文搜索使用 MySQL FULLTEXT + ngram

**背景：** 需要支持中文笔记的标题+内容全文搜索，需选定搜索方案。

**决策：** MySQL 8.0 内置 FULLTEXT 索引，ngram parser，ngram_token_size=2。

**理由：** 单用户系统数据量上限万级，MySQL FULLTEXT 零额外部署成本、无数据同步开销。

**替代方案：** Elasticsearch — 中文搜索精度更高，但需独立部署和数据同步，对单用户系统是过度设计。

**影响：** 中文搜索精度有限，长尾词可能漏匹配。若不满足需求，可降级 LIKE 或迁移 ES。

**状态：** 已采纳，2026-03-09

---

### ADR-002: ORM 使用 MyBatis-Plus

**背景：** 需要 ORM 支持 CRUD 和 FULLTEXT 原生 SQL。

**决策：** MyBatis-Plus 3.5.x。

**理由：** FULLTEXT `MATCH AGAINST` 需要原生 SQL；内置 IPage 分页插件开箱即用；无 N+1 陷阱风险。

**替代方案：** Spring Data JPA — CRUD 简洁，但复杂搜索需 @NativeQuery，JPA 懒加载机制对本项目无收益。

**状态：** 已采纳，2026-03-09

---

### ADR-003: UI 组件库使用 Element Plus

**背景：** 前端需要表格、树、分页器、对话框等通用 UI 组件。

**决策：** Element Plus 2.x。

**理由：** Vue 3 最成熟的组件库，el-tree（分类树）、el-table（标签表格）、el-pagination 开箱即用，显著减少开发量。

**替代方案：** 纯手写组件 — 灵活但开发周期长，MVP 阶段不划算。

**状态：** 已采纳，2026-03-09
