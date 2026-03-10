# docs/specs — 需求与设计文档

## 全量文档
全量需求、产品、技术设计文档直接放在本目录下：
- `requirements.md` — 需求规格
- `product.md` — 产品设计
- `tech.md` — 技术设计

## 增量特性文档
每个增量特性的文档放在独立子目录，命名格式：`featureXXX-specs/`

示例：
```
docs/specs/
├── requirements.md          # 全量需求
├── product.md               # 全量产品设计
├── tech.md                  # 全量技术设计
├── feature001-specs/        # 特性 001
│   ├── requirements.md
│   ├── product.md
│   └── tech.md
└── feature002-specs/        # 特性 002
    ├── requirements.md
    ├── product.md
    └── tech.md
```

## 合并规则
- 特性交付后，将 feature spec 内容合并回全量文档
- **保留 feature 目录用于追溯**，不删除
