import type { Note, NotesResponse, ReviewFilter, ReviewLevel, ReviewOverview, ReviewQueue, Status } from './types';

const baseUrl = import.meta.env.VITE_API_BASE || '';

async function request<T>(url: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(`${baseUrl}${url}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(init.headers || {})
    }
  });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(data.error || `请求失败 ${response.status}`);
  }
  return data as T;
}

function auth(token: string, init: RequestInit = {}) {
  return {
    ...init,
    headers: {
      Authorization: `Bearer ${token}`,
      ...(init.headers || {})
    }
  };
}

export const api = {
  notes(category: string, query: string, review: ReviewFilter = 'all') {
    const params = new URLSearchParams();
    if (category && category !== 'all') params.set('category', category);
    if (query.trim()) params.set('q', query.trim());
    if (review !== 'all') params.set('review', review);
    return request<NotesResponse>(`/api/notes?${params.toString()}`);
  },
  note(id: string) {
    return request<{ note: Note; markdown: string; rawMarkdown: string }>(`/api/notes/${id}`);
  },
  reviewToday() {
    return request<ReviewQueue>('/api/reviews/today');
  },
  reviewOverview() {
    return request<ReviewOverview>('/api/reviews/overview');
  },
  submitReview(noteId: string, level: ReviewLevel) {
    return request<ReviewQueue>(`/api/reviews/${noteId}`, {
      method: 'POST',
      body: JSON.stringify({ level })
    });
  },
  login(password: string) {
    return request<{ token: string }>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ password })
    });
  },
  create(token: string, title: string, category: string) {
    return request<{ note: Note }>('/api/notes', auth(token, {
      method: 'POST',
      body: JSON.stringify({ title, category })
    }));
  },
  save(token: string, noteId: string, payload: {
    markdown: string;
    meta: { favorite: boolean; status: Status; tags: string[] };
  }) {
    return request<{ note: Note }>(`/api/notes/${noteId}`, auth(token, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }));
  },
  move(token: string, noteId: string, title: string, category: string) {
    return request<{ note: Note }>(`/api/notes/${noteId}/move`, auth(token, {
      method: 'PATCH',
      body: JSON.stringify({ title, category })
    }));
  },
  delete(token: string, noteId: string) {
    return request<{ ok: boolean }>(`/api/notes/${noteId}`, auth(token, { method: 'DELETE' }));
  }
};
