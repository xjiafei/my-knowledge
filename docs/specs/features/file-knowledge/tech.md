# 技术设计 — 文件类型知识（增量特性）

## 1. 架构概览

本特性在现有 Spring Boot + Vue 3 + MySQL 系统上增量扩展，不修改任何现有模块的核心逻辑。

### 新增模块关系

```
knowledge-frontend/
  pages/FileListPage.vue         ← 新增页面
  components/FileUploadDialog.vue ← 新增组件
  api/file.js                    ← 新增 API 模块
  stores/fileStore.js            ← 新增 Store

   HTTP multipart/form-data ↕    HTTP GET/DELETE ↕

knowledge-backend/
  controller/FileController      ← 新增
  service/FileService            ← 新增（业务逻辑 + 安全校验）
  service/FileStorageService     ← 新增（文件 I/O）
  mapper/KnowledgeFileMapper     ← 新增
  mapper/FileTagMapper           ← 新增
  entity/KnowledgeFile           ← 新增
  entity/FileTag                 ← 新增
  dto/FileDTO / FileUploadRequest ← 新增

      MyBatis-Plus ↕

MySQL（knowledge_db）
  knowledge_file                 ← 新增表
  file_tag                       ← 新增表
  tag                            ← 复用（不修改）

Disk（服务器本地）
  uploads/{yyyy}/{MM}/{uuid}.ext ← 新增存储目录
```

### 集成点（与现有系统）

| 现有组件 | 集成方式 | 影响 |
|---------|---------|------|
| `tag` 表 | 只读引用（`file_tag.tag_id` 外键指向 `tag.id`） | 不修改 tag 表和 TagService |
| `TagService.deleteTag()` | 无需修改：`file_tag` 配置 `ON DELETE CASCADE` | 自动级联清除 |
| `TagMapper` / `TagService` | 上传时复用 `/api/tags` 获取标签列表 | 无代码修改 |
| `AppSidebar.vue` | 追加 1 个 `<router-link>` | 改动 ≤ 3 行 |
| `router/index.js` | 追加 1 条路由定义 | 改动 1 行 |
| `GlobalExceptionHandler` | 新增 507 错误处理（复用 `BusinessException` 机制） | 追加 1 个 catch 分支 |

---

## 2. 数据库设计

### 2.1 新增表 DDL

```sql
-- 文件元数据表
CREATE TABLE IF NOT EXISTS knowledge_file (
    id            BIGINT        AUTO_INCREMENT PRIMARY KEY              COMMENT '主键',
    original_name VARCHAR(255)  NOT NULL                               COMMENT '原始文件名（上传时的文件名）',
    stored_name   VARCHAR(100)  NOT NULL UNIQUE                        COMMENT '存储文件名（UUID.ext，物理磁盘名）',
    storage_path  VARCHAR(500)  NOT NULL                               COMMENT '相对存储路径，格式：yyyy/MM/uuid.ext',
    mime_type     VARCHAR(100)  NOT NULL                               COMMENT 'MIME 类型，如 application/pdf',
    file_size     BIGINT        NOT NULL                               COMMENT '文件大小（字节）',
    file_category VARCHAR(20)   NOT NULL                               COMMENT '文件类型：PDF/DOCUMENT/IMAGE/TEXT/ARCHIVE/OTHER',
    description   VARCHAR(500)  NULL                                   COMMENT '文件描述，可选，最多 500 字符',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP     COMMENT '上传时间',

    INDEX idx_file_category (file_category),
    INDEX idx_created_at    (created_at),
    INDEX idx_original_name (original_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件元数据表';

-- 文件-标签关联表（与 note_tag 结构对称）
CREATE TABLE IF NOT EXISTS file_tag (
    file_id BIGINT NOT NULL COMMENT '文件ID',
    tag_id  BIGINT NOT NULL COMMENT '标签ID',

    PRIMARY KEY (file_id, tag_id),
    INDEX idx_file_tag_tag_id (tag_id),
    CONSTRAINT fk_filetag_file FOREIGN KEY (file_id) REFERENCES knowledge_file(id) ON DELETE CASCADE,
    CONSTRAINT fk_filetag_tag  FOREIGN KEY (tag_id)  REFERENCES tag(id)            ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件-标签关联表';
```

### 2.2 索引策略

