# 测试用例集 — 文件类型知识（增量特性）

> 版本：1.0 | 日期：2026-03-10 | 作者：QA Agent
>
> 约定：
> - **HOST** = `http://localhost:8080`
> - **UPLOADS** = 服务器 uploads 目录（默认 `./uploads/`）
> - `{fileId}` 表示由前一步创建返回的真实文件 ID
> - 用例 ID 前缀 **TC-F-** 区别于现有 TC-N-/TC-S-/TC-T-/TC-C- 系列
> - **类型**：Unit = 单元测试；API = API 集成测试；E2E = Playwright 端到端测试
> - **优先级**：P0（必测）/ P1（重要）/ P2（补充）/ P3（低频边界）
> - 所有 curl 命令使用 `-s`（静默）；文件上传使用 `-F` 参数发送 `multipart/form-data`

---

## 一、文件上传 — 正向用例

| 用例ID | 所属模块 | 前置条件 | 操作步骤 | 预期结果 | 优先级 | 测试类型 |
|--------|---------|---------|---------|---------|--------|---------|
| TC-F-001 | 文件上传 | 系统已启动；数据库已初始化；`uploads/` 目录可写；待上传文件 `sample.pdf`（< 1MB）已准备好 | `curl -s -X POST HOST/api/files -F "file=@sample.pdf;type=application/pdf" -F "description=季度报告" -F "tagIds=[]"` | HTTP 200；响应 `code=200`；`data.id` 不为 null；`data.originalFilename="sample.pdf"`；`data.mimeType="application/pdf"`；`data.fileSize > 0`；`data.description="季度报告"`；`data.uploadedAt` 不为 null；磁盘路径 `UPLOADS/{year}/{month}/{uuid}.pdf` 文件实际存在 | P0 | API |
| TC-F-002 | 文件上传 | 同 TC-F-001，待上传文件为 `sample.docx` | `curl -s -X POST HOST/api/files -F "file=@sample.docx;type=application/vnd.openxmlformats-officedocument.wordprocessingml.document"` | HTTP 200；响应 `code=200`；`data.originalFilename="sample.docx"`；`data.fileCategory="文档"` | P0 | API |
| TC-F-003 | 文件上传 | 同 TC-F-001，待上传文件为 `sample.png` | `curl -s -X POST HOST/api/files -F "file=@sample.png;type=image/png"` | HTTP 200；响应 `code=200`；`data.originalFilename="sample.png"`；`data.fileCategory="图片"` | P0 | API |
| TC-F-004 | 文件上传 | 同 TC-F-001，待上传文件为 `sample.txt` | `curl -s -X POST HOST/api/files -F "file=@sample.txt;type=text/plain"` | HTTP 200；响应 `code=200`；`data.fileCategory="文本"` | P1 | API |
| TC-F-005 | 文件上传 | 同 TC-F-001，待上传文件为 `archive.zip` | `curl -s -X POST HOST/api/files -F "file=@archive.zip;type=application/zip"` | HTTP 200；响应 `code=200`；`data.fileCategory="压缩包"` | P1 | API |
| TC-F-006 | 文件上传 | 系统已启动；已有标签 tagId=1（"技术"）、tagId=2（"设计"） | `curl -s -X POST HOST/api/files -F "file=@sample.pdf;type=application/pdf" -F "tagIds[]=1" -F "tagIds[]=2"` | HTTP 200；响应 `code=200`；`data.tags` 数组长度为 2，包含 id=1 和 id=2 的标签；`file_tag` 表中存在对应关联行 | P0 | API |
| TC-F-007 | 文件上传 | 同 TC-F-001，不传 description 和 tagIds 字段 | `curl -s -X POST HOST/api/files -F "file=@sample.pdf;type=application/pdf"` | HTTP 200；响应 `code=200`；`data.description=null` 或空字符串；`data.tags=[]`（无标签） | P0 | API |

---

## 二、文件上传 — 异常用例

