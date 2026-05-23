<script setup lang="ts">
import { ChevronRight, Heart, LayoutDashboard, PanelLeftClose, PanelLeftOpen } from '@lucide/vue';
import type { Note, ReviewFilter, ReviewOverview } from '../types';
import { formatDate, statusLabel } from '../utils';

const props = defineProps<{
  notes: Note[];
  selectedId: string;
  reviewFilter: ReviewFilter;
  reviewOverview: ReviewOverview;
  collapsed: boolean;
  loading: boolean;
}>();

defineEmits<{
  select: [id: string];
  'update:reviewFilter': [filter: ReviewFilter];
  toggle: [];
}>();

const reviewFilters: Array<{ value: ReviewFilter; label: string; count: () => number }> = [
  { value: 'all', label: '全部', count: () => props.reviewOverview.dueToday + props.reviewOverview.levels.scheduled },
  { value: 'due', label: '今日待复习', count: () => props.reviewOverview.dueToday },
  { value: 'unreviewed', label: '未复习', count: () => props.reviewOverview.levels.unreviewed },
  { value: 'again', label: '不熟', count: () => props.reviewOverview.levels.again },
  { value: 'normal', label: '一般', count: () => props.reviewOverview.levels.normal },
  { value: 'easy', label: '熟悉', count: () => props.reviewOverview.levels.easy },
  { value: 'scheduled', label: '已安排', count: () => props.reviewOverview.levels.scheduled }
];
</script>

<template>
  <section class="note-stream" :class="{ collapsed, loading }">
    <div class="panel-heading">
      <span class="panel-title">
        <LayoutDashboard :size="17" />
        <span>学习流</span>
      </span>
      <div class="panel-actions">
        <Transition name="fade-soft" mode="out-in">
          <small v-if="!collapsed" :key="notes.length">{{ notes.length }} 篇</small>
        </Transition>
        <button
          class="icon-button"
          type="button"
          :aria-label="collapsed ? '展开学习流' : '收起学习流'"
          :title="collapsed ? '展开学习流' : '收起学习流'"
          :aria-expanded="!collapsed"
          @click="$emit('toggle')"
        >
          <Transition name="icon-swap" mode="out-in">
            <PanelLeftOpen v-if="collapsed" key="open" :size="16" />
            <PanelLeftClose v-else key="close" :size="16" />
          </Transition>
        </button>
      </div>
    </div>

    <Transition name="panel-swap" mode="out-in">
      <button
        v-if="collapsed"
        key="rail"
        class="collapsed-rail"
        type="button"
        title="展开学习流"
        @click="$emit('toggle')"
      >
        <span>学习流</span>
        <strong>{{ notes.length }}</strong>
        <small>笔记</small>
      </button>

      <div v-else key="list" class="note-list-wrap">
        <div class="review-filter" aria-label="掌握度筛选">
          <button
            v-for="filter in reviewFilters"
            :key="filter.value"
            :class="{ active: reviewFilter === filter.value }"
            type="button"
            @click="$emit('update:reviewFilter', filter.value)"
          >
            <span>{{ filter.label }}</span>
            <b>{{ filter.count() }}</b>
          </button>
        </div>

        <div class="note-list">
          <button
            v-for="(note, index) in notes"
            :key="note.id"
            class="note-row"
            :class="{ selected: note.id === selectedId }"
            :style="{ '--row-delay': `${Math.min(index, 11) * 20}ms` }"
            @click="$emit('select', note.id)"
          >
            <span class="status-dot" :data-status="note.status" :title="statusLabel[note.status]" />
            <span class="note-copy">
              <strong>{{ note.title }}</strong>
              <small>{{ note.category }} · {{ formatDate(note.updatedAt) }} · {{ note.wordCount.toLocaleString() }} 字</small>
            </span>
            <Heart v-if="note.favorite" :size="15" class="heart" fill="currentColor" />
            <ChevronRight :size="17" />
          </button>
          <span v-if="loading" class="loading-sheen" aria-hidden="true" />
          <div v-if="!notes.length" class="soft-empty">没有匹配的笔记</div>
        </div>
      </div>
    </Transition>
  </section>
</template>