| 表 | 索引名 | 类型 | 用途 |
|----|--------|------|------|
| knowledge_file | PRIMARY (id) | B-Tree | 主键查找 |
| knowledge_file | UQ (stored_name) | UNIQUE | 防止磁盘文件名冲突 |
| knowledge_file | idx_file_category | B-Tree | 按文件类型筛选 |
| knowledge_file | idx_created_at | B-Tree | 按上传时间排序 |
| knowledge_file | idx_original_name | B-Tree | 按原始文件名 LIKE 搜索（前缀优化） |
| file_tag | PRIMARY (file_id, tag_id) | B-Tree | 联合主键，防重复关联 |
| file_tag | idx_file_tag_tag_id | B-Tree | 按 tag_id 查找关联文件 |

### 2.3 ER 关系（增量）

```
KnowledgeFile *---* Tag   （多对多，通过 file_tag；tag 表共享）
Note          *---* Tag   （多对多，通过 note_tag；已有，不变）
```

---

## 3. API 接口定义

### 统一响应格式（复用现有）

```json
// 成功
{ "code": 200, "message": "success", "data": {} }
// 错误
{ "code": 400, "message": "不支持该文件格式", "data": null }
// 磁盘不足（新增）
{ "code": 507, "message": "存储空间不足，请清理磁盘后重试", "data": null }
```

---

### POST /api/files — 上传文件

**Content-Type：** `multipart/form-data`

**表单字段：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | MultipartFile | 是 | 上传文件体 |
| description | string | 否 | 文件描述，最多 500 字符 |
| tagIds | long[] | 否 | 标签 ID 列表（可传多个） |

**响应（200）：**

```json
{
  "code": 200, "message": "success",
  "data": {
    "id": 1,
    "originalName": "report.pdf",
    "mimeType": "application/pdf",
    "fileSize": 2411724,
    "fileSizeDisplay": "2.3 MB",
    "fileCategory": "PDF",
    "description": "2026年Q1财务报告",
    "tags": [{ "id": 1, "name": "财务" }],
    "createdAt": "2026-03-10T14:30:00"
  }
}
```

**错误：**

| code | 条件 |
|------|------|
| 400 | 文件为空；格式不支持；描述超 500 字符 |
| 413 | 文件大小超过 100MB（Spring multipart 层面拦截） |
| 507 | 累计存储超过 10GB |

---

### GET /api/files — 文件列表

**查询参数：**

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| page | int | 1 | 页码 |
| size | int | 20 | 每页条数 |
| fileCategory | string | - | 文件类型筛选：PDF/DOCUMENT/IMAGE/TEXT/ARCHIVE（可选） |
| tagId | long | - | 标签 ID 筛选（可选）；P0 只支持单个 tagId；多标签 AND 筛选为 P1+ |
| keyword | string | - | 原始文件名模糊搜索（LIKE %keyword%，可选，最长 100 字符） |

**响应（200）：**

```json
{
  "code": 200, "message": "success",
  "data": {
    "records": [{
      "id": 1,
      "originalName": "report.pdf",
      "mimeType": "application/pdf",
      "fileSize": 2411724,
      "fileSizeDisplay": "2.3 MB",
      "fileCategory": "PDF",
      "description": "2026年Q1财务报告",
      "tags": [{ "id": 1, "name": "财务" }],
      "createdAt": "2026-03-10T14:30:00"
    }],
    "total": 50,
    "page": 1,
    "size": 20
  }
}
```

**错误：** `400` 参数非法。

---

### GET /api/files/{id}/download — 下载文件

**响应：**

- `200`：文件流，Headers：
  ```
  Content-Type: {mime_type}
  Content-Disposition: attachment; filename*=UTF-8''{encoded_original_name}
  Content-Length: {file_size}
  ```
  **文件名编码实现（RFC 5987）：**
  ```java
  String encoded = URLEncoder.encode(originalName, StandardCharsets.UTF_8)
                             .replace("+", "%20");
  response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
  ```
- `404`：文件元数据不存在

**实现说明：** 后端先查元数据，再从磁盘读取文件流，通过 `StreamingResponseBody` 或 `InputStreamResource` 返回。若磁盘文件已丢失（元数据存在但物理文件不存在），返回 `404` 并记录 warn 日志。

---

### DELETE /api/files/{id} — 删除文件

**响应（200）：** `{ "code": 200, "message": "success", "data": null }`

**删除顺序：**
1. 查元数据，不存在则返回 `404`
2. `@Transactional`：删除 `file_tag` 关联（由外键 CASCADE 自动）+ 删除 `knowledge_file` 记录
3. 事务提交后，删除磁盘文件（非事务操作，异常只记录 warn，不回滚已提交的元数据删除）

