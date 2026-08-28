## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).

## 文档沉淀

- 优化方案、技术债审视、安全加固建议、性能、可靠性、可观测性与交付改进建议必须写入 `docs/optimizations/`，不得只保留在会话记录中。
- 新文档使用 `YYYY-MM-DD-主题.md` 命名，并遵循 `docs/optimizations/README.md` 的内容和状态维护约定。
