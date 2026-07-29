import { Marked } from 'marked'
import DOMPurify from 'dompurify'
// 按需注册语言：全量 barrel 会打进约 1MB（190 种语言），这里只留博客实际会用到的
import hljs from 'highlight.js/lib/core'
import javascript from 'highlight.js/lib/languages/javascript'
import typescript from 'highlight.js/lib/languages/typescript'
import xml from 'highlight.js/lib/languages/xml'
import css from 'highlight.js/lib/languages/css'
import json from 'highlight.js/lib/languages/json'
import bash from 'highlight.js/lib/languages/bash'
import java from 'highlight.js/lib/languages/java'
import python from 'highlight.js/lib/languages/python'
import sql from 'highlight.js/lib/languages/sql'
import yaml from 'highlight.js/lib/languages/yaml'
import markdown from 'highlight.js/lib/languages/markdown'
import go from 'highlight.js/lib/languages/go'

for (const [name, lang] of Object.entries({
  javascript, typescript, xml, css, json, bash, java, python, sql, yaml, markdown, go
})) {
  hljs.registerLanguage(name, lang)
}
// 常见别名
hljs.registerAliases(['js', 'jsx'], { languageName: 'javascript' })
hljs.registerAliases(['ts', 'tsx'], { languageName: 'typescript' })
hljs.registerAliases(['html', 'vue'], { languageName: 'xml' })
hljs.registerAliases(['sh', 'shell', 'zsh'], { languageName: 'bash' })
hljs.registerAliases(['yml'], { languageName: 'yaml' })
hljs.registerAliases(['md'], { languageName: 'markdown' })

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
  const rawHtml = marked.parse(md || '')
  const html = DOMPurify.sanitize(rawHtml)
  return { html, headings }
}