**错误：** `404` 文件不存在。

> **设计决策：** 磁盘删除在事务提交后执行。若磁盘删除失败，元数据已删除（文件对用户不可见），磁盘孤立文件可通过定期清理任务处理。这比"磁盘删除失败时回滚事务"更简单可靠。

---

## 4. 后端实现设计

### 4.1 项目结构（增量）

```
knowledge-backend/src/main/
├── java/com/knowledge/
│   ├── controller/
│   │   └── FileController.java           ← 新增
│   ├── service/
│   │   ├── FileService.java              ← 新增（业务逻辑）
│   │   └── FileStorageService.java       ← 新增（文件 I/O）
│   ├── mapper/
│   │   ├── KnowledgeFileMapper.java      ← 新增
│   │   └── FileTagMapper.java            ← 新增
│   ├── entity/
│   │   ├── KnowledgeFile.java            ← 新增
│   │   └── FileTag.java                  ← 新增
│   └── dto/
│       ├── FileDTO.java                  ← 新增（响应 DTO，含 tags 列表）
│       └── FileUploadRequest.java        ← 新增（描述 + tagIds）
└── resources/
    ├── application.yml                   ← 修改：增加 multipart + upload 配置
    ├── application-dev.yml               ← 修改：增加 app.upload.dir
    └── application-prod.yml              ← 修改：增加 app.upload.dir
```

### 4.2 关键类职责

| 类 | 职责 |
|----|------|
| `FileController` | REST 端点：POST /api/files、GET /api/files、GET /api/files/{id}/download、DELETE /api/files/{id}；@Valid 校验 |
| `FileService` | 文件格式校验（MIME + 扩展名）；磁盘空间检查；事务内保存元数据 + 标签关联；事务后删除磁盘 |
| `FileStorageService` | 生成 UUID 存储路径；写文件到磁盘；读文件流；删除磁盘文件；计算已用空间 |
| `KnowledgeFileMapper` | `BaseMapper<KnowledgeFile>` + 带 JOIN 的分页查询（关联 file_tag 过滤标签） |
| `FileTagMapper` | `BaseMapper<FileTag>` + 批量插入关联 |

### 4.3 文件格式校验（双重校验）

**第一层：扩展名白名单（Controller 层即时校验）**

```java
private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
    "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
    "jpg", "jpeg", "png", "gif", "webp", "txt", "md", "zip"
);
```

**第二层：Content-Type 白名单映射（Service 层校验）**

```java
private static final Map<String, String> EXTENSION_MIME_MAP = Map.of(
    "pdf",  "application/pdf",
    "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "doc",  "application/msword",
    // ... 其他类型
    "jpg",  "image/jpeg",
    "png",  "image/png"
    // ...
);
```

校验逻辑：
1. 提取文件扩展名（从 originalFilename 末尾取，小写）→ 不在白名单 → `400`
2. 获取 `MultipartFile.getContentType()` → 与扩展名期望的 MIME 对比 → 不匹配 → `400`（防止 `.jpg` 实际内容是 `.exe` 的伪装）

> 注：不引入 Apache Tika（增加 ~15MB 依赖），采用 Content-Type + 扩展名双重白名单校验，在单用户信任环境下已足够安全。

### 4.4 路径穿越防护

```java
// FileStorageService.resolveStorePath()
String safeExt = ext.replaceAll("[^a-zA-Z0-9]", "");  // 扩展名只保留字母数字
String uuid = UUID.randomUUID().toString().replace("-", "");
String yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
Path storagePath = Paths.get(uploadDir, yearMonth, uuid + "." + safeExt).normalize();

// 防止路径穿越：确认最终路径在 uploadDir 内
if (!storagePath.startsWith(Paths.get(uploadDir).normalize())) {
    throw BusinessException.badRequest("非法文件路径");
}
```

原始文件名（`original_name`）**只存数据库**，从不用于构建磁盘路径。

### 4.5 磁盘空间检查

```java
// FileService.checkDiskSpace()
long usedBytes = Files.walk(Paths.get(uploadDir))
    .filter(Files::isRegularFile)
    .mapToLong(p -> p.toFile().length())
    .sum();
long limitBytes = 10L * 1024 * 1024 * 1024; // 10GB
if (usedBytes + incomingSize > limitBytes) {
    throw new BusinessException(507, "存储空间不足，请清理磁盘后重试");
}
```

### 4.6 配置（application.yml 增量）

