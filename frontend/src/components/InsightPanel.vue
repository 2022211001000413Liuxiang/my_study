<script setup lang="ts">
import { BookOpen, CalendarCheck, Clock3, PanelRightClose, PanelRightOpen, Tag } from '@lucide/vue';
import type { Note, Stats } from '../types';
import { formatDate } from '../utils';

defineProps<{
  stats: Stats;
  recent: Note[];
  note: Note | null;
  collapsed: boolean;
}>();

defineEmits<{
  select: [id: string];
  toggle: [];
}>();
</script>

<template>
  <aside class="inspector" :class="{ collapsed }">
    <div class="panel-heading">
      <span class="panel-title">
        <BookOpen :size="16" />
        <span>目录</span>
      </span>
      <button
        class="icon-button"
        type="button"
        :aria-label="collapsed ? '展开目录' : '收起目录'"
        :title="collapsed ? '展开目录' : '收起目录'"
        :aria-expanded="!collapsed"
        @click="$emit('toggle')"
      >
        <Transition name="icon-swap" mode="out-in">
          <PanelRightOpen v-if="collapsed" key="open" :size="16" />
          <PanelRightClose v-else key="close" :size="16" />
        </Transition>
      </button>
    </div>

    <Transition name="panel-swap" mode="out-in">
      <button
        v-if="collapsed"
        key="rail"
        class="collapsed-rail"
        type="button"
        title="展开目录"
        @click="$emit('toggle')"
      >
        <span>目录</span>
        <strong>{{ note?.headings.length || 0 }}</strong>
        <small>标题</small>
      </button>

      <div v-else key="content" class="inspector-content">
        <div class="metric-grid">
          <div class="metric"><small>笔记</small><strong>{{ stats.total }}</strong></div>
          <div class="metric"><small>字数</small><strong>{{ stats.words.toLocaleString() }}</strong></div>
          <div class="metric"><small>待复习</small><strong>{{ stats.dueReviews }}</strong></div>
        </div>

        <section class="side-section review-mini">
          <h3><CalendarCheck :size="16" /> 今日复习</h3>
          <p>{{ stats.dueReviews ? `还有 ${stats.dueReviews} 篇需要过一遍` : '今天的复习队列已清空' }}</p>
        </section>

        <section class="side-section">
          <h3><Clock3 :size="16" /> 最近更新</h3>
          <button
            v-for="item in recent"
            :key="item.id"
            class="recent-link"
            @click="$emit('select', item.id)"
          >
            <span>{{ item.title }}</span>
            <small>{{ formatDate(item.updatedAt) }}</small>
          </button>
        </section>

        <section class="side-section">
          <h3><Tag :size="16" /> 标签</h3>
          <div class="tag-cloud">
            <span v-for="tag in stats.tags" :key="tag.name">{{ tag.name }}<b>{{ tag.count }}</b></span>
          </div>
        </section>

        <section v-if="note?.headings.length" class="side-section toc">
          <h3><BookOpen :size="16" /> 目录</h3>
          <a
            v-for="(heading, index) in note.headings.slice(0, 16)"
            :key="`${heading.slug}-${index}`"
            :href="`#${heading.slug}`"
            :style="{ paddingLeft: `${(heading.level - 1) * 10}px` }"
          >
            {{ heading.text }}
          </a>
        </section>
      </div>
    </Transition>
  </aside>
</template>
