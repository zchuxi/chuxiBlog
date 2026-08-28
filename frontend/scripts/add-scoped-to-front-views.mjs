// P2-2: 给前台视图的非 scoped <style> 补 scoped
// 用法: 在 frontend 目录执行  node scripts/add-scoped-to-front-views.mjs
import { readFileSync, writeFileSync } from 'node:fs';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const views = ['AboutView','BangumiDetailView','BangumiView','CalendarView','ToolDetailView','ToolView','TreeHoleView'];

for (const v of views) {
  const file = resolve(root, `src/views/${v}.vue`);
  let src = readFileSync(file, 'utf8');
  let changed = false;
  src = src.replace(/<style(?![^>]*\bscoped\b)([^>]*)>/g, (whole, attrs) => {
    changed = true;
    return `<style scoped${attrs}>`;
  });
  if (changed) { writeFileSync(file, src, 'utf8'); console.log(`✔ ${v}`); }
  else console.log(`- ${v}: 无需处理`);
}
