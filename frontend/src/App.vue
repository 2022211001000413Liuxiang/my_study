<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { BookOpen, CalendarCheck, Edit3 } from '@lucide/vue';
import { api } from './api';
import AdminStudio from './components/AdminStudio.vue';
import InsightPanel from './components/InsightPanel.vue';
import LoginGate from './components/LoginGate.vue';
import NoteList from './components/NoteList.vue';
import ReaderPane from './components/ReaderPane.vue';
import ReviewPane from './components/ReviewPane.vue';
import SidebarNav from './components/SidebarNav.vue';
import type {
  Category,
  Note,
  ReviewFilter,
  ReviewLevel,
  ReviewOverview,
  ReviewQueue,
  Stats,
  Status,
  ViewMode
} from './types';

const emptyStats: Stats = { total: 0, words: 0, favorites: 0, dueReviews: 0, tags: [] };
const emptyReviewQueue: ReviewQueue = { notes: [], dueCount: 0, reviewedToday: 0, nextReviewAt: null };
const emptyReviewOverview: ReviewOverview = {
  dueToday: 0,
  reviewedToday: 0,
  levels: { unreviewed: 0, again: 0, normal: 0, easy: 0, scheduled: 0 },
  completedDays: [],
  upcomingDays: [],
  recentReviewed: []
};
type NoteDetail = { note: Note; markdown: string; rawMarkdown: string };

const notes = ref<Note[]>([]);
const categories = ref<Category[]>([]);
const stats = ref<Stats>(emptyStats);
const reviewQueue = ref<ReviewQueue>(emptyReviewQueue);
const reviewOverview = ref<ReviewOverview>(emptyReviewOverview);
const selectedId = ref('');
const selectedCategory = ref('all');
const selectedReview = ref<ReviewFilter>('all');
const query = ref('');
const markdown = ref('');
const rawMarkdown = ref('');
const currentNote = ref<Note | null>(null);
const mode = ref<ViewMode>('reader');
const token = ref(localStorage.getItem('study-token') || '');
const password = ref('');
const editorText = ref('');
const draftTitle = ref('');
const draftCategory = ref('');
const draftTags = ref('');
const draftStatus = ref<Status>('learning');
const draftFavorite = ref(false);
const notice = ref('');
const busy = ref(false);
const reviewBusy = ref(false);
const notesLoading = ref(false);
const streamCollapsed = ref(false);
const inspectorCollapsed = ref(false);
const noteCache = new Map<string, NoteDetail>();

const topCategories = computed(() => [{ name: 'all', count: stats.value.total }, ...categories.value]);
const recent = computed(() => notes.value.slice(0, 5));
const activeReviewNote = computed(() => reviewQueue.value.notes[0] || null);

let debounceHandle = 0;
let noteRequestSerial = 0;
let notesRequestSerial = 0;

onMounted(() => {
  loadInitialData().catch(showError);
});

watch(query, () => {
  window.clearTimeout(debounceHandle);
  debounceHandle = window.setTimeout(() => {
    loadNotes(selectedCategory.value, query.value).catch(showError);
  }, 180);
});

watch(selectedCategory, () => {
  window.clearTimeout(debounceHandle);
  loadNotes(selectedCategory.value, query.value).catch(showError);
});

watch(selectedReview, () => {
  window.clearTimeout(debounceHandle);
  loadNotes(selectedCategory.value, query.value, selectedReview.value).catch(showError);
});

watch(selectedId, (id) => {
  if (id) {
    loadNote(id).catch(showError);
  }
});

watch(mode, (nextMode) => {
  if (nextMode === 'review') {
    focusReviewNote();
    loadReviewQueue().catch(showError);
  }
});

watch(
  activeReviewNote,
  () => {
    if (mode.value === 'review') {
      focusReviewNote();
    }
  }
);

async function loadInitialData() {
  await Promise.all([loadNotes(), loadReviewQueue(), loadReviewOverview()]);
}