```yaml
# application.yml 新增
spring:
  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 105MB   # 略大于单文件限制（含表单字段开销）
      file-size-threshold: 1MB  # 超过 1MB 写临时文件，减少内存压力

# application-dev.yml 新增
app:
  upload:
    dir: ${user.home}/knowledge-uploads   # 开发环境存储目录

# application-prod.yml 新增
app:
  upload:
    dir: /var/knowledge/uploads           # 生产环境存储目录
```

**GlobalExceptionHandler 新增（处理 multipart 超限）：**

```java
@ExceptionHandler(MaxUploadSizeExceededException.class)
public Result<?> handleMaxUploadSize(MaxUploadSizeExceededException e) {
    return Result.error(413, "文件大小不能超过 100MB");
}
```

### 4.7 FileService 核心方法

```java
@Transactional
public FileDTO uploadFile(MultipartFile file, String description, List<Long> tagIds) {
    // 1. 格式校验（扩展名 + Content-Type）
    String ext = validateAndGetExtension(file);
    // 2. 大小校验（冗余，multipart 配置已拦截，这里做业务层保障）
    if (file.getSize() > 100 * 1024 * 1024L) throw BusinessException.badRequest("文件大小不能超过 100MB");
    // 2b. 标签数量上限校验
    if (tagIds != null && tagIds.size() > 20) throw BusinessException.badRequest("标签数量不能超过 20 个");
    // 3. 磁盘空间检查
    checkDiskSpace(file.getSize());
    // 4. 确定 FileCategory
    FileCategory category = resolveCategory(ext);
    // 5. 生成存储路径（UUID）
    String storagePath = fileStorageService.generatePath(ext);
    String storedName = Paths.get(storagePath).getFileName().toString();
    // 6. 保存元数据（事务内）
    KnowledgeFile entity = new KnowledgeFile();
    entity.setOriginalName(file.getOriginalFilename());
    entity.setStoredName(storedName);
    entity.setStoragePath(storagePath);
    entity.setMimeType(file.getContentType());
    entity.setFileSize(file.getSize());
    entity.setFileCategory(category.name());
    entity.setDescription(description);
    knowledgeFileMapper.insert(entity);
    // 7. 保存标签关联（事务内）
    if (tagIds != null && !tagIds.isEmpty()) {
        fileTagMapper.batchInsert(entity.getId(), tagIds);
    }
    // 8. 写入磁盘（仍在 @Transactional 内，写入失败抛 IOException → Spring 回滚 DB 操作）
    fileStorageService.store(file, storagePath);
    return toDTO(entity, tagIds);
}
```

> **事务边界说明：**
> - `fileStorageService.store()` 在 `@Transactional` 方法内最后执行
> - 磁盘写入失败（IOException）→ 事务回滚（DB 记录撤销）— 无孤立元数据
> - DB 操作失败 → 事务回滚（磁盘文件未创建）— 无孤立磁盘文件
> - **已知局限：** 若磁盘写入成功、但事务提交阶段 DB 网络异常，会产生孤立磁盘文件（单用户系统此概率极低，可接受）

### 4.8 KnowledgeFileMapper 关键查询

分页查询（需支持类型筛选、标签筛选、关键词搜索）：

```xml
<!-- mapper/KnowledgeFileMapper.xml -->
<select id="selectPageWithTags" resultMap="FileWithTagsResultMap">
  SELECT f.*, t.id AS tag_id, t.name AS tag_name
  FROM knowledge_file f
  LEFT JOIN file_tag ft ON ft.file_id = f.id
  LEFT JOIN tag t ON t.id = ft.tag_id
  <where>
    <if test="fileCategory != null and fileCategory != ''">
      AND f.file_category = #{fileCategory}
    </if>
    <if test="keyword != null and keyword != ''">
      AND f.original_name LIKE CONCAT('%', #{keyword}, '%')
    </if>
    <if test="tagId != null">
      AND f.id IN (
        SELECT file_id FROM file_tag WHERE tag_id = #{tagId}
      )
    </if>
  </where>
  ORDER BY f.created_at DESC
</select>
```

---

## 5. 前端实现设计

### 5.1 项目结构（增量）

```
knowledge-frontend/src/
├── pages/
│   └── FileListPage.vue            ← 新增
├── components/
│   └── FileUploadDialog.vue        ← 新增
├── api/
│   └── file.js                     ← 新增
├── stores/
│   └── fileStore.js                ← 新增
├── router/index.js                 ← 修改（追加 1 条路由）
└── components/AppSidebar.vue       ← 修改（追加 1 个链接）
```

