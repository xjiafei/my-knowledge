-- Create database if not exists
CREATE DATABASE IF NOT EXISTS knowledge_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE knowledge_db;

-- 分类表
CREATE TABLE IF NOT EXISTS category (
    id         BIGINT       AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    name       VARCHAR(100) NOT NULL                  COMMENT '分类名称',
    parent_id  BIGINT       NULL                      COMMENT '父分类ID，NULL为顶级分类',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_parent_id (parent_id),
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES category(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分类表';

-- 标签表
CREATE TABLE IF NOT EXISTS tag (
    id         BIGINT      AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    name       VARCHAR(50) NOT NULL UNIQUE            COMMENT '标签名称，全局唯一',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签表';

-- 笔记表
CREATE TABLE IF NOT EXISTS note (
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
CREATE TABLE IF NOT EXISTS note_tag (
    note_id BIGINT NOT NULL COMMENT '笔记ID',
    tag_id  BIGINT NOT NULL COMMENT '标签ID',

    PRIMARY KEY (note_id, tag_id),
    INDEX idx_tag_id (tag_id),
    CONSTRAINT fk_notetag_note FOREIGN KEY (note_id) REFERENCES note(id) ON DELETE CASCADE,
    CONSTRAINT fk_notetag_tag  FOREIGN KEY (tag_id)  REFERENCES tag(id)  ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记-标签关联表';
