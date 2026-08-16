import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

/**
 * 将日期格式化为相对时间字符串。
 *
 * @param {string|Date} date 日期字符串或 Date 对象
 * @returns {string} 相对时间字符串，如"刚刚"、"5分钟前"、"2小时前"等
 */
export function formatRelativeTime(date) {
  if (!date) return ''
  const d = dayjs(date)
  const now = dayjs()
  const diffMinutes = now.diff(d, 'minute')
  const diffHours = now.diff(d, 'hour')
  const diffDays = now.diff(d, 'day')

  if (diffMinutes < 1) return '刚刚'
  if (diffMinutes < 60) return `${diffMinutes}分钟前`
  if (diffHours < 24) return `${diffHours}小时前`
  if (d.year() === now.year()) return d.format('MM-DD HH:mm')
  return d.format('YYYY-MM-DD HH:mm')
}

export default dayjs
