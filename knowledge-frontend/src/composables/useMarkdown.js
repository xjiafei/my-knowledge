import { marked } from 'marked'
import { markedHighlight } from 'marked-highlight'
import hljs from 'highlight.js'
import DOMPurify from 'dompurify'

// Use marked.use() to inject code highlighting extension (marked v5+ standard approach)
marked.use(markedHighlight({
  langPrefix: 'hljs language-',
  highlight(code, lang) {
    const language = hljs.getLanguage(lang) ? lang : 'plaintext'
    return hljs.highlight(code, { language }).value
  }
}))

marked.use({ gfm: true, breaks: true })

/**
 * Render markdown text to sanitized HTML
 * @param {string} text - Markdown source text
 * @returns {string} - Sanitized HTML
 */
export function renderMarkdown(text) {
  if (!text) return ''
  const html = marked(text)
  return DOMPurify.sanitize(html)
}

export default { renderMarkdown }