| 用例ID | 所属模块 | 前置条件 | 操作步骤 | 预期结果 | 优先级 | 测试类型 |
|--------|---------|---------|---------|---------|--------|---------|
| TC-F-008 | 文件上传 | 系统已启动；待上传文件 `malware.exe`（.exe 扩展名） | `curl -s -X POST HOST/api/files -F "file=@malware.exe;type=application/octet-stream"` | HTTP 200；响应 `code=400`；`message` 包含"不支持该文件格式"和支持格式列表；`data=null`；磁盘无新文件写入 | P0 | API |
| TC-F-009 | 文件上传 | 系统已启动；待上传文件 `large-file.bin`（101MB，超过 100MB 限制） | `curl -s -X POST HOST/api/files -F "file=@large-file.bin;type=application/octet-stream"` | HTTP 200（或 HTTP 413 Payload Too Large）；响应 `code=400`（或 413）；`message` 包含"文件大小不能超过 100MB"；`data=null`；磁盘无文件写入 | P0 | API |
| TC-F-010 | 文件上传 | 系统已启动；待上传文件 `empty.txt`（0 字节） | `curl -s -X POST HOST/api/files -F "file=@empty.txt;type=text/plain"` | HTTP 200；响应 `code=400`；`message` 包含"文件内容不能为空"；`data=null` | P0 | API |
| TC-F-011 | 文件上传 | 系统已启动；构造 description 字段为 501 字符的字符串 | `curl -s -X POST HOST/api/files -F "file=@sample.pdf;type=application/pdf" -F "description=<501字符字符串>"` | HTTP 200；响应 `code=400`；`message` 包含"描述不能超过 500 字符"；`data=null` | P1 | API |
| TC-F-012 | 文件上传 | 系统已启动；不携带 file 字段（空请求体） | `curl -s -X POST HOST/api/files -H "Content-Type: multipart/form-data"` | HTTP 200；响应 `code=400`；`message` 包含"文件不能为空"；`data=null` | P0 | API |

---

## 三、文件上传 — 边界用例

| 用例ID | 所属模块 | 前置条件 | 操作步骤 | 预期结果 | 优先级 | 测试类型 |
|--------|---------|---------|---------|---------|--------|---------|
| TC-F-013 | 文件上传 | 系统已启动；待上传文件大小恰好 100MB（100 * 1024 * 1024 字节） | `curl -s -X POST HOST/api/files -F "file=@100mb-file.bin;type=application/zip"` | HTTP 200；响应 `code=200`；上传成功，`data.fileSize=104857600`（字节）；磁盘文件存在 | P0 | API |
| TC-F-014 | 文件上传 | 系统已启动；准备文件名含特殊字符的文件：`报告 2026 (Q1) #1.pdf` | `curl -s -X POST HOST/api/files -F "file=@'报告 2026 (Q1) #1.pdf';type=application/pdf"` | HTTP 200；响应 `code=200`；`data.originalFilename="报告 2026 (Q1) #1.pdf"`（保留原始文件名）；磁盘存储路径为 `{uuid}.pdf`（UUID 重命名，不含特殊字符） | P1 | API |
| TC-F-015 | 文件上传 | 系统已启动；已有文件 `report.pdf`（fileId=1）；再次准备同名文件 `report.pdf` | 步骤1：`curl -s -X POST HOST/api/files -F "file=@report.pdf;type=application/pdf"` 上传第一个 `report.pdf`；步骤2：再次上传同名文件 | 步骤2：HTTP 200；响应 `code=200`；创建了新的文件记录，`data.id != 1`；两条记录的 `originalFilename` 均为 `report.pdf`，但 `id` 和存储路径（UUID）不同；磁盘上存在两个不同 UUID 的 .pdf 文件（原文件未被覆盖） | P0 | API |
| TC-F-016 | 文件上传 | 系统已启动；description 恰好 500 字符（边界值） | `curl -s -X POST HOST/api/files -F "file=@sample.pdf;type=application/pdf" -F "description=<500字符字符串>"` | HTTP 200；响应 `code=200`；`data.description` 长度为 500 | P1 | API |
| TC-F-017 | 文件上传（安全：MIME 伪装） | 系统已启动；构造一个内容为 PE 可执行文件但扩展名为 `.pdf`、Content-Type 声明为 `application/pdf` 的文件 | `curl -s -X POST HOST/api/files -F "file=@fake.pdf;type=application/pdf"`（fake.pdf 的魔数为 MZ，实为 EXE） | HTTP 200；响应 `code=400`；`message` 包含"文件类型校验失败"（服务端通过 MIME 嗅探或文件魔数检测识别真实类型）；磁盘无文件写入 | P0 | API |

