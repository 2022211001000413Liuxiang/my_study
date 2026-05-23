<script setup lang="ts">
import { computed, onBeforeUnmount, shallowRef, watch } from 'vue';
import { CalendarCheck, CheckCircle2, RefreshCw } from '@lucide/vue';
import { renderMarkdown } from '../markdown';
import type { Note, ReviewDay, ReviewLevel, ReviewOverview, ReviewQueue } from '../types';

const props = defineProps<{
  queue: ReviewQueue;
  overview: ReviewOverview;
  note: Note | null;
  markdown: string;
  busy: boolean;
}>();

const emit = defineEmits<{
  feedback: [level: ReviewLevel];
}>();

const queueNote = computed(() => props.queue.notes[0] || null);
const displayNote = computed(() => {
  if (!queueNote.value) return null;
  return props.note?.id === queueNote.value.id ? props.note : queueNote.value;
});
const isCurrentLoaded = computed(() => Boolean(queueNote.value && props.note?.id === queueNote.value.id));
const html = shallowRef('');

let renderFrame = 0;

watch(
  () => [props.markdown, props.note?.id, queueNote.value?.id],
  () => {
    if (renderFrame) {
      window.cancelAnimationFrame(renderFrame);
    }
    html.value = '';
    if (!isCurrentLoaded.value) return;
    renderFrame = window.requestAnimationFrame(() => {
      html.value = renderMarkdown(props.markdown);
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

function submit(level: ReviewLevel) {
  if (!props.busy) {
    emit('feedback', level);
  }
}

function shortDate(value: string | null) {
  if (!value) return '暂无安排';
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit'
  }).format(new Date(value));
}

function dayLabel(value: string) {
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit'
  }).format(new Date(value));
}

function heatLevel(day: ReviewDay, days: ReviewDay[]) {
  const max = Math.max(1, ...days.map((item) => item.count));
  if (!day.count) return 0;
  return Math.min(4, Math.ceil((day.count / max) * 4));
}
</script>

<template>
  <section class="review-pane">
    <Transition name="reader-swap" mode="out-in">
      <div v-if="queue.notes.length && displayNote" :key="displayNote.id" class="review-session">
        <header class="review-header">
          <div>
            <p class="eyebrow">Review Session</p>
            <h2>{{ displayNote.title }}</h2>
          </div>
          <div class="review-progress" aria-label="复习进度">
            <CalendarCheck :size="18" />
            <span>剩余 {{ overview.dueToday }} 篇</span>
            <b>已复习 {{ overview.reviewedToday }}</b>
          </div>
        </header>

        <section class="review-overview">
          <div class="overview-card">
            <small>未复习</small>
            <strong>{{ overview.levels.unreviewed }}</strong>
          </div>
          <div class="overview-card again">
            <small>不熟</small>
            <strong>{{ overview.levels.again }}</strong>
          </div>
          <div class="overview-card normal">
            <small>一般</small>
            <strong>{{ overview.levels.normal }}</strong>
          </div>
          <div class="overview-card easy">
            <small>熟悉</small>
            <strong>{{ overview.levels.easy }}</strong>
          </div>
        </section>

        <div class="review-facts">
          <span>{{ displayNote.category }}</span>
          <span>{{ displayNote.wordCount.toLocaleString() }} 字</span>
          <span>已复习 {{ displayNote.reviewCount }} 次</span>
          <span>下次 {{ shortDate(displayNote.nextReviewAt) }}</span>
        </div>

        <div class="review-reader">
          <div v-if="isCurrentLoaded" class="markdown-body" v-html="html" />
          <div v-else class="soft-empty">正在打开复习笔记...</div>
        </div>

        <section class="review-calendar">
          <div>
            <h3>最近 14 天</h3>
            <div class="heat-strip">
              <span
                v-for="day in overview.completedDays"
                :key="`done-${day.date}`"
                :data-level="heatLevel(day, overview.completedDays)"
                :title="`${dayLabel(day.date)} 完成 ${day.count} 篇`"
              />
            </div>
          </div>
          <div>
            <h3>未来 30 天</h3>
            <div class="heat-strip upcoming">
              <span
                v-for="day in overview.upcomingDays"
                :key="`next-${day.date}`"
                :data-level="heatLevel(day, overview.upcomingDays)"
                :title="`${dayLabel(day.date)} 到期 ${day.count} 篇`"
              />
            </div>
          </div>
        </section>

        <footer class="review-actions">
          <button class="review-button again" type="button" :disabled="busy" @click="submit('again')">
            <RefreshCw :size="17" />
            <span>不熟</span>
            <small>明天再来</small>
          </button>
          <button class="review-button normal" type="button" :disabled="busy" @click="submit('normal')">
            <CalendarCheck :size="17" />
            <span>一般</span>
            <small>3 天后</small>
          </button>
          <button class="review-button easy" type="button" :disabled="busy" @click="submit('easy')">
            <CheckCircle2 :size="17" />
            <span>熟悉</span>
            <small>7 / 14 / 30 天</small>
          </button>
        </footer>
      </div>

      <div v-else key="done" class="review-done">
        <div class="done-mark"><CheckCircle2 :size="34" /></div>
        <p class="eyebrow">Review Complete</p>
        <h2>今日已清空</h2>
        <p>今天已完成 {{ overview.reviewedToday }} 篇复习。</p>
        <small>下一篇最早复习：{{ shortDate(queue.nextReviewAt) }}</small>
        <section class="review-calendar done-calendar">
          <div>
            <h3>最近 14 天</h3>
            <div class="heat-strip">
              <span
                v-for="day in overview.completedDays"
                :key="`done-empty-${day.date}`"
                :data-level="heatLevel(day, overview.completedDays)"
                :title="`${dayLabel(day.date)} 完成 ${day.count} 篇`"
              />
            </div>
          </div>
          <div>
            <h3>未来 30 天</h3>
            <div class="heat-strip upcoming">
              <span
                v-for="day in overview.upcomingDays"
                :key="`next-empty-${day.date}`"
                :data-level="heatLevel(day, overview.upcomingDays)"
                :title="`${dayLabel(day.date)} 到期 ${day.count} 篇`"
              />
            </div>
          </div>
        </section>
        <div v-if="overview.recentReviewed.length" class="recent-reviewed">
          <span v-for="item in overview.recentReviewed" :key="item.id">{{ item.title }}</span>
        </div>
      </div>
    </Transition>
  </section>
</template>
