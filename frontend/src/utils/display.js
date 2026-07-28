// 与原站一致的展示辅助：标签色板、封面回退、日期格式
export const TAG_PALETTES = [
  { textColor: '#8b5b1d', borderColor: 'rgba(229, 180, 113, 0.42)', backgroundColor: 'rgba(255, 247, 233, 0.92)' },
  { textColor: '#2f5f50', borderColor: 'rgba(110, 177, 155, 0.38)', backgroundColor: 'rgba(237, 248, 243, 0.92)' },
  { textColor: '#3e5877', borderColor: 'rgba(126, 160, 198, 0.36)', backgroundColor: 'rgba(239, 245, 252, 0.92)' },
  { textColor: '#7a4a44', borderColor: 'rgba(214, 152, 143, 0.36)', backgroundColor: 'rgba(252, 240, 238, 0.92)' }
]

export function tagPaletteStyle(name) {
  let hash = 0
  for (const ch of String(name || '')) hash = (hash * 31 + ch.charCodeAt(0)) >>> 0
  const p = TAG_PALETTES[hash % TAG_PALETTES.length]
  return {
    '--lx-tag-text': p.textColor,
    '--lx-tag-border': p.borderColor,
    '--lx-tag-background': p.backgroundColor
  }
}

export const FALLBACK_COVERS = [
  '/image/bg/Landscape/01.webp',
  '/image/bg/Landscape/05.webp',
  '/image/bg/Landscape/08.webp',
  '/image/bg/Landscape/10.webp',
  '/image/bg/Landscape/12.webp',
  '/image/bg/Landscape/13.webp'
]

export function coverOf(article, index = 0) {
  if (article && article.coverUrl) return article.coverUrl
  return FALLBACK_COVERS[index % FALLBACK_COVERS.length]
}

export const HERO_GRADIENTS = [
  'linear-gradient(135deg, rgba(86, 126, 173, 0.94), rgba(120, 205, 214, 0.78))',
  'linear-gradient(135deg, rgba(233, 151, 93, 0.9), rgba(255, 217, 146, 0.72))',
  'linear-gradient(135deg, rgba(115, 98, 216, 0.88), rgba(138, 196, 255, 0.72))'
]

export const FOLD_TEXT_COLORS = ['#d4f1ff', '#dbfff2', '#f7e0ff', '#ffe2cb', '#ffeec1', '#dbe4ff']

export function mmdd(dateStr) {
  if (!dateStr) return ''
  const s = String(dateStr)
  return `${s.slice(5, 7)}/${s.slice(8, 10)}`
}

export function ymd(dateStr) {
  return dateStr ? String(dateStr).slice(0, 10) : ''
}
