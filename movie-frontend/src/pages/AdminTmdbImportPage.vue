<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import {
  fetchImdbRatingsSyncStatus,
  fetchTmdbImportStatus,
  fetchTmdbTvSeasonImportStatus,
  runTmdbImportBatch,
  runTmdbTvSeasonImportBatch,
  setTmdbImportEnabled,
  setTmdbImportInterval,
  setTmdbTvSeasonImportEnabled,
  setTmdbTvSeasonImportInterval,
  syncImdbRatings,
  syncMissingImdbRatings,
  type ImdbRatingsSyncStatus,
  type ImdbRatingsSyncResult,
  type TmdbImportStatus
} from '../api/tmdbImport'

const status = ref<TmdbImportStatus | null>(null)
const tvSeasonStatus = ref<TmdbImportStatus | null>(null)
const loading = ref(false)
const switching = ref(false)
const tvSeasonSwitching = ref(false)
const savingInterval = ref(false)
const savingTvSeasonInterval = ref(false)
const running = ref(false)
const tvSeasonRunning = ref(false)
const imdbSyncing = ref(false)
const tmdbId = ref<number | null>(null)
const tvSeasonTmdbId = ref<number | null>(null)
const importCount = ref(1)
const tvSeasonImportCount = ref(1)
const intervalSeconds = ref<number | null>(null)
const tvSeasonIntervalSeconds = ref<number | null>(null)
const imdbSyncResult = ref<ImdbRatingsSyncResult | null>(null)
const imdbSyncStatus = ref<ImdbRatingsSyncStatus | null>(null)
const message = ref('')
const errorMessage = ref('')
let imdbSyncPollTimer: number | undefined

const statusText = computed(() => {
  if (!status.value) {
    return '读取中'
  }
  return status.value.enabled ? '运行中' : '已暂停'
})

const tvSeasonStatusText = computed(() => {
  if (!tvSeasonStatus.value) {
    return '读取中'
  }
  return tvSeasonStatus.value.enabled ? '运行中' : '已暂停'
})

const imdbSyncRunning = computed(() => imdbSyncing.value || imdbSyncStatus.value?.syncing === true)

async function loadStatus() {
  loading.value = true
  errorMessage.value = ''

  try {
    status.value = await fetchTmdbImportStatus()
    if (tmdbId.value === null) {
      tmdbId.value = status.value.nextTmdbId
    }
    intervalSeconds.value = status.value.intervalSeconds
  } catch (error) {
    console.error(error)
    errorMessage.value = '读取导入状态失败'
  } finally {
    loading.value = false
  }
}

async function loadTvSeasonStatus() {
  loading.value = true
  errorMessage.value = ''

  try {
    tvSeasonStatus.value = await fetchTmdbTvSeasonImportStatus()
    if (tvSeasonTmdbId.value === null) {
      tvSeasonTmdbId.value = tvSeasonStatus.value.nextTmdbId
    }
    tvSeasonIntervalSeconds.value = tvSeasonStatus.value.intervalSeconds
  } catch (error) {
    console.error(error)
    errorMessage.value = '读取剧集导入状态失败'
  } finally {
    loading.value = false
  }
}

async function loadImdbSyncStatus() {
  try {
    const nextStatus = await fetchImdbRatingsSyncStatus()
    imdbSyncStatus.value = nextStatus
    if (nextStatus.lastResult) {
      imdbSyncResult.value = nextStatus.lastResult
    }
    if (nextStatus.syncing) {
      startImdbSyncPolling()
    }
    return nextStatus
  } catch (error) {
    console.error(error)
    return null
  }
}