---

## 四、文件列表查询 — 正向用例

| 用例ID | 所属模块 | 前置条件 | 操作步骤 | 预期结果 | 优先级 | 测试类型 |
|--------|---------|---------|---------|---------|--------|---------|
| TC-F-018 | 文件列表 | 系统已启动；数据库中有 5 条文件记录（含 PDF 2 个、图片 2 个、文档 1 个）；按上传时间依次为 file1（最早）…file5（最晚） | `curl -s "HOST/api/files?page=1&size=20"` | HTTP 200；响应 `code=200`；`data.records` 长度为 5；`data.total=5`；`data.page=1`；`data.size=20`；第一条记录为 file5（上传时间最晚，降序排列）；每条记录含 `id`、`originalFilename`、`fileSize`、`fileCategory`、`description`、`tags`、`uploadedAt` 字段 | P0 | API |
| TC-F-019 | 文件列表 | 同 TC-F-018 | `curl -s "HOST/api/files?page=1&size=20&category=PDF"` | HTTP 200；响应 `code=200`；`data.records` 长度为 2；所有记录的 `fileCategory="PDF"` | P0 | API |
| TC-F-020 | 文件列表 | 同 TC-F-018 | `curl -s "HOST/api/files?page=1&size=20&category=图片"` | HTTP 200；响应 `code=200`；`data.records` 长度为 2；所有记录的 `fileCategory="图片"` | P0 | API |
| TC-F-021 | 文件列表 | 数据库中有 3 个文件关联标签 tagId=1，2 个文件无此标签 | `curl -s "HOST/api/files?page=1&size=20&tagId=1"` | HTTP 200；响应 `code=200`；`data.records` 长度为 3；所有记录的 `tags` 数组均包含 id=1 的标签 | P1 | API |
| TC-F-022 | 文件列表 | 数据库中有 3 个文件，其中 2 个文件名含关键词"季度" | `curl -s "HOST/api/files?page=1&size=20&keyword=季度"` | HTTP 200；响应 `code=200`；`data.records` 长度为 2；所有记录的 `originalFilename` 均包含"季度"（LIKE 匹配，不区分大小写） | P1 | API |
| TC-F-023 | 文件列表（组合筛选） | 数据库中有：A（PDF + tagId=1）、B（PDF + 无标签）、C（图片 + tagId=1）、D（图片 + 无标签） | `curl -s "HOST/api/files?category=PDF&tagId=1"` | HTTP 200；响应 `code=200`；`data.records` 只包含文件 A（同时满足 PDF 类型 AND tagId=1）；B、C、D 均不出现 | P1 | API |

---

## 五、文件列表查询 — 边界用例

| 用例ID | 所属模块 | 前置条件 | 操作步骤 | 预期结果 | 优先级 | 测试类型 |
|--------|---------|---------|---------|---------|--------|---------|
| TC-F-024 | 文件列表（空态） | 数据库中无任何文件记录 | `curl -s "HOST/api/files?page=1&size=20"` | HTTP 200；响应 `code=200`；`data.records=[]`；`data.total=0` | P0 | API |
| TC-F-025 | 文件列表（分页恰好 20 条） | 数据库中恰好有 20 条文件记录 | `curl -s "HOST/api/files?page=1&size=20"` | HTTP 200；`data.records` 长度为 20；`data.total=20`；`data.page=1` | P1 | API |
| TC-F-026 | 文件列表（筛选无结果） | 数据库中有文件，但无 fileCategory="压缩包" 的文件 | `curl -s "HOST/api/files?category=压缩包"` | HTTP 200；响应 `code=200`；`data.records=[]`；`data.total=0`（不返回 404） | P1 | API |
| TC-F-027 | 文件列表（搜索无结果） | 数据库中无文件名含"xyznotexist"的文件 | `curl -s "HOST/api/files?keyword=xyznotexist"` | HTTP 200；`data.records=[]`；`data.total=0` | P1 | API |
| TC-F-028 | 文件列表（分页第 2 页） | 数据库中有 25 条文件记录 | `curl -s "HOST/api/files?page=2&size=20"` | HTTP 200；`data.records` 长度为 5（第 2 页剩余）；`data.total=25`；`data.page=2` | P1 | API |

