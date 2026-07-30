#!/bin/sh
# 机械检查点（约定见 AGENTS.md 第 2 条 / README「线上 schema 变更流程（审阅点）」）：
# 从标准输入读取 git diff --name-status 输出（每行：状态<TAB>路径，重命名行为 状态<TAB>旧路径<TAB>新路径），
# 若变更涉及 backend/src/main/java/com/chuxi/entity/（含删除/重命名），
# 则同一变更必须包含 scripts/ 目录下新增或修改的 .sql 文件，否则失败。
# 本脚本只做机械校验，不替代人工审阅，也不改变现有 mvn test 步骤。
# 本地模拟：git diff --name-status <base> HEAD | sh scripts/ci/check-entity-ddl.sh
changed=$(cat)

# 展开所有涉及路径（重命名行的旧/新路径都算实体目录改动）
paths=$(printf '%s\n' "$changed" | cut -f2- | tr '\t' '\n')
entity=$(printf '%s\n' "$paths" | grep '^backend/src/main/java/com/chuxi/entity/' || true)
if [ -z "$entity" ]; then
  echo "[entity-ddl-check] 未检测到实体目录改动，跳过"
  exit 0
fi

echo "[entity-ddl-check] 检测到实体改动："
printf '%s\n' "$entity" | sed 's/^/  - /'

# 仅统计新增/修改/复制/重命名后的 scripts/ 下 .sql（纯删除 .sql 不算随附 DDL）
sql=$(printf '%s\n' "$changed" | awk -F'\t' '$1 ~ /^[AMCR]/ { print $NF }' | grep '^scripts/.*\.sql$' || true)
if [ -n "$sql" ]; then
  echo "[entity-ddl-check] 同一变更包含 scripts/ 下新增或修改的 .sql，通过："
  printf '%s\n' "$sql" | sed 's/^/  - /'
  exit 0
fi

echo "[entity-ddl-check] 失败：实体变更须随附 DDL 供人工审阅（请在 scripts/ 下新增或修改对应 .sql 后重新提交）" >&2
exit 1