async function refreshStatus() {
  await Promise.all([
    loadStatus(),
    loadTvSeasonStatus(),
    loadImdbSyncStatus()
  ])
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

async function toggleTvSeasonImport() {
  if (!tvSeasonStatus.value) {
    return
  }

  tvSeasonSwitching.value = true
  message.value = ''
  errorMessage.value = ''

  try {
    tvSeasonStatus.value = await setTmdbTvSeasonImportEnabled(!tvSeasonStatus.value.enabled)
    message.value = tvSeasonStatus.value.enabled ? '剧集定时任务已开启' : '剧集定时任务已暂停'
  } catch (error) {
    console.error(error)
    errorMessage.value = '更新剧集定时任务开关失败'
  } finally {
    tvSeasonSwitching.value = false
  }
}

async function saveInterval() {
  if (!Number.isInteger(intervalSeconds.value) || intervalSeconds.value === null || intervalSeconds.value < 1) {
    errorMessage.value = '请输入有效的间隔时长'
    return
  }
  if (intervalSeconds.value > 86400) {
    errorMessage.value = '间隔时长不能超过 86400 秒'
    return
  }

  savingInterval.value = true
  message.value = ''
  errorMessage.value = ''

  try {
    status.value = await setTmdbImportInterval(intervalSeconds.value)
    intervalSeconds.value = status.value.intervalSeconds
    message.value = `定时任务间隔已更新为 ${status.value.intervalSeconds} 秒`
  } catch (error) {
    console.error(error)
    errorMessage.value = '更新间隔时长失败'
  } finally {
    savingInterval.value = false
  }
}

async function saveTvSeasonInterval() {
  if (!Number.isInteger(tvSeasonIntervalSeconds.value) || tvSeasonIntervalSeconds.value === null || tvSeasonIntervalSeconds.value < 1) {
    errorMessage.value = '请输入有效的剧集导入间隔'
    return
  }
  if (tvSeasonIntervalSeconds.value > 86400) {
    errorMessage.value = '剧集导入间隔不能超过 86400 秒'
    return
  }

  savingTvSeasonInterval.value = true
  message.value = ''
  errorMessage.value = ''

  try {
    tvSeasonStatus.value = await setTmdbTvSeasonImportInterval(tvSeasonIntervalSeconds.value)
    tvSeasonIntervalSeconds.value = tvSeasonStatus.value.intervalSeconds
    message.value = `剧集定时任务间隔已更新为 ${tvSeasonStatus.value.intervalSeconds} 秒`
  } catch (error) {
    console.error(error)
    errorMessage.value = '更新剧集导入间隔失败'
  } finally {
    savingTvSeasonInterval.value = false
  }
}

async function runBatch() {
  if (!tmdbId.value || tmdbId.value < 1) {
    errorMessage.value = '请输入有效的 TMDB ID'
    return
  }
  if (!Number.isInteger(importCount.value) || importCount.value < 1) {
    errorMessage.value = '请输入有效的导入次数'
    return
  }

  running.value = true
  message.value = ''
  errorMessage.value = ''

  try {
    const result = await runTmdbImportBatch(tmdbId.value, importCount.value)
    const failedIds = result.items
      .filter((item) => !item.success)
      .map((item) => item.tmdbId)

    message.value = `已从 TMDB ${result.startTmdbId} 开始导入 ${result.requestedCount} 个，成功 ${result.successCount} 个，失败 ${result.failureCount} 个`
    if (failedIds.length > 0) {
      message.value += `。失败 ID：${failedIds.slice(0, 8).join('、')}`
      if (failedIds.length > 8) {
        message.value += '...'
      }
    }
    await loadStatus()
  } catch (error) {
    console.error(error)
    errorMessage.value = '手动导入失败，请确认 TMDB ID 和导入次数是否正确'
  } finally {
    running.value = false
  }
}

async function runTvSeasonBatch() {
  if (!tvSeasonTmdbId.value || tvSeasonTmdbId.value < 1) {
    errorMessage.value = '请输入有效的 TMDB 剧集 ID'
    return
  }
  if (!Number.isInteger(tvSeasonImportCount.value) || tvSeasonImportCount.value < 1) {
    errorMessage.value = '请输入有效的剧集导入次数'
    return
  }

  tvSeasonRunning.value = true
  message.value = ''
  errorMessage.value = ''

  try {
    const result = await runTmdbTvSeasonImportBatch(tvSeasonTmdbId.value, tvSeasonImportCount.value)
    const failedIds = result.items
      .filter((item) => !item.success)
      .map((item) => item.tmdbId)

    message.value = `已从 TMDB 剧集 ${result.startTmdbId} 开始导入 ${result.requestedCount} 个，成功 ${result.successCount} 个，失败 ${result.failureCount} 个`
    if (failedIds.length > 0) {
      message.value += `。失败 ID：${failedIds.slice(0, 8).join('、')}`
      if (failedIds.length > 8) {
        message.value += '...'
      }
    }
    await loadTvSeasonStatus()
  } catch (error) {
    console.error(error)
    errorMessage.value = '手动导入剧集失败，请确认 TMDB 剧集 ID 和导入次数是否正确'
  } finally {
    tvSeasonRunning.value = false
  }
}

async function runImdbSync(missingOnly = false) {
  imdbSyncing.value = true
  imdbSyncResult.value = null
  message.value = ''
  errorMessage.value = ''

  try {
    const nextStatus = missingOnly ? await syncMissingImdbRatings() : await syncImdbRatings()
    imdbSyncStatus.value = nextStatus
    if (nextStatus.lastResult) {
      imdbSyncResult.value = nextStatus.lastResult
    }
    message.value = nextStatus.syncing
      ? (missingOnly ? '待补 IMDb 评分同步已开始，页面会自动刷新结果' : 'IMDb 评分同步已开始，页面会自动刷新结果')
      : buildImdbSyncSuccessMessage(nextStatus.lastResult)
    if (nextStatus.syncing) {
      startImdbSyncPolling()
    }
  } catch (error) {
    console.error(error)
    errorMessage.value = 'IMDb 评分同步失败，请稍后重试或查看后端日志'
  } finally {
    imdbSyncing.value = false
  }
}

function startImdbSyncPolling() {
  stopImdbSyncPolling()
  imdbSyncPollTimer = window.setTimeout(pollImdbSyncStatus, 3000)
}

async function pollImdbSyncStatus() {
  const nextStatus = await loadImdbSyncStatus()
  if (!nextStatus) {
    startImdbSyncPolling()
    return
  }

  if (nextStatus.syncing) {
    startImdbSyncPolling()
    return
  }

  stopImdbSyncPolling()
  if (nextStatus.lastErrorMessage) {
    errorMessage.value = `IMDb 评分同步失败：${nextStatus.lastErrorMessage}`
    return
  }

  message.value = buildImdbSyncSuccessMessage(nextStatus.lastResult)
}

function stopImdbSyncPolling() {
  if (imdbSyncPollTimer !== undefined) {
    window.clearTimeout(imdbSyncPollTimer)
    imdbSyncPollTimer = undefined
  }
}

function buildImdbSyncSuccessMessage(result?: ImdbRatingsSyncResult | null) {
  if (!result) {
    return 'IMDb 评分同步完成'
  }
  const targetText = result.missingOnly ? '本次待补 IMDb 评分的电影' : '本地有 IMDb ID 的电影'
  return `IMDb 评分同步完成：${targetText} ${result.moviesWithImdbId} 部，匹配 ${result.matchedMovies} 部，更新 ${result.updatedMovies} 部`
}

function formatDateTime(value?: string | null) {
  if (!value) {
    return '暂无'
  }

  const date = parseServerDateTimeAsUtc(value)
  if (Number.isNaN(date.getTime())) {
    return value.replace('T', ' ').slice(0, 19)
  }

  return [
    date.getFullYear(),
    padDatePart(date.getMonth() + 1),
    padDatePart(date.getDate())
  ].join('-') + ' ' + [
    padDatePart(date.getHours()),
    padDatePart(date.getMinutes()),
    padDatePart(date.getSeconds())
  ].join(':')
}

function parseServerDateTimeAsUtc(value: string) {
  const [datePart, timePart = '00:00:00'] = value.split('T')
  const [year, month, day] = datePart.split('-').map(Number)
  const [hour = '0', minute = '0', secondText = '0'] = timePart.split(':')
  const second = Number(secondText.split('.')[0])

  return new Date(Date.UTC(year, month - 1, day, Number(hour), Number(minute), second))
}

function padDatePart(value: number) {
  return String(value).padStart(2, '0')
}

function formatNumber(value?: number | null) {
  if (value === null || value === undefined) {
    return '-'
  }
  return value.toLocaleString()
}

onMounted(() => {
  refreshStatus()
})

onBeforeUnmount(() => {
  stopImdbSyncPolling()
})
</script>

<template>
  <div class="admin-page">
    <section class="panel">
      <div class="panel-header">
        <div>
          <h1>后台管理</h1>
          <p>管理 TMDB 电影导入任务，也可以手动同步 IMDb 评分和评分人数。</p>
        </div>
        <button class="refresh-btn" :disabled="loading" @click="refreshStatus">
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
          <span>导入间隔</span>
          <strong>{{ status?.intervalSeconds ?? '-' }} 秒</strong>
        </div>
        <div class="status-item">
          <span>最近更新时间</span>
          <strong>{{ formatDateTime(status?.updatedAt) }}</strong>
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
        <label class="form-field interval-field">
          <span>间隔时长（秒）</span>
          <input
            v-model.number="intervalSeconds"
            type="number"
            min="1"
            max="86400"
            placeholder="例如 10"
          />
        </label>
        <button class="secondary-btn" :disabled="!status || savingInterval" @click="saveInterval">
          {{ savingInterval ? '保存中' : '保存间隔' }}
        </button>
      </div>
    </section>

    <section class="panel">
      <div class="panel-header">
        <div>
          <h2>TMDB 剧集定时导入</h2>
          <p>按 TMDB 剧集 ID 从小到大自动导入，每个剧集 ID 会导入该剧所有正式季。</p>
        </div>
      </div>

      <div class="status-grid">
        <div class="status-item">
          <span>定时任务</span>
          <strong :class="{ paused: !tvSeasonStatus?.enabled }">{{ tvSeasonStatusText }}</strong>
        </div>
        <div class="status-item">
          <span>下一次自动导入剧集 ID</span>
          <strong>{{ tvSeasonStatus?.nextTmdbId ?? '-' }}</strong>
        </div>
        <div class="status-item">
          <span>导入间隔</span>
          <strong>{{ tvSeasonStatus?.intervalSeconds ?? '-' }} 秒</strong>
        </div>
        <div class="status-item">
          <span>最近更新时间</span>
          <strong>{{ formatDateTime(tvSeasonStatus?.updatedAt) }}</strong>
        </div>
      </div>

      <div class="actions">
        <button
          class="primary-btn"
          :class="{ danger: tvSeasonStatus?.enabled }"
          :disabled="!tvSeasonStatus || tvSeasonSwitching"
          @click="toggleTvSeasonImport"
        >
          {{ tvSeasonStatus?.enabled ? '暂停剧集定时任务' : '开启剧集定时任务' }}
        </button>
        <label class="form-field interval-field">
          <span>间隔时长（秒）</span>
          <input
            v-model.number="tvSeasonIntervalSeconds"
            type="number"
            min="1"
            max="86400"
            placeholder="例如 300"
          />
        </label>
        <button class="secondary-btn" :disabled="!tvSeasonStatus || savingTvSeasonInterval" @click="saveTvSeasonInterval">
          {{ savingTvSeasonInterval ? '保存中' : '保存间隔' }}
        </button>
      </div>
    </section>

    <section class="panel">
      <h2>手动批量导入 TMDB 剧集</h2>
      <p>从输入的 TMDB 剧集 ID 开始，按 ID 递增导入指定次数；每个 ID 会导入全部正式季。</p>
      <div class="run-form">
        <label class="form-field">
          <span>起始 TMDB 剧集 ID</span>
          <input
            v-model.number="tvSeasonTmdbId"
            type="number"
            min="1"
            placeholder="输入 TMDB 剧集 ID"
          />
        </label>
        <label class="form-field count-field">
          <span>导入次数</span>
          <input
            v-model.number="tvSeasonImportCount"
            type="number"
            min="1"
            placeholder="1"
          />
        </label>
        <button class="primary-btn" :disabled="tvSeasonRunning" @click="runTvSeasonBatch">
          {{ tvSeasonRunning ? '导入中' : '立即导入' }}
        </button>
      </div>
    </section>

    <section class="panel">
      <h2>手动批量导入 TMDB 电影</h2>
      <p>从输入的 TMDB ID 开始，按 ID 递增导入指定次数。导入次数默认 1。</p>
      <div class="run-form">
        <label class="form-field">
          <span>起始 TMDB ID</span>
          <input
            v-model.number="tmdbId"
            type="number"
            min="1"
            placeholder="输入 TMDB ID"
          />
        </label>
        <label class="form-field count-field">
          <span>导入次数</span>
          <input
            v-model.number="importCount"
            type="number"
            min="1"
            placeholder="1"
          />
        </label>
        <button class="primary-btn" :disabled="running" @click="runBatch">
          {{ running ? '导入中' : '立即导入' }}
        </button>
      </div>
    </section>

    <section class="panel">
      <div class="panel-header">
        <div>
          <h2>手动同步 IMDb 评分</h2>
          <p>点击后会下载 IMDb 官方评分数据集，并用电影表里的 IMDb ID 更新 IMDb 评分和评分人数。</p>
        </div>
        <div class="imdb-sync-actions">
          <button class="secondary-btn" :disabled="imdbSyncRunning" @click="runImdbSync(true)">
            {{ imdbSyncRunning ? '同步中' : '只同步待补' }}
          </button>
          <button class="primary-btn" :disabled="imdbSyncRunning" @click="runImdbSync()">
            {{ imdbSyncRunning ? '同步中' : '全量同步' }}
          </button>
        </div>
      </div>

      <div class="imdb-status-row">
        <div class="status-item">
          <span>上次同步时间</span>
          <strong>{{ formatDateTime(imdbSyncStatus?.lastSyncedAt) }}</strong>
        </div>
        <div v-if="imdbSyncStatus?.syncing" class="status-item">
          <span>当前同步开始</span>
          <strong>{{ formatDateTime(imdbSyncStatus.currentStartedAt) }}</strong>
        </div>
        <div class="status-item">
          <span>待补 IMDb 评分</span>
          <strong>{{ formatNumber(imdbSyncStatus?.missingRatingCount) }}</strong>
        </div>
      </div>

      <div v-if="imdbSyncResult" class="result-grid">
        <div class="status-item">
          <span>{{ imdbSyncResult.missingOnly ? '本次待补 IMDb 评分' : '本地 IMDb ID' }}</span>
          <strong>{{ imdbSyncResult.moviesWithImdbId }}</strong>
        </div>
        <div class="status-item">
          <span>数据集总行数</span>
          <strong>{{ imdbSyncResult.totalRatingsRows }}</strong>
        </div>
        <div class="status-item">
          <span>匹配电影</span>
          <strong>{{ imdbSyncResult.matchedMovies }}</strong>
        </div>
        <div class="status-item">
          <span>已更新</span>
          <strong>{{ imdbSyncResult.updatedMovies }}</strong>
        </div>
        <div class="status-item">
          <span>未匹配</span>
          <strong>{{ imdbSyncResult.missingMovies }}</strong>
        </div>
        <div class="status-item">
          <span>完成时间</span>
          <strong>{{ formatDateTime(imdbSyncResult.finishedAt) }}</strong>
        </div>
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
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 22px;
}

.result-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 22px;
}

.imdb-status-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
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
  word-break: break-word;
}

.status-item strong.paused {
  color: #b45309;
}

.actions,
.run-form {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  margin-top: 18px;
}

.imdb-sync-actions {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.form-field span {
  color: #4b5563;
  font-size: 13px;
}

.actions input,
.run-form input {
  width: 220px;
  height: 38px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  padding: 0 12px;
  outline: none;
}

.actions input:focus,
.run-form input:focus {
  border-color: #1677ff;
}

.count-field input {
  width: 130px;
}

.interval-field input {
  width: 140px;
}

button {
  height: 38px;
  border: none;
  border-radius: 6px;
  padding: 0 16px;
  cursor: pointer;
  white-space: nowrap;
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

.secondary-btn {
  border: 1px solid #cfe0ff;
  background: #eef5ff;
  color: #1677ff;
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
  .run-form,
  .imdb-sync-actions {
    flex-direction: column;
  }

  .status-grid,
  .result-grid,
  .imdb-status-row {
    grid-template-columns: 1fr;
  }

  .form-field {
    width: 100%;
  }

  .run-form input,
  .actions input,
  .interval-field input,
  .count-field input,
  .imdb-sync-actions,
  button {
    width: 100%;
  }
}
</style>