async function loadNotes(
  nextCategory = selectedCategory.value,
  nextQuery = query.value,
  nextReview = selectedReview.value
) {
  const requestId = ++notesRequestSerial;
  notesLoading.value = true;
  try {
    const data = await api.notes(nextCategory, nextQuery, nextReview);
    if (requestId !== notesRequestSerial) return;
    notes.value = data.notes;
    categories.value = data.categories;
    stats.value = data.stats;
    if (!selectedId.value && data.notes[0]) {
      selectedId.value = data.notes[0].id;
    }
    if (selectedId.value && !data.notes.some((note) => note.id === selectedId.value)) {
      selectedId.value = data.notes[0]?.id || '';
    }
  } finally {
    if (requestId === notesRequestSerial) {
      notesLoading.value = false;
    }
  }
}

async function loadReviewQueue() {
  reviewQueue.value = await api.reviewToday();
}

async function loadReviewOverview() {
  reviewOverview.value = await api.reviewOverview();
}

async function loadNote(id: string) {
  const requestId = ++noteRequestSerial;
  const cached = noteCache.get(id);
  if (cached) {
    applyNoteDetail(cached);
    return;
  }
  const data = await api.note(id);
  if (requestId !== noteRequestSerial) return;
  noteCache.set(id, data);
  applyNoteDetail(data);
}

function applyNoteDetail(data: NoteDetail) {
  currentNote.value = data.note;
  markdown.value = data.markdown;
  rawMarkdown.value = data.rawMarkdown;
  editorText.value = data.rawMarkdown;
  draftTitle.value = data.note.title;
  draftCategory.value = data.note.category;
  draftTags.value = data.note.tags.join(', ');
  draftStatus.value = data.note.status;
  draftFavorite.value = data.note.favorite;
}

async function login() {
  busy.value = true;
  try {
    const data = await api.login(password.value);
    token.value = data.token;
    localStorage.setItem('study-token', data.token);
    password.value = '';
    toast('已进入管理后台');
  } catch (error) {
    showError(error);
  } finally {
    busy.value = false;
  }
}

async function saveNote() {
  if (!currentNote.value) return;
  busy.value = true;
  try {
    noteCache.delete(currentNote.value.id);
    await api.save(token.value, currentNote.value.id, {
      markdown: editorText.value,
      meta: {
        favorite: draftFavorite.value,
        status: draftStatus.value,
        tags: draftTags.value.split(/[,，\s]+/).map((tag) => tag.trim()).filter(Boolean)
      }
    });
    toast('笔记已保存');
    await refreshAfterContentChange();
    await loadNote(currentNote.value.id);
  } catch (error) {
    showError(error);
  } finally {
    busy.value = false;
  }
}

async function moveNote() {
  if (!currentNote.value) return;
  busy.value = true;
  try {
    noteCache.delete(currentNote.value.id);
    const data = await api.move(token.value, currentNote.value.id, draftTitle.value, draftCategory.value);
    toast('位置已更新');
    await refreshAfterContentChange();
    selectedId.value = data.note.id;
  } catch (error) {
    showError(error);
  } finally {
    busy.value = false;
  }
}

async function createNote() {
  busy.value = true;
  try {
    const category = selectedCategory.value === 'all' ? 'inbox' : selectedCategory.value;
    const data = await api.create(token.value, '新的学习记录', category);
    toast('新笔记已创建');
    await refreshAfterContentChange();
    selectedId.value = data.note.id;
    mode.value = 'admin';
  } catch (error) {
    showError(error);
  } finally {
    busy.value = false;
  }
}

async function deleteNote() {
  if (!currentNote.value) return;
  const confirmed = window.confirm(`删除《${currentNote.value.title}》？这个操作会删除 md 文件。`);
  if (!confirmed) return;
  busy.value = true;
  try {
    noteCache.delete(currentNote.value.id);
    await api.delete(token.value, currentNote.value.id);
    toast('笔记已删除');
    currentNote.value = null;
    markdown.value = '';
    selectedId.value = '';
    await refreshAfterContentChange();
  } catch (error) {
    showError(error);
  } finally {
    busy.value = false;
  }
}