---

## 六、文件下载

| 用例ID | 所属模块 | 前置条件 | 操作步骤 | 预期结果 | 优先级 | 测试类型 |
|--------|---------|---------|---------|---------|--------|---------|
| TC-F-029 | 文件下载 | 已上传文件 `report.pdf`，返回 `fileId=1`；磁盘文件实际存在 | `curl -s -I "HOST/api/files/1/download"` | HTTP 200；响应头含 `Content-Disposition: attachment; filename="report.pdf"`；`Content-Type` 为 `application/pdf`；`Content-Length > 0`；响应体为文件二进制内容（非空） | P0 | API |
| TC-F-030 | 文件下载（文件已被删除） | 已有 fileId=1 的文件记录，但对应磁盘文件已被手动删除（模拟磁盘文件丢失） | `curl -s "HOST/api/files/1/download"` | HTTP 200；响应 `code=404`；`message` 包含"文件不存在"；响应体无文件内容 | P0 | API |
| TC-F-031 | 文件下载（记录不存在） | 数据库中不存在 id=99999 的文件记录 | `curl -s "HOST/api/files/99999/download"` | HTTP 200；响应 `code=404`；`message` 包含"文件不存在"；`data=null` | P0 | API |

---

## 七、文件删除

| 用例ID | 所属模块 | 前置条件 | 操作步骤 | 预期结果 | 优先级 | 测试类型 |
|--------|---------|---------|---------|---------|--------|---------|
| TC-F-032 | 文件删除 | 已上传文件 `sample.pdf`，fileId=1；磁盘文件路径已知 | 步骤1：`curl -s -X DELETE HOST/api/files/1`；步骤2：`curl -s "HOST/api/files/1/download"`；步骤3：检查磁盘文件路径是否还存在 | 步骤1：HTTP 200；响应 `code=200`；`message="删除成功"`；步骤2：响应 `code=404`；步骤3：磁盘文件路径不存在（文件已物理删除�� | P0 | API |
| TC-F-033 | 文件删除（file_tag 级联清除） | 已上传文件 fileId=1，关联标签 tagId=1；`file_tag` 表中存在 `file_id=1, tag_id=1` 记录 | 步骤1：`curl -s -X DELETE HOST/api/files/1`；步骤2：查询 `file_tag` 表中 `file_id=1` 的记录 | 步骤1：`code=200`；步骤2：`file_tag` 中无 `file_id=1` 的记录（级联删除生效） | P0 | API |
| TC-F-034 | 文件删除（记录已不存在） | 数据库中不存在 id=99999 的文件记录 | `curl -s -X DELETE HOST/api/files/99999` | HTTP 200；响应 `code=404`；`message` 包含"文件不存在" | P0 | API |

---

## 八、标签集成

