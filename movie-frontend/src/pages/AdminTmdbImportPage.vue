<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  fetchTmdbImportStatus,
  runTmdbImportOnce,
  setTmdbImportEnabled,
  type TmdbImportStatus
} from '../api/tmdbImport'

const status = ref<TmdbImportStatus | null>(null)
const loading = ref(false)
const switching = ref(false)
const running = ref(false)
const tmdbId = ref<number | null>(null)
const message = ref('')
const errorMessage = ref('')

const statusText = computed(() => {
  if (!status.value) {
    return '读取中'
  }
  return status.value.enabled ? '运行中' : '已暂停'
})

async function loadStatus() {
  loading.value = true
  errorMessage.value = ''

  try {
    status.value = await fetchTmdbImportStatus()
    if (tmdbId.value === null) {
      tmdbId.value = status.value.nextTmdbId
    }
  } catch (error) {
    console.error(error)
    errorMessage.value = '读取导入状态失败'
  } finally {
    loading.value = false
  }
}

async function toggleImport() {
  if (!status.value) {
    return
  }

  switching.value = true
  message.value = ''
  errorMessage.value = ''

  try {
    status.value = await setTmdbImportEnabled(!status.value.enabled)
    message.value = status.value.enabled ? '定时任务已开启' : '定时任务已暂停'
  } catch (error) {
    console.error(error)
    errorMessage.value = '更新开关失败'
  } finally {
    switching.value = false
  }
}

async function runOnce() {
  if (!tmdbId.value || tmdbId.value < 1) {
    errorMessage.value = '请输入有效的 TMDB ID'
    return
  }

  running.value = true
  message.value = ''
  errorMessage.value = ''

  try {
    const movie = await runTmdbImportOnce(tmdbId.value)
    message.value = `已导入：${movie.title || movie.originalTitle || `TMDB ${tmdbId.value}`}`
    await loadStatus()
  } catch (error) {
    console.error(error)
    errorMessage.value = '手动导入失败，请确认这个 TMDB ID 是否存在'
  } finally {
    running.value = false
  }
}

onMounted(() => {
  loadStatus()
})
</script>

<template>
  <div class="admin-page">
    <section class="panel">
      <div class="panel-header">
        <div>
          <h1>TMDB 导入管理</h1>
          <p>查看当前进度，暂停或开启定时导入，也可以手动导入指定电影。</p>
        </div>
        <button class="refresh-btn" :disabled="loading" @click="loadStatus">
          {{ loading ? '刷新中' : '刷新' }}
        </button>
      </div>

      <div class="status-grid">
        <div class="status-item">
          <span>定时任务</span>
          <strong :class="{ paused: !status?.enabled }">{{ statusText }}</strong>
        </div>
        <div class="status-item">
          <span>下一次自动导入 ID</span>
          <strong>{{ status?.nextTmdbId ?? '-' }}</strong>
        </div>
        <div class="status-item">
          <span>最近更新时间</span>
          <strong>{{ status?.updatedAt ?? '-' }}</strong>
        </div>
      </div>

      <div class="actions">
        <button
          class="primary-btn"
          :class="{ danger: status?.enabled }"
          :disabled="!status || switching"
          @click="toggleImport"
        >
          {{ status?.enabled ? '暂停定时任务' : '开启定时任务' }}
        </button>
      </div>
    </section>

    <section class="panel">
      <h2>手动执行一次</h2>
      <div class="run-form">
        <input
          v-model.number="tmdbId"
          type="number"
          min="1"
          placeholder="输入 TMDB ID"
        />
        <button class="primary-btn" :disabled="running" @click="runOnce">
          {{ running ? '导入中' : '立即导入' }}
        </button>
      </div>
    </section>

    <div v-if="message" class="message success">{{ message }}</div>
    <div v-if="errorMessage" class="message error">{{ errorMessage }}</div>
  </div>
</template>

<style scoped>
.admin-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.panel {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 22px;
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

h1,
h2 {
  margin: 0;
  color: #111827;
}

h1 {
  font-size: 24px;
}

h2 {
  font-size: 18px;
}

p {
  margin: 8px 0 0;
  color: #6b7280;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 22px;
}

.status-item {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 14px;
}

.status-item span {
  display: block;
  color: #6b7280;
  font-size: 13px;
  margin-bottom: 8px;
}

.status-item strong {
  color: #16803c;
  font-size: 18px;
}

.status-item strong.paused {
  color: #b45309;
}

.actions,
.run-form {
  display: flex;
  gap: 12px;
  margin-top: 18px;
}

.run-form input {
  width: 220px;
  height: 38px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  padding: 0 12px;
  outline: none;
}

.run-form input:focus {
  border-color: #1677ff;
}

button {
  height: 38px;
  border: none;
  border-radius: 6px;
  padding: 0 16px;
  cursor: pointer;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.primary-btn,
.refresh-btn {
  background: #1677ff;
  color: #fff;
}

.primary-btn.danger {
  background: #dc2626;
}

.message {
  border-radius: 8px;
  padding: 12px 14px;
  font-size: 14px;
}

.message.success {
  border: 1px solid #bbf7d0;
  background: #f0fdf4;
  color: #166534;
}

.message.error {
  border: 1px solid #fecaca;
  background: #fef2f2;
  color: #991b1b;
}

@media (max-width: 760px) {
  .panel {
    padding: 16px;
  }

  h1 {
    font-size: 21px;
  }

  .panel-header,
  .actions,
  .run-form {
    flex-direction: column;
  }

  .status-grid {
    grid-template-columns: 1fr;
  }

  .run-form input,
  button {
    width: 100%;
  }
}
</style>
