-- H2-compatible schema (MySQL-specific syntax removed)
-- Used only for QA/testing with the h2 Spring profile

-- 分类表
CREATE TABLE IF NOT EXISTS category (
    id         BIGINT       AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    parent_id  BIGINT       NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES category(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_category_parent_id ON category (parent_id);

-- 标签表
CREATE TABLE IF NOT EXISTS tag (
    id         BIGINT      AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(50) NOT NULL,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_tag_name UNIQUE (name)
);

-- 笔记表
CREATE TABLE IF NOT EXISTS note (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(200) NOT NULL,
    content     CLOB,
    summary     VARCHAR(200),
    category_id BIGINT       NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_note_category FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_note_category_id ON note (category_id);
CREATE INDEX IF NOT EXISTS idx_note_updated_at  ON note (updated_at);
CREATE INDEX IF NOT EXISTS idx_note_created_at  ON note (created_at);
CREATE INDEX IF NOT EXISTS idx_note_title       ON note (title);

-- 笔记-标签关联表
CREATE TABLE IF NOT EXISTS note_tag (
    note_id BIGINT NOT NULL,
    tag_id  BIGINT NOT NULL,

    PRIMARY KEY (note_id, tag_id),
    CONSTRAINT fk_notetag_note FOREIGN KEY (note_id) REFERENCES note(id) ON DELETE CASCADE,
    CONSTRAINT fk_notetag_tag  FOREIGN KEY (tag_id)  REFERENCES tag(id)  ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_note_tag_tag_id ON note_tag (tag_id);