| 用例ID | 所属模块 | 前置条件 | 操作步骤 | 预期结果 | 优先级 | 测试类型 |
|--------|---------|---------|---------|---------|--------|---------|
| TC-F-035 | 标签集成 | 已上传文件 fileId=1；已有标签 tagId=1（"技术"）、tagId=2（"设计"） | `curl -s -X POST HOST/api/files/1/tags -H "Content-Type: application/json" -d '{"tagIds":[1,2]}'` | HTTP 200；响应 `code=200`；`data.tags` 含 id=1 和 id=2 的标签；`file_tag` 表新增 2 条记录 | P1 | API |
| TC-F-036 | 标签集成（删除文件单个标签） | 已上传文件 fileId=1，关联标签 tagId=1 和 tagId=2 | `curl -s -X DELETE HOST/api/files/1/tags/1` | HTTP 200；响应 `code=200`；`GET /api/files` 中 fileId=1 的 tags 只含 tagId=2；`file_tag` 中 `file_id=1, tag_id=1` 的记录已删除 | P1 | API |
| TC-F-037 | 标签集成（删除标签后 file_tag 级联清除） | 已有标签 tagId=1；已有文件 fileId=1 和 fileId=2 均关联 tagId=1；`file_tag` 表中有 2 条关联记录 | 步骤1：`curl -s -X DELETE HOST/api/tags/1`；步骤2：`curl -s "HOST/api/files?tagId=1"`；步骤3：查询 `file_tag` 表中 `tag_id=1` 的记录 | 步骤1：`code=200`；步骤2：`data.records=[]`（无文件关联已删除的标签）；步骤3：`file_tag` 中无 `tag_id=1` 的记录（ON DELETE CASCADE 生效） | P0 | API |
| TC-F-038 | 标签集成（标签数量超上限） | 已上传文件 fileId=1；尝试关联 21 个标签（已有 20 个） | `curl -s -X POST HOST/api/files/1/tags -H "Content-Type: application/json" -d '{"tagIds":[21]}'`（当前已关联 20 个标签） | HTTP 200；响应 `code=400`；`message` 包含"标签数量不能超过 20 个" | P1 | API |

---

## 九、安全测试

