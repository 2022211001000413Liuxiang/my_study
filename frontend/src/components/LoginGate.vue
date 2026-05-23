<script setup lang="ts">
import { Lock } from '@lucide/vue';

defineProps<{ busy: boolean }>();
const password = defineModel<string>('password', { required: true });

defineEmits<{
  login: [];
}>();
</script>

<template>
  <div class="login-panel">
    <Lock :size="34" />
    <h3>进入管理后台</h3>
    <p>管理操作会直接写入本地 md 目录。</p>
    <input
      v-model="password"
      type="password"
      placeholder="输入管理密码"
      @keydown.enter="$emit('login')"
    />
    <button :disabled="busy" @click="$emit('login')">{{ busy ? '验证中' : '登录' }}</button>
  </div>
</template>
