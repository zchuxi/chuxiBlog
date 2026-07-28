import { Marked } from 'marked'
import hljs from 'highlight.js'

function slugify(text, seen) {
  let id = String(text).trim().replace(/<[^>]+>/g, '').replace(/[\s]+/g, '-')
  if (seen.has(id)) {
    let i = 1
    while (seen.has(`${id}-${i}`)) i += 1
    id = `${id}-${i}`
  }
  seen.add(id)
  return id
}

export function renderMarkdown(md) {
  const seen = new Set()
  const headings = []
  const marked = new Marked({
    gfm: true,
    breaks: false
  })
  marked.use({
    renderer: {
      heading({ tokens, depth }) {
        const text = this.parser.parseInline(tokens)
        const raw = tokens.map(t => t.raw || '').join('') || text
        const id = slugify(raw.replace(/[#*`]/g, ''), seen)
        headings.push({ id, text: raw.replace(/[#*`]/g, '').trim(), level: depth })
        return `<h${depth} id="${id}">${text}</h${depth}>\n`
      },
      code({ text, lang }) {
        const language = lang && hljs.getLanguage(lang) ? lang : null
        const highlighted = language
          ? hljs.highlight(text, { language }).value
          : hljs.highlightAuto(text).value
        return `<pre><code class="hljs language-${language || 'plaintext'}">${highlighted}</code></pre>\n`
      },
      image({ href, title, text }) {
        return `<figure><img class="md-zoom" src="${href}" alt="${text || ''}"${title ? ` title="${title}"` : ''} loading="lazy"></figure>\n`
      }
    }
  })
  const html = marked.parse(md || '')
  return { html, headings }
}