async function submitReview(level: ReviewLevel) {
  const note = activeReviewNote.value;
  if (!note || reviewBusy.value) return;
  reviewBusy.value = true;
  try {
    noteCache.delete(note.id);
    reviewQueue.value = await api.submitReview(note.id, level);
    await Promise.all([loadNotes(), loadReviewOverview()]);
    focusReviewNote();
    toast('复习进度已记录');
  } catch (error) {
    showError(error);
  } finally {
    reviewBusy.value = false;
  }
}

async function refreshAfterContentChange() {
  await Promise.all([loadNotes(), loadReviewQueue(), loadReviewOverview()]);
}

function focusReviewNote() {
  const note = activeReviewNote.value;
  if (note && selectedId.value !== note.id) {
    selectedId.value = note.id;
  }
}

function selectNote(id: string) {
  if (mode.value === 'review') {
    focusReviewNote();
    return;
  }
  selectedId.value = id;
}

function toggleStream() {
  streamCollapsed.value = !streamCollapsed.value;
}

function toggleInspector() {
  inspectorCollapsed.value = !inspectorCollapsed.value;
}

function showError(error: unknown) {
  toast(error instanceof Error ? error.message : '操作失败');
}

function toast(message: string) {
  notice.value = message;
  window.setTimeout(() => {
    if (notice.value === message) notice.value = '';
  }, 2600);
}
</script>

<template>
  <main class="app-shell">
    <div class="atmosphere" />
    <SidebarNav
      v-model:query="query"
      v-model:selected-category="selectedCategory"
      :categories="topCategories"
      :stats="stats"
    />

    <section class="workbench">
      <header class="topbar">
        <div>
          <p class="eyebrow">Study Workbench</p>
          <h1>{{ currentNote?.title || '正在建立索引' }}</h1>
        </div>
        <div class="mode-switch" role="tablist" aria-label="视图切换">
          <button :class="{ active: mode === 'reader' }" @click="mode = 'reader'">
            <BookOpen :size="17" /> 阅读
          </button>
          <button :class="{ active: mode === 'review' }" @click="mode = 'review'">
            <CalendarCheck :size="17" /> 复习
            <b v-if="stats.dueReviews">{{ stats.dueReviews }}</b>
          </button>
          <button :class="{ active: mode === 'admin' }" @click="mode = 'admin'">
            <Edit3 :size="17" /> 管理
          </button>
        </div>
      </header>

      <section
        class="study-grid"
        :class="{
          'stream-collapsed': streamCollapsed,
          'inspector-collapsed': inspectorCollapsed
        }"
      >
        <NoteList
          :notes="notes"
          :selected-id="selectedId"
          :review-filter="selectedReview"
          :collapsed="streamCollapsed"
          :loading="notesLoading"
          :review-overview="reviewOverview"
          @select="selectNote"
          @update:review-filter="selectedReview = $event"
          @toggle="toggleStream"
        />

        <article class="main-panel">
          <ReaderPane
            v-if="mode === 'reader'"
            :note="currentNote"
            :markdown="markdown"
          />
          <ReviewPane
            v-else-if="mode === 'review'"
            :queue="reviewQueue"
            :overview="reviewOverview"
            :note="currentNote"
            :markdown="markdown"
            :busy="reviewBusy"
            @feedback="submitReview"
          />
          <AdminStudio
            v-else-if="token"
            :note="currentNote"
            v-model:editor-text="editorText"
            v-model:draft-title="draftTitle"
            v-model:draft-category="draftCategory"
            v-model:draft-tags="draftTags"
            v-model:draft-status="draftStatus"
            v-model:draft-favorite="draftFavorite"
            :busy="busy"
            @save="saveNote"
            @move="moveNote"
            @create="createNote"
            @delete="deleteNote"
          />
          <LoginGate
            v-else
            v-model:password="password"
            :busy="busy"
            @login="login"
          />
        </article>

        <InsightPanel
          :stats="stats"
          :recent="recent"
          :note="currentNote"
          :collapsed="inspectorCollapsed"
          @select="selectNote"
          @toggle="toggleInspector"
        />
      </section>
    </section>

    <div v-if="notice" class="toast">{{ notice }}</div>
  </main>
</template>
