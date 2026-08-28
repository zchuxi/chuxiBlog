// P2-1: 给后台 Panel 的非 scoped <style> 补 scoped,并迁移 .admin-root 全局覆写
// 用法: 在 frontend 目录执行  node scripts/add-scoped-to-admin-panels.mjs
import { readFileSync, writeFileSync } from 'node:fs';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const adminCssPath = resolve(root, 'src/assets/css/admin.css');

const panels = [
  'AiConfigPanel', 'AppearancePanel', 'ArticlesPanel', 'BackgroundPanel',
  'BangumiPanel', 'NavMenuPanel', 'PageContentPanel', 'ScenePanel',
  'SiteContentPanel', 'SiteSettingsPanel',
];

// 保守策略:只给「style 体内完全不含 .admin-root 全局覆写」的 Panel 补 scoped。
// ArticlesPanel / SiteContentPanel 等含大量 .admin-root 穿透覆写,scoped 化会使其失效,
// 这类留待人工按「:deep() 或迁移 admin.css」逐条处理,不在本批自动改造范围。
const GLOBAL_OVERRIDE_RE = /\.admin-root/;

let skipped = [];

for (const p of panels) {
  const file = resolve(root, `src/views/admin/${p}.vue`);
  let src = readFileSync(file, 'utf8');

  // 提取 style 体,若含 .admin-root 穿透覆写则整个文件跳过(保守,不碰)
  const styleBody = (src.match(/<style[^>]*>([\s\S]*?)<\/style>/) || [])[1] || '';
  if (GLOBAL_OVERRIDE_RE.test(styleBody)) {
    skipped.push(p);
    console.log(`⊘ ${p}: 含 .admin-root 穿透覆写,跳过(留待人工处理)`);
    continue;
  }

  let changed = false;
  src = src.replace(/<style(?![^>]*\bscoped\b)([^>]*)>/g, (whole, attrs) => {
    changed = true;
    return `<style scoped${attrs}>`;
  });

  if (changed) {
    writeFileSync(file, src, 'utf8');
    console.log(`✔ ${p}: 补 scoped 完成`);
  } else {
    console.log(`- ${p}: 无需处理`);
  }
}

if (skipped.length) console.log(`\n跳过 ${skipped.length} 个: ${skipped.join(', ')}`);