| 用例ID | 所属模块 | 前置条件 | 操作步骤 | 预期结果 | 优先级 | 测试类型 |
|--------|---------|---------|---------|---------|--------|---------|
| TC-F-039 | 安全（路径穿越） | 系统已启动 | `curl -s -X POST HOST/api/files -F "file=@sample.pdf;type=application/pdf" -F "filename=../../etc/passwd.pdf"` 或通过 Burp Suite 修改 multipart Content-Disposition 中的 filename 为 `../../../etc/passwd` | HTTP 200；响应 `code=200` 或 `code=400`；关键：磁盘上**不存在** `/etc/passwd`、`uploads/` 目录以外的新文件；存储路径必须在 `uploads/{year}/{month}/` 内（通过 UUID 重命名，原始 filename 不直接用于存储路径） | P0 | API |
| TC-F-040 | 安全（恶意文件名，SQL 注入尝试） | 系统已启动 | `curl -s -X POST HOST/api/files -F "file=@sample.pdf;type=application/pdf"` 其中 multipart filename 设为 `'; DROP TABLE knowledge_file; --.pdf` | HTTP 200；响应 `code=200`；文件上传成功；`data.originalFilename` 值为原始文件名字符串（SQL 注入字符不被执行）；数据库 `knowledge_file` 表仍正常存在 | P0 | API |
| TC-F-041 | 安全（恶意文件名，XSS 尝试） | 系统已启动 | `curl -s -X POST HOST/api/files -F "file=@sample.pdf;type=application/pdf"` 其中 filename 设为 `<script>alert(1)</script>.pdf` | HTTP 200；响应 `code=200`；`data.originalFilename` 为经过转义的字符串（不含原始 `<script>` 标签）或原始字符串被安全存储后在前端渲染时转义；API 响应中不直接返回可执行脚本 | P1 | API |
| TC-F-042 | 安全（MIME 伪装，服务端双重校验） | 系统已启动；构造 Content-Type 声明为 `image/png` 但扩展名为 `.exe` 的文件 | `curl -s -X POST HOST/api/files -F "file=@malware.exe;type=image/png"` | HTTP 200；响应 `code=400`；`message` 包含格式校验失败（扩展名 .exe 不在支持列表，无论 Content-Type 声明为何）；磁盘无文件写入 | P0 | API |
| TC-F-043 | 安全（单元测试：路径穿越防护） | 无需运行服务 | 单元测试：向 `FileService.buildStoragePath(filename)` 传入 `../../../etc/passwd.pdf`、`..\\..\\windows\\system32\\config.pdf` | 返回的存储路径不含 `../` 或 `..\` 字符序列；存储路径格式为 `{year}/{month}/{uuid}.pdf`（UUID 截断了原始 filename 中的所有路径穿越字符） | P0 | Unit |

---

## 十、E2E 测试用例（Playwright）

| 用例ID | 所属模块 | 前置条件 | 操作步骤 | 预期结果 | 优先级 | 测试类型 |
|--------|---------|---------|---------|---------|--------|---------|
| TC-F-044 | E2E — 上传文件 | 后端和前端服务均已启动；数据库干净（无文件记录）；已准备 `sample.pdf`（< 1MB）；已有标签"技术" | 1. 访问 `http://localhost:5173/files`；2. 页面显示空状态"暂无文件"；3. 点击"上传文件"按钮；4. 弹出上传对话框；5. 通过文件选择器选择 `sample.pdf`；6. 验证已选文件区域显示文件名和大小；7. 填写描述"测试文档"；8. 在标签多选下拉中选择"技术"；9. 点击"上传文件"按钮；10. 等待上传完成 | 步骤2：空状态插图和提示文案可见（`[data-testid="empty-state"]`）；步骤4：对话框打开（`[data-testid="upload-dialog"]` visible）；步骤6：显示"sample.pdf"和文件大小；步骤10：对话框关闭；Toast 提示"上传成功"可见；文件列表刷新，第一行显示"sample.pdf"，标签含"技术" | P0 | E2E |
| TC-F-045 | E2E — 类型筛选 | 后端和前端服务均已启动；数据库中有 PDF 文件 2 个、图片文件 2 个 | 1. 访问 `/files`；2. 默认"全部"Tab，列表显示 4 条；3. 点击"PDF" Tab；4. 等待列表刷新；5. 点击"全部" Tab | 步骤2：`[data-testid="file-table"]` 行数为 4；步骤4：行数变为 2，所有行的文件图标为 PDF 类型；步骤5：行数恢复为 4 | P0 | E2E |
| TC-F-046 | E2E — 文件名搜索 | 后端和前端服务均已启动；数据库中有文件"季度报告.pdf"和"年度总结.docx" | 1. 访问 `/files`；2. 在搜索框输入"季度"；3. 等待防抖 300ms 后列表刷新；4. 清空搜索框；5. 等待列表刷新 | 步骤3：列表只显示"季度报告.pdf"；步骤5：列表恢复显示 2 条文件 | P1 | E2E |
| TC-F-047 | E2E — 删除文件（确认流程） | 后端和前端服务均已启动；数据库中有文件"sample.pdf"（fileId=1） | 1. 访问 `/files`；2. 点击文件行的删除图标；3. 确认对话框弹出，显示文件名"sample.pdf"；4. 点击"取消"；5. 再次点击删除图标；6. 点击"确认删除" | 步骤3：`el-message-box` 对话框显示"确认删除文件《sample.pdf》？"；步骤4：对话框关闭，文件仍在列表中；步骤6：Toast 提示"删除成功"，文件从列表中消失；若列表变空则显示空状态 | P0 | E2E |
| TC-F-048 | E2E — 空态展示 | 后端和前端服务均已启动；数据库中无任何文件记录 | 1. 访问 `/files` | 页面显示空状态插图；提示文案含"暂无文件，点击上传添加知识文件"；"上传文件"按钮可见（`getByRole('button', { name: '上传文件' })`） | P0 | E2E |
| TC-F-049 | E2E — 上传格式校验（前端校验） | 后端和前端服务均已启动；准备文件 `test.exe` | 1. 访问 `/files`；2. 点击"上传文件"；3. 在文件选择器中选择 `test.exe` | 选择文件后立即显示格式错误提示（客户端校验）；"上传文件"按钮不可点击（或点击无效）；对话框中显示错误文案含"不支持该文件格式" | P1 | E2E |

---

## 十一、单元测试用例（FileService）

