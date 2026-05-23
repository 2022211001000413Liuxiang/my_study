import type { Status } from './types';

export const statusLabel: Record<Status, string> = {
  learning: '学习中',
  reviewing: '复盘',
  done: '完成'
};

export function formatDate(value: string) {
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value));
}
