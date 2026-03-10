#!/bin/bash
# 阶段完成时调用 — 自动存档当前变更
echo "=== 阶段完成，存档变更 ==="
git add -A
git status --short
echo "---"
echo "请 commit 并等待架构师审批。"
