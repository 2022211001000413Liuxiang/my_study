<script setup lang="ts">
import { CalendarCheck, Folder, Search, Sparkles } from '@lucide/vue';
import type { Category, Stats } from '../types';

defineProps<{
  categories: Category[];
  stats: Stats;
}>();

const query = defineModel<string>('query', { required: true });
const selectedCategory = defineModel<string>('selectedCategory', { required: true });
</script>

<template>
  <aside class="sidebar">
    <div class="brand-block">
      <span class="brand-mark"><Sparkles :size="18" /></span>
      <div>
        <p class="eyebrow">Local Notes</p>
        <h2>个人学习记录</h2>
      </div>
    </div>

    <label class="search-box">
      <Search :size="18" />
      <input v-model="query" placeholder="搜索笔记、标签、正文" />
    </label>

    <nav class="category-list" aria-label="笔记分类">
      <button
        v-for="category in categories"
        :key="category.name"
        :class="{ active: selectedCategory === category.name }"
        @click="selectedCategory = category.name"
      >
        <Folder :size="16" />
        <span>{{ category.name === 'all' ? '全部笔记' : category.name }}</span>
        <b>{{ category.count }}</b>
      </button>
    </nav>

    <div class="sidebar-meter">
      <span>库容量</span>
      <strong>{{ stats.words.toLocaleString() }}</strong>
      <small>{{ stats.total }} 篇 / {{ stats.favorites }} 收藏</small>
      <em><CalendarCheck :size="14" /> 今日待复习 {{ stats.dueReviews }}</em>
    </div>
  </aside>
</template>
