<script setup lang="ts">
import { Check, FilePlus2, Heart, MoveRight, Trash2 } from '@lucide/vue';
import type { Note, Status } from '../types';

defineProps<{
  note: Note | null;
  busy: boolean;
}>();

const editorText = defineModel<string>('editorText', { required: true });
const draftTitle = defineModel<string>('draftTitle', { required: true });
const draftCategory = defineModel<string>('draftCategory', { required: true });
const draftTags = defineModel<string>('draftTags', { required: true });
const draftStatus = defineModel<Status>('draftStatus', { required: true });
const draftFavorite = defineModel<boolean>('draftFavorite', { required: true });

defineEmits<{
  save: [];
  move: [];
  create: [];
  delete: [];
}>();
</script>

<template>
  <div class="admin">
    <div class="admin-actions">
      <button :disabled="busy" @click="$emit('create')"><FilePlus2 :size="17" /> 新建</button>
      <button :disabled="!note || busy" @click="$emit('save')"><Check :size="17" /> 保存正文</button>
      <button :disabled="!note || busy" @click="$emit('move')"><MoveRight :size="17" /> 更新位置</button>
      <button class="danger" :disabled="!note || busy" @click="$emit('delete')"><Trash2 :size="17" /> 删除</button>
    </div>

    <div class="form-grid">
      <label>标题<input v-model="draftTitle" /></label>
      <label>分类路径<input v-model="draftCategory" /></label>
      <label>标签<input v-model="draftTags" /></label>
      <label>
        状态
        <select v-model="draftStatus">
          <option value="learning">学习中</option>
          <option value="reviewing">复盘</option>
          <option value="done">完成</option>
        </select>
      </label>
    </div>

    <label class="favorite-toggle">
      <input v-model="draftFavorite" type="checkbox" />
      <Heart :size="17" /> 收藏这篇笔记
    </label>

    <textarea v-model="editorText" spellcheck="false" />
  </div>
</template>
