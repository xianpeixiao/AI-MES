import { marked } from 'marked'

marked.setOptions({
  breaks: true,
  gfm: true
})

/** 仅匹配 ASCII URL，避免把 💡、中文标题吞进链接 */
const URL_PATTERN = /https?:\/\/[A-Za-z0-9._~:/?#\[\]@!$&'()*+,;=%-]+/g
const MARKDOWN_LINK_PATTERN = /\[([^\]]*)\]\((https?:\/\/[^)\s]+)\)/g
const SECTION_EMOJI = '📌|🔍|💡|🛠️|📊|📋|🎯|⚠️|🧭|📤'

function trimUrl(url: string) {
  return url.replace(/[.,;:!?]+$/g, '')
}

function linkifySegment(segment: string) {
  return segment.replace(URL_PATTERN, (url) => {
    const cleaned = trimUrl(url)
    return `[${cleaned}](${cleaned})`
  })
}

function protectUrls(text: string) {
  const urls: string[] = []
  const protectedText = text.replace(URL_PATTERN, (url) => {
    urls.push(url)
    return `\u0000URL${urls.length - 1}\u0000`
  })
  return { protectedText, urls }
}

function restoreUrls(text: string, urls: string[]) {
  return text.replace(/\u0000URL(\d+)\u0000/g, (_, index) => urls[Number(index)] ?? '')
}

function normalizeLists(text: string) {
  let next = text
  next = next.replace(/([。；;！!])\s*-(?=\S)/g, '$1\n- ')
  next = next.replace(/([\u4e00-\u9fff])-(?=[\u4e00-\u9fff])/g, '$1\n- ')
  next = next.replace(/(^|\n)\s*-(?=[^\s\-|*])/g, '$1- ')
  return next
}

function normalizeChatMarkdown(text: string) {
  let next = text.replace(/\r\n/g, '\n').replace(/\u00a0/g, ' ')

  next = next.replace(/(https?:\/\/[A-Za-z0-9._~:/?#\[\]@!$&'()*+,;=%-]+)(?=[\u4e00-\u9fff]|📌|🔍|💡|🛠️|📊|📋|🎯|⚠️)/g, '$1\n\n')
  next = next.replace(new RegExp(`([^\\n])\\s*(?=((?:${SECTION_EMOJI})\\s*\\*?\\*?))`, 'g'), '$1\n\n')
  next = next.replace(/(https?:\/\/[^\s]+)\s+(?=https?:\/\/)/g, '$1\n')
  next = next.replace(/[ \t]*[-*]?[ \t]*(Gitee|GitHub|Github|码云)\s*[:：]\s*(https?:\/\/[^\s]+)/gi, '\n- $1：$2')

  const { protectedText, urls } = protectUrls(next)
  next = restoreUrls(normalizeLists(protectedText), urls)
  return next.trim()
}

function preprocessMarkdown(text: string) {
  if (!text) return ''

  const normalized = normalizeChatMarkdown(text)
  let result = ''
  let lastIndex = 0
  let match: RegExpExecArray | null
  const pattern = new RegExp(MARKDOWN_LINK_PATTERN.source, 'g')

  while ((match = pattern.exec(normalized)) !== null) {
    result += linkifySegment(normalized.slice(lastIndex, match.index))
    result += match[0]
    lastIndex = match.index + match[0].length
  }

  result += linkifySegment(normalized.slice(lastIndex))
  return result
}

function enhanceLinks(html: string) {
  return html.replace(/<a\s+/g, '<a target="_blank" rel="noopener noreferrer" ')
}

/** 将 AI 回复渲染为 HTML；单换行转 <br>，并确保链接可点击 */
export function renderChatMarkdown(text: string) {
  const html = marked.parse(preprocessMarkdown(text)) as string
  return enhanceLinks(html)
}

/** 委托点击 Markdown 区域中的外链 */
export function openMarkdownLink(event: MouseEvent) {
  const target = event.target
  if (!(target instanceof Element)) return
  const anchor = target.closest('a')
  if (!anchor || !anchor.getAttribute('href')) return
  event.preventDefault()
  event.stopPropagation()
  window.open(anchor.href, '_blank', 'noopener,noreferrer')
}