| 用例ID | 所属模块 | 前置条件 | 操作步骤 | 预期结果 | 优先级 | 测试类型 |
|--------|---------|---------|---------|---------|--------|---------|
| TC-F-050 | 单元 — 格式校验 | 无（Mock 依赖） | 调用 `fileService.validateFileType("report.pdf", "application/pdf")` 及 `fileService.validateFileType("malware.exe", "application/octet-stream")` | `"report.pdf"` 调用：无异常抛出；`"malware.exe"` 调用：抛出业务异常，message 含"不支持的文件格式" | P0 | Unit |
| TC-F-051 | 单元 — 大小校验 | 无（Mock 依赖） | 调用 `fileService.validateFileSize(104857600)` （100MB，边界值）和 `fileService.validateFileSize(104857601)` （100MB+1字节） | `104857600`：无异常（边界值允许）；`104857601`：抛出业务异常，message 含"文件大小不能超过 100MB" | P0 | Unit |
| TC-F-052 | 单元 — 存储路径生成 | 无（Mock LocalDate 返回 2026-03） | 调用 `fileService.buildStoragePath("report.pdf")` | 返回路径格式为 `2026/03/{uuid}.pdf`；UUID 部分符合 UUID v4 格式（32位十六进制+连字符）；路径不含 `../` | P0 | Unit |
| TC-F-053 | 单元 — 路径穿越防护 | 无 | 调用 `fileService.buildStoragePath("../../../etc/passwd.pdf")` 和 `fileService.buildStoragePath("..\\windows\\system32.pdf")` | 两个调用返回的路径均不含 `../` 或 `..\`；路径格式仍为 `{year}/{month}/{uuid}.pdf` | P0 | Unit |
| TC-F-054 | 单元 — 描述长度校验 | 无 | 调用 `fileService.validateDescription("a".repeat(500))` 和 `fileService.validateDescription("a".repeat(501))` | `500字符`：无异常；`501字符`：抛出业务异常，message 含"描述不能超过 500 字符" | P1 | Unit |

---

## 十二、用例覆盖矩阵

| API 端点 | 覆盖用例 |
|---------|---------|
| `POST /api/files` | TC-F-001 ~ TC-F-017 |
| `GET /api/files` | TC-F-018 ~ TC-F-028 |
| `GET /api/files/{id}/download` | TC-F-029 ~ TC-F-031 |
| `DELETE /api/files/{id}` | TC-F-032 ~ TC-F-034 |
| `POST /api/files/{id}/tags` | TC-F-035, TC-F-038 |
| `DELETE /api/files/{id}/tags/{tagId}` | TC-F-036 |
| `DELETE /api/tags/{id}` (级联影响 file_tag) | TC-F-037 |
| 安全边界（API 层） | TC-F-039 ~ TC-F-043 |
| E2E 用户旅程 | TC-F-044 ~ TC-F-049 |
| FileService 单元测试 | TC-F-050 ~ TC-F-054 |

---

## 十三、边界用例覆盖确认

| 边界类型 | 覆盖用例 |
|---------|---------|
| Null / Undefined | TC-F-007（无描述/标签字段为 null）；TC-F-012（file 字段缺失） |
| 空值 | TC-F-010（0 字节空文件）；TC-F-024（空列表空态） |
| 非法类型 | TC-F-008（不支持扩展名）；TC-F-042（MIME 伪装）；TC-F-017（MIME 嗅探） |
| 边界值 | TC-F-013（100MB 临界值）；TC-F-016（description 500 字符）；TC-F-051（大小校验边界）；TC-F-025（分页 20 条） |
| 错误路径 | TC-F-009（超大文件）；TC-F-031（404 不存在）；TC-F-034（删除已删除）；TC-F-030（磁盘文件丢失） |
| 并发 | 单用户系统无并发专项用例；TC-F-015（同名文件不覆盖即为幂等上传验证） |
| 大数据量 | TC-F-028（超 20 条分页）；TC-F-038（标签数上限 20） |
| 特殊字符 | TC-F-014（中文/空格/括号文件名）；TC-F-039（路径穿越）；TC-F-040（SQL 注入）；TC-F-041（XSS）；TC-F-043（单元测试路径穿越） |
