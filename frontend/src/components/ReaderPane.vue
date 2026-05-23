<script setup lang="ts">
import { onBeforeUnmount, shallowRef, watch } from 'vue';
import type { Note } from '../types';
import { renderMarkdown } from '../markdown';
import { formatDate, statusLabel } from '../utils';

const props = defineProps<{
  note: Note | null;
  markdown: string;
}>();

const html = shallowRef('');
let renderFrame = 0;

watch(
  () => props.markdown,
  (markdown) => {
    if (renderFrame) {
      window.cancelAnimationFrame(renderFrame);
    }
    html.value = '';
    renderFrame = window.requestAnimationFrame(() => {
      html.value = renderMarkdown(markdown);
      renderFrame = 0;
    });
  },
  { immediate: true }
);

onBeforeUnmount(() => {
  if (renderFrame) {
    window.cancelAnimationFrame(renderFrame);
  }
});
</script>

<template>
  <Transition name="reader-swap" mode="out-in">
    <div v-if="note" :key="note.id" class="reader">
      <div class="reader-meta">
        <span>{{ note.path }}</span>
        <span>{{ statusLabel[note.status] }}</span>
        <span>{{ formatDate(note.updatedAt) }}</span>
      </div>
      <div class="markdown-body" v-html="html" />
    </div>
    <div v-else key="empty" class="empty-state">还没有选中笔记</div>
  </Transition>
</template>
