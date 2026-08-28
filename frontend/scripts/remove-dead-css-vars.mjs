// P0 清理脚本：从 base.css 中移除未被任何地方引用的 CSS 变量声明
// 用法：在 frontend 目录下执行  node scripts/remove-dead-css-vars.mjs
import { readFileSync, writeFileSync } from 'node:fs';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import postcss from 'postcss';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const target = resolve(root, 'src/assets/css/base.css');

// 与体检结论保持一致的死变量清单（全工程确认仅 base.css 自身出现）
// 仅删除「从 Tailwind 预检样式抄来的 --tw-* 死变量」。
// --bg-color / --scrollbar-thumb-hover / --viewport-reveal-angle 等虽在其它文件中无引用，
// 但在 base.css 自身有 var() 使用（真定义+真使用），不属于死变量，保留。
const twVars = [
  '--tw-backdrop-blur', '--tw-backdrop-brightness', '--tw-backdrop-contrast',
  '--tw-backdrop-grayscale', '--tw-backdrop-hue-rotate', '--tw-backdrop-invert',
  '--tw-backdrop-opacity', '--tw-backdrop-saturate', '--tw-backdrop-sepia',
  '--tw-blur', '--tw-border-spacing-x', '--tw-border-spacing-y', '--tw-brightness',
  '--tw-contain-layout', '--tw-contain-paint', '--tw-contain-size', '--tw-contain-style',
  '--tw-content', '--tw-contrast', '--tw-drop-shadow', '--tw-gradient-from-position',
  '--tw-gradient-to-position', '--tw-gradient-via-position', '--tw-grayscale',
  '--tw-hue-rotate', '--tw-invert', '--tw-numeric-figure', '--tw-numeric-fraction',
  '--tw-numeric-spacing', '--tw-ordinal', '--tw-pan-x', '--tw-pan-y', '--tw-pinch-zoom',
  '--tw-ring-color', '--tw-ring-inset', '--tw-ring-offset-color', '--tw-ring-offset-shadow',
  '--tw-ring-offset-width', '--tw-ring-shadow', '--tw-rotate', '--tw-saturate',
  '--tw-scale-x', '--tw-scale-y', '--tw-scroll-snap-strictness', '--tw-sepia',
  '--tw-shadow', '--tw-shadow-colored', '--tw-skew-x', '--tw-skew-y',
  '--tw-slashed-zero', '--tw-translate-x', '--tw-translate-y',
];
const deadVars = new Set(twVars);

const css = readFileSync(target, 'utf8');
const ast = postcss.parse(css);

let removed = 0;
ast.walkDecls((decl) => {
  const prop = decl.prop.trim();
  if (deadVars.has(prop)) {
    decl.remove();
    removed += 1;
  }
});

const out = ast.toString();
writeFileSync(target, out, 'utf8');
console.log(`removed ${removed} declarations from base.css`);
console.log(`size: ${css.length} -> ${out.length} bytes`);