### 5.2 API 模块（api/file.js）

```javascript
import request from './request'

export function getFiles(params) {
  return request.get('/api/files', { params })
}

export function uploadFile(formData, onProgress) {
  return request.post('/api/files', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: e => onProgress?.(Math.round((e.loaded * 100) / e.total))
  })
}

export function downloadFile(id) {
  return request.get(`/api/files/${id}/download`, { responseType: 'blob' })
}

export function deleteFile(id) {
  return request.delete(`/api/files/${id}`)
}
```

### 5.3 Store（stores/fileStore.js）

```javascript
// Pinia store 状态
state: {
  files: [],          // 当前页文件列表
  total: 0,           // 总条数
  page: 1,            // 当前页
  size: 20,           // 每页条数
  fileCategory: '',   // 类型筛选（空串=全部）
  tagIds: [],         // 标签 ID 筛选列表
  keyword: '',        // 文件名搜索关键词
  loading: false
}
// actions: fetchFiles（构造查询参数调用 getFiles）、resetFilters
```

### 5.4 文件下载实现（Blob 方式）

```javascript
// FileListPage.vue
async function handleDownload(file) {
  try {
    const response = await downloadFile(file.id)
    const blob = new Blob([response.data], { type: response.headers['content-type'] })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = file.originalName  // 使用原始文件名
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error('下载失败，文件可能已被删除')
  }
}
```

### 5.5 上传进度（FileUploadDialog.vue）

```javascript
// 上传进度回调 → 更新 uploadProgress (0-100)
const uploadProgress = ref(0)
const uploadStatus = ref('idle') // idle | selected | uploading | success | error

async function submitUpload() {
  uploadStatus.value = 'uploading'
  const formData = new FormData()
  formData.append('file', selectedFile.value)
  if (description.value) formData.append('description', description.value)
  selectedTagIds.value.forEach(id => formData.append('tagIds', id))

  try {
    await uploadFile(formData, p => { uploadProgress.value = p })
    uploadStatus.value = 'success'
    emit('success')  // 父组件刷新列表
    dialogVisible.value = false
    ElMessage.success('上传成功')
  } catch (e) {
    uploadStatus.value = 'error'
    errorMsg.value = e.response?.data?.message || '上传失败，请重试'
  }
}
```

### 5.6 路由修改（router/index.js）

```javascript
import FileListPage from '../pages/FileListPage.vue'
// 在现有路由数组末尾追加：
{ path: '/files', name: 'FileList', component: FileListPage }
```

### 5.7 侧边栏修改（AppSidebar.vue）

在现有快捷入口末尾追加：

```html
<router-link to="/files" class="sidebar-link">文件管理</router-link>
```

---

## 6. 文件存储方案

### 存储路径设计

```
{upload-dir}/
  2026/
    03/
      a1b2c3d4e5f6...abc.pdf      ← UUID 命名，无原始文件名信息
      f7e8d9c0a1b2...xyz.docx
    04/
      ...
```

| 设计要点 | 说明 |
|---------|------|
| 按年/月分目录 | 防止单目录文件过多，便于按时间管理和备份 |
| UUID 命名 | 防止路径穿越；防止同名文件覆盖；不泄露原始文件名 |
| 相对路径入库 | `storage_path` 只存 `2026/03/uuid.pdf`，不存绝对路径，便于迁移 |
| 上传目录可配置 | `app.upload.dir` 通过配置文件注入，不硬编码 |

### 文件 I/O 实现（FileStorageService）

```java
@Service
public class FileStorageService {
    @Value("${app.upload.dir}")
    private String uploadDir;

    // 生成存储路径（返回相对路径字符串）
    public String generatePath(String ext) {
        String yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return yearMonth + "/" + uuid + "." + ext.toLowerCase();
    }

    // 写文件到磁盘
    public void store(MultipartFile file, String relativePath) throws IOException {
        Path target = Paths.get(uploadDir, relativePath).normalize();
        Files.createDirectories(target.getParent());
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // 读文件流（用于下载）
    public Path resolve(String relativePath) {
        return Paths.get(uploadDir, relativePath).normalize();
    }

    // 删除磁盘文件
    public boolean delete(String relativePath) {
        try {
            return Files.deleteIfExists(Paths.get(uploadDir, relativePath).normalize());
        } catch (IOException e) {
            log.warn("删除磁盘文件失败: {}", relativePath, e);
            return false;
        }
    }
}
```

---

## 7. 安全设计

| 威胁 | 防护措施 | 实现层 |
|------|---------|--------|
| 路径穿越（Path Traversal） | 存储名使用 UUID（不含路径分隔符）；`normalize()` 后验证是否仍在 uploadDir 内 | `FileStorageService` |
| 恶意文件类型伪装 | 扩展名白名单 + Content-Type 与扩展名映射双重校验 | `FileService.validateAndGetExtension()` |
| 文件名注入 | 原始文件名只存 DB，不用于构建磁盘路径；下载时用 `filename*=UTF-8''` RFC 5987 编码 | `FileService`、`FileController` |
| 大文件 DoS | Spring multipart `max-file-size=100MB`；`MaxUploadSizeExceededException` 全局处理 | 配置 + `GlobalExceptionHandler` |
| 磁盘耗尽 | 上传前检查已用空间，超 10GB 返回 507 | `FileService.checkDiskSpace()` |
| 静态路径暴露 | `uploads/` 目录不配置为 Spring 静态资源，下载只通过 `/api/files/{id}/download` 接口 | `FileController`（流式返回） |
| XSS（文件名展示） | 前端使用 `{{ }}` 插值（Vue 自动转义），不用 `v-html` 展示文件名 | `FileListPage.vue` |

---

## 8. 测试方案（技术层）

### 后端单元/集成测试（knowledge-backend/src/test/）

| 测试类 | 类型 | 关注点 |
|-------|------|--------|
| `FileServiceTest` | 单元（Mockito） | 格式校验逻辑、文件分类推断、磁盘空间检查 |
| `FileStorageServiceTest` | 单元（临时目录） | 路径生成、UUID 唯一性、路径穿越防护 |
| `FileControllerTest` | 集成（H2 + MockMvc） | 4 个接口的 HTTP 状态码、响应 JSON、文件上传流程 |

### E2E 测试（testing/e2e/）

| 测试场景 | 工具 |
|---------|------|
| 上传文件 → 列表验证 | Playwright |
| 下载文件 → 浏览器下载触发 | Playwright |
| 删除文件 → 确认框 → 列表刷新 | Playwright |
| 类型筛选 + 标签筛选组合 | Playwright |

---

## 9. 需求-设计追溯矩阵

| 用户故事 | 优先级 | 后端 API | 前端组件 | 数据库 |
|---------|--------|---------|---------|--------|
| US-F001 上传文件 | P0 | POST /api/files | FileUploadDialog | knowledge_file + file_tag INSERT |
| US-F002 查看文件列表 | P0 | GET /api/files | FileListPage | knowledge_file SELECT（分页+筛选） |
| US-F003 下载文件 | P0 | GET /api/files/{id}/download | FileListPage（下载按钮） | knowledge_file SELECT（获取路径） |
| US-F004 删除文件 | P0 | DELETE /api/files/{id} | FileListPage（删除确认） | knowledge_file + file_tag DELETE |
| US-F005 文件打标签 | P1 | POST /api/files（含 tagIds） | FileUploadDialog（TagSelect）+ FileListPage | file_tag INSERT/SELECT |
| US-F006 按文件名搜索 | P1 | GET /api/files?keyword=xxx | FileListPage（搜索框） | knowledge_file LIKE 查询 |

---

## 10. 风险识别与应对

| 风险 | 概率 | 影响 | 应对策略 |
|------|------|------|---------|
| 磁盘写入失败导致数据不一致 | 低 | 高 | 磁盘写入在事务内最后执行；写入失败触发 DB 回滚；上传目录权限在部署文档中明确说明 |
| 大文件上传超时（客户端） | 中 | 中 | axios 不设 timeout（默认无限）；服务端 `spring.mvc.async.request-timeout` 设为 120s |
| Content-Type 校验被绕过 | 低 | 中 | 双重校验（扩展名 + Content-Type）；单用户信任环境风险可接受；后续可升级为文件头 magic bytes 校验 |
| UUID 碰撞 | 极低 | 低 | UUID v4 碰撞概率 < 1/10^18，数据库 UNIQUE 约束兜底 |
| 磁盘空间检查有并发竞态 | 极低 | 低 | 单用户系统无并发，空间检查仅用于提示，非强制保障 |
| 下载时磁盘文件丢失（元数据存在） | 低 | 中 | 检测后返回 404；记录 warn 日志；提示用户文件已损坏 |
