<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import {
  fetchImdbRatingsSyncStatus,
  fetchTmdbImportStatus,
  fetchTmdbTvSeasonImportStatus,
  fetchTvSeasonImdbRatingsSyncStatus,
  runTmdbImportBatch,
  runTmdbTvSeasonImportBatch,
  setTmdbImportEnabled,
  setTmdbImportInterval,
  setTmdbTvSeasonImportEnabled,
  setTmdbTvSeasonImportInterval,
  syncImdbRatings,
  syncMissingImdbRatings,
  syncMissingTvSeasonImdbRatings,
  syncTvSeasonImdbRatings,
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
const tvSeasonImdbSyncing = ref(false)
const tmdbId = ref<number | null>(null)
const tvSeasonTmdbId = ref<number | null>(null)
const importCount = ref(1)
const tvSeasonImportCount = ref(1)
const intervalSeconds = ref<number | null>(null)
const tvSeasonIntervalSeconds = ref<number | null>(null)
const imdbSyncResult = ref<ImdbRatingsSyncResult | null>(null)
const imdbSyncStatus = ref<ImdbRatingsSyncStatus | null>(null)
const tvSeasonImdbSyncResult = ref<ImdbRatingsSyncResult | null>(null)
const tvSeasonImdbSyncStatus = ref<ImdbRatingsSyncStatus | null>(null)
const message = ref('')
const errorMessage = ref('')
let imdbSyncPollTimer: number | undefined
let tvSeasonImdbSyncPollTimer: number | undefined

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
const tvSeasonImdbSyncRunning = computed(() => tvSeasonImdbSyncing.value || tvSeasonImdbSyncStatus.value?.syncing === true)

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

async function loadTvSeasonImdbSyncStatus() {
  try {
    const nextStatus = await fetchTvSeasonImdbRatingsSyncStatus()
    tvSeasonImdbSyncStatus.value = nextStatus
    if (nextStatus.lastResult) {
      tvSeasonImdbSyncResult.value = nextStatus.lastResult
    }
    if (nextStatus.syncing) {
      startTvSeasonImdbSyncPolling()
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
    loadImdbSyncStatus(),
    loadTvSeasonImdbSyncStatus()
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

async function runTvSeasonImdbSync(missingOnly = false) {
  tvSeasonImdbSyncing.value = true
  tvSeasonImdbSyncResult.value = null
  message.value = ''
  errorMessage.value = ''

  try {
    const nextStatus = missingOnly ? await syncMissingTvSeasonImdbRatings() : await syncTvSeasonImdbRatings()
    tvSeasonImdbSyncStatus.value = nextStatus
    if (nextStatus.lastResult) {
      tvSeasonImdbSyncResult.value = nextStatus.lastResult
    }
    message.value = nextStatus.syncing
      ? (missingOnly ? '剧集待补 IMDb 评分同步已开始，页面会自动刷新结果' : '剧集 IMDb 评分同步已开始，页面会自动刷新结果')
      : buildTvSeasonImdbSyncSuccessMessage(nextStatus.lastResult)
    if (nextStatus.syncing) {
      startTvSeasonImdbSyncPolling()
    }
  } catch (error) {
    console.error(error)
    errorMessage.value = '剧集 IMDb 评分同步失败，请稍后重试或查看后端日志'
  } finally {
    tvSeasonImdbSyncing.value = false
  }
}

function startImdbSyncPolling() {
  stopImdbSyncPolling()
  imdbSyncPollTimer = window.setTimeout(pollImdbSyncStatus, 3000)
}

function startTvSeasonImdbSyncPolling() {
  stopTvSeasonImdbSyncPolling()
  tvSeasonImdbSyncPollTimer = window.setTimeout(pollTvSeasonImdbSyncStatus, 3000)
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

async function pollTvSeasonImdbSyncStatus() {
  const nextStatus = await loadTvSeasonImdbSyncStatus()
  if (!nextStatus) {
    startTvSeasonImdbSyncPolling()
    return
  }

  if (nextStatus.syncing) {
    startTvSeasonImdbSyncPolling()
    return
  }

  stopTvSeasonImdbSyncPolling()
  if (nextStatus.lastErrorMessage) {
    errorMessage.value = `剧集 IMDb 评分同步失败：${nextStatus.lastErrorMessage}`
    return
  }

  message.value = buildTvSeasonImdbSyncSuccessMessage(nextStatus.lastResult)
}

function stopImdbSyncPolling() {
  if (imdbSyncPollTimer !== undefined) {
    window.clearTimeout(imdbSyncPollTimer)
    imdbSyncPollTimer = undefined
  }
}

function stopTvSeasonImdbSyncPolling() {
  if (tvSeasonImdbSyncPollTimer !== undefined) {
    window.clearTimeout(tvSeasonImdbSyncPollTimer)
    tvSeasonImdbSyncPollTimer = undefined
  }
}

function buildImdbSyncSuccessMessage(result?: ImdbRatingsSyncResult | null) {
  if (!result) {
    return 'IMDb 评分同步完成'
  }
  const targetText = result.missingOnly ? '本次待补 IMDb 评分的电影' : '本地有 IMDb ID 的电影'
  return `IMDb 评分同步完成：${targetText} ${result.moviesWithImdbId} 部，匹配 ${result.matchedMovies} 部，更新 ${result.updatedMovies} 部`
}

function buildTvSeasonImdbSyncSuccessMessage(result?: ImdbRatingsSyncResult | null) {
  if (!result) {
    return '剧集 IMDb 评分同步完成'
  }
  const targetText = result.missingOnly ? '本次待补 IMDb 评分的剧集季' : '本地有 IMDb 父级 ID 的剧集季'
  return `剧集 IMDb 评分同步完成：${targetText} ${result.moviesWithImdbId} 个，匹配 ${result.matchedMovies} 个，更新 ${result.updatedMovies} 个`
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
  stopTvSeasonImdbSyncPolling()
})
</script>

<template>
  <div class="admin-page">
    <div class="admin-shell">
      <header class="admin-header">
        <div>
          <h1>后台管理</h1>
          <p>管理 TMDB 导入任务和 IMDb 评分同步。</p>
        </div>
        <button class="refresh-btn" :disabled="loading" @click="refreshStatus">
          {{ loading ? '刷新中' : '刷新' }}
        </button>
      </header>

      <div v-if="message" class="message success">{{ message }}</div>
      <div v-if="errorMessage" class="message error">{{ errorMessage }}</div>

      <section class="section-block">
        <div class="section-heading">
          <h2>自动导入任务</h2>
          <p>控制电影和剧集的 TMDB 定时导入状态。</p>
        </div>

        <div class="task-grid">
          <article class="task-card">
            <div class="task-card-header">
              <div>
                <span class="task-kicker">TMDB</span>
                <h3>电影自动导入</h3>
              </div>
              <span class="status-badge" :class="{ paused: !status?.enabled }">{{ statusText }}</span>
            </div>

            <div class="primary-metric">
              <span>下一次自动导入 ID</span>
              <strong>{{ status?.nextTmdbId ?? '-' }}</strong>
            </div>

            <div class="info-grid">
              <div class="info-item">
                <span>导入间隔</span>
                <strong>{{ status?.intervalSeconds ?? '-' }} 秒</strong>
              </div>
              <div class="info-item">
                <span>最近更新时间</span>
                <strong>{{ formatDateTime(status?.updatedAt) }}</strong>
              </div>
            </div>

            <div class="task-actions">
              <button class="primary-btn" :class="{ danger: status?.enabled }" :disabled="!status || switching"
                @click="toggleImport">
                {{ status?.enabled ? '暂停定时任务' : '开启定时任务' }}
              </button>
              <label class="form-field interval-field">
                <span>间隔时长（秒）</span>
                <input v-model.number="intervalSeconds" type="number" min="1" max="86400" placeholder="例如 10" />
              </label>
              <button class="secondary-btn" :disabled="!status || savingInterval" @click="saveInterval">
                {{ savingInterval ? '保存中' : '保存间隔' }}
              </button>
            </div>
          </article>

          <article class="task-card">
            <div class="task-card-header">
              <div>
                <span class="task-kicker">TMDB</span>
                <h3>剧集自动导入</h3>
              </div>
              <span class="status-badge" :class="{ paused: !tvSeasonStatus?.enabled }">{{ tvSeasonStatusText }}</span>
            </div>

            <div class="primary-metric">
              <span>下一次自动导入剧集 ID</span>
              <strong>{{ tvSeasonStatus?.nextTmdbId ?? '-' }}</strong>
            </div>

            <div class="info-grid">
              <div class="info-item">
                <span>导入间隔</span>
                <strong>{{ tvSeasonStatus?.intervalSeconds ?? '-' }} 秒</strong>
              </div>
              <div class="info-item">
                <span>最近更新时间</span>
                <strong>{{ formatDateTime(tvSeasonStatus?.updatedAt) }}</strong>
              </div>
            </div>

            <div class="task-actions">
              <button class="primary-btn" :class="{ danger: tvSeasonStatus?.enabled }"
                :disabled="!tvSeasonStatus || tvSeasonSwitching" @click="toggleTvSeasonImport">
                {{ tvSeasonStatus?.enabled ? '暂停剧集定时任务' : '开启剧集定时任务' }}
              </button>
              <label class="form-field interval-field">
                <span>间隔时长（秒）</span>
                <input v-model.number="tvSeasonIntervalSeconds" type="number" min="1" max="86400" placeholder="例如 300" />
              </label>
              <button class="secondary-btn" :disabled="!tvSeasonStatus || savingTvSeasonInterval"
                @click="saveTvSeasonInterval">
                {{ savingTvSeasonInterval ? '保存中' : '保存间隔' }}
              </button>
            </div>
          </article>
        </div>
      </section>

      <section class="section-block">
        <div class="section-heading">
          <h2>手动导入</h2>
          <p>按指定起始 ID 批量导入电影或剧集。</p>
        </div>

        <div class="task-grid">
          <article class="task-card compact-card">
            <div class="task-card-header">
              <div>
                <span class="task-kicker">电影</span>
                <h3>手动批量导入 TMDB 电影</h3>
              </div>
            </div>
            <p>从输入的 TMDB ID 开始，按 ID 递增导入指定次数。导入次数默认 1。</p>
            <div class="run-form">
              <label class="form-field">
                <span>起始 TMDB ID</span>
                <input v-model.number="tmdbId" type="number" min="1" placeholder="输入 TMDB ID" />
              </label>
              <label class="form-field count-field">
                <span>导入次数</span>
                <input v-model.number="importCount" type="number" min="1" placeholder="1" />
              </label>
              <button class="primary-btn" :disabled="running" @click="runBatch">
                {{ running ? '导入中' : '立即导入' }}
              </button>
            </div>
          </article>

          <article class="task-card compact-card">
            <div class="task-card-header">
              <div>
                <span class="task-kicker">剧集</span>
                <h3>手动批量导入 TMDB 剧集</h3>
              </div>
            </div>
            <p>从输入的 TMDB 剧集 ID 开始，按 ID 递增导入指定次数；每个 ID 会导入全部正式季。</p>
            <div class="run-form">
              <label class="form-field">
                <span>起始 TMDB 剧集 ID</span>
                <input v-model.number="tvSeasonTmdbId" type="number" min="1" placeholder="输入 TMDB 剧集 ID" />
              </label>
              <label class="form-field count-field">
                <span>导入次数</span>
                <input v-model.number="tvSeasonImportCount" type="number" min="1" placeholder="1" />
              </label>
              <button class="primary-btn" :disabled="tvSeasonRunning" @click="runTvSeasonBatch">
                {{ tvSeasonRunning ? '导入中' : '立即导入' }}
              </button>
            </div>
          </article>
        </div>
      </section>

      <section class="section-block">
        <div class="section-heading">
          <h2>IMDb 评分同步</h2>
          <p>同步 IMDb 评分和评分人数，支持全量同步和只同步待补。</p>
        </div>

        <div class="task-grid">
          <article class="task-card sync-card">
            <div class="task-card-header">
              <div>
                <span class="task-kicker">电影</span>
                <h3>电影 IMDb 评分同步</h3>
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
            <p>下载 IMDb 官方评分数据集，并用电影表里的 IMDb ID 更新 IMDb 评分和评分人数。</p>

            <div class="info-grid imdb-status-row">
              <div class="info-item">
                <span>上次同步时间</span>
                <strong>{{ formatDateTime(imdbSyncStatus?.lastSyncedAt) }}</strong>
              </div>
              <div class="info-item">
                <span>待补 IMDb 评分</span>
                <strong>{{ formatNumber(imdbSyncStatus?.missingRatingCount) }}</strong>
              </div>
              <div v-if="imdbSyncStatus?.syncing" class="info-item syncing-item">
                <span>当前同步开始</span>
                <strong>{{ formatDateTime(imdbSyncStatus.currentStartedAt) }}</strong>
              </div>
            </div>

            <div v-if="imdbSyncResult" class="result-block">
              <div class="result-title">最近结果</div>
              <div class="result-grid">
                <div class="info-item">
                  <span>{{ imdbSyncResult.missingOnly ? '本次待补 IMDb 评分' : '本地 IMDb ID' }}</span>
                  <strong>{{ imdbSyncResult.moviesWithImdbId }}</strong>
                </div>
                <div class="info-item">
                  <span>数据集总行数</span>
                  <strong>{{ imdbSyncResult.totalRatingsRows }}</strong>
                </div>
                <div class="info-item">
                  <span>匹配电影</span>
                  <strong>{{ imdbSyncResult.matchedMovies }}</strong>
                </div>
                <div class="info-item">
                  <span>已更新</span>
                  <strong>{{ imdbSyncResult.updatedMovies }}</strong>
                </div>
                <div class="info-item">
                  <span>未匹配</span>
                  <strong>{{ imdbSyncResult.missingMovies }}</strong>
                </div>
                <div class="info-item">
                  <span>完成时间</span>
                  <strong>{{ formatDateTime(imdbSyncResult.finishedAt) }}</strong>
                </div>
              </div>
            </div>
          </article>

          <article class="task-card sync-card">
            <div class="task-card-header">
              <div>
                <span class="task-kicker">剧集</span>
                <h3>剧集 IMDb 评分同步</h3>
              </div>
              <div class="imdb-sync-actions">
                <button class="secondary-btn" :disabled="tvSeasonImdbSyncRunning" @click="runTvSeasonImdbSync(true)">
                  {{ tvSeasonImdbSyncRunning ? '同步中' : '只同步待补' }}
                </button>
                <button class="primary-btn" :disabled="tvSeasonImdbSyncRunning" @click="runTvSeasonImdbSync()">
                  {{ tvSeasonImdbSyncRunning ? '同步中' : '全量同步' }}
                </button>
              </div>
            </div>
            <p>下载 IMDb 官方单集和评分数据集，并按每一季的单集平均分和评分人数总和更新剧集 IMDb 评分。</p>

            <div class="info-grid imdb-status-row">
              <div class="info-item">
                <span>上次同步时间</span>
                <strong>{{ formatDateTime(tvSeasonImdbSyncStatus?.lastSyncedAt) }}</strong>
              </div>
              <div class="info-item">
                <span>待补 IMDb 评分</span>
                <strong>{{ formatNumber(tvSeasonImdbSyncStatus?.missingRatingCount) }}</strong>
              </div>
              <div v-if="tvSeasonImdbSyncStatus?.syncing" class="info-item syncing-item">
                <span>当前同步开始</span>
                <strong>{{ formatDateTime(tvSeasonImdbSyncStatus.currentStartedAt) }}</strong>
              </div>
            </div>

            <div v-if="tvSeasonImdbSyncResult" class="result-block">
              <div class="result-title">最近结果</div>
              <div class="result-grid">
                <div class="info-item">
                  <span>{{ tvSeasonImdbSyncResult.missingOnly ? '本次待补 IMDb 评分' : '本地 IMDb 父级 ID' }}</span>
                  <strong>{{ tvSeasonImdbSyncResult.moviesWithImdbId }}</strong>
                </div>
                <div class="info-item">
                  <span>评分数据集总行数</span>
                  <strong>{{ tvSeasonImdbSyncResult.totalRatingsRows }}</strong>
                </div>
                <div class="info-item">
                  <span>匹配剧集季</span>
                  <strong>{{ tvSeasonImdbSyncResult.matchedMovies }}</strong>
                </div>
                <div class="info-item">
                  <span>已更新</span>
                  <strong>{{ tvSeasonImdbSyncResult.updatedMovies }}</strong>
                </div>
                <div class="info-item">
                  <span>未匹配</span>
                  <strong>{{ tvSeasonImdbSyncResult.missingMovies }}</strong>
                </div>
                <div class="info-item">
                  <span>完成时间</span>
                  <strong>{{ formatDateTime(tvSeasonImdbSyncResult.finishedAt) }}</strong>
                </div>
              </div>
            </div>
          </article>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.admin-page {
  background: #f5f6f8;
  color: #111827;
  margin: -24px;
  min-height: calc(100vh - 72px);
  padding: 24px;
}

.admin-shell {
  display: flex;
  flex-direction: column;
  gap: 22px;
  margin: 0 auto;
  max-width: 1360px;
}

.admin-header,
.section-block,
.task-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.admin-header {
  align-items: center;
  display: flex;
  gap: 18px;
  justify-content: space-between;
  padding: 22px 24px;
}

.section-block {
  padding: 20px;
}

.section-heading {
  align-items: flex-end;
  display: flex;
  gap: 16px;
  justify-content: space-between;
  margin-bottom: 16px;
}

h1,
h2,
h3 {
  margin: 0;
}

h1 {
  font-size: 26px;
  font-weight: 700;
}

h2 {
  font-size: 18px;
  font-weight: 700;
}

h3 {
  font-size: 17px;
  font-weight: 700;
}

p {
  color: #6b7280;
  font-size: 14px;
  line-height: 1.6;
  margin: 8px 0 0;
}

.task-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.task-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
  padding: 18px;
}

.compact-card {
  gap: 14px;
}

.sync-card {
  gap: 15px;
}

.task-card-header {
  align-items: flex-start;
  display: flex;
  gap: 14px;
  justify-content: space-between;
}

.task-kicker {
  color: #1677ff;
  display: block;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
  margin-bottom: 6px;
}

.status-badge {
  background: #eaf8ef;
  border: 1px solid #bfe8cc;
  border-radius: 999px;
  color: #16803c;
  flex: 0 0 auto;
  font-size: 13px;
  font-weight: 700;
  line-height: 1;
  padding: 8px 11px;
}

.status-badge.paused {
  background: #fff7ed;
  border-color: #fed7aa;
  color: #b45309;
}

.primary-metric {
  background: #f8fbff;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  padding: 16px;
}

.primary-metric span,
.info-item span,
.form-field span {
  color: #6b7280;
  display: block;
  font-size: 13px;
}

.primary-metric strong {
  color: #1677ff;
  display: block;
  font-size: 30px;
  font-weight: 750;
  line-height: 1.1;
  margin-top: 8px;
  word-break: break-word;
}

.info-grid,
.result-grid {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.result-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.info-item {
  background: #fafafa;
  border: 1px solid #edf0f3;
  border-radius: 8px;
  min-width: 0;
  padding: 12px;
}

.info-item strong {
  color: #111827;
  display: block;
  font-size: 15px;
  line-height: 1.35;
  margin-top: 7px;
  word-break: break-word;
}

.syncing-item {
  background: #f8fbff;
  border-color: #dbeafe;
}

.task-actions,
.run-form {
  align-items: flex-end;
  border-top: 1px solid #eef0f3;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: auto;
  padding-top: 16px;
}

.run-form {
  margin-top: 2px;
}

.imdb-sync-actions {
  display: flex;
  flex: 0 0 auto;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.form-field input {
  background: #fff;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  color: #111827;
  height: 38px;
  outline: none;
  padding: 0 12px;
  width: 220px;
}

.form-field input:focus {
  border-color: #1677ff;
  box-shadow: 0 0 0 3px rgba(22, 119, 255, 0.12);
}

.count-field input,
.interval-field input {
  width: 140px;
}

button {
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 700;
  height: 38px;
  padding: 0 16px;
  white-space: nowrap;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.primary-btn,
.refresh-btn {
  background: #1677ff;
  color: #fff;
}

.secondary-btn {
  background: #eef5ff;
  border: 1px solid #cfe0ff;
  color: #1677ff;
}

.primary-btn.danger {
  background: #dc2626;
}

.result-block {
  border-top: 1px solid #eef0f3;
  padding-top: 14px;
}

.result-title {
  color: #374151;
  font-size: 14px;
  font-weight: 700;
  margin-bottom: 10px;
}

.message {
  border-radius: 8px;
  font-size: 14px;
  padding: 12px 14px;
}

.message.success {
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  color: #166534;
}

.message.error {
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #991b1b;
}

@media (max-width: 980px) {
  .admin-page {
    margin: -16px;
    padding: 16px;
  }

  .task-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .admin-header,
  .section-heading,
  .task-card-header,
  .task-actions,
  .run-form,
  .imdb-sync-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .admin-header,
  .section-block,
  .task-card {
    padding: 16px;
  }

  h1 {
    font-size: 22px;
  }

  .primary-metric strong {
    font-size: 26px;
  }

  .info-grid,
  .result-grid {
    grid-template-columns: 1fr;
  }

  .form-field,
  .form-field input,
  .count-field input,
  .interval-field input,
  .imdb-sync-actions,
  button {
    width: 100%;
  }
}
</style>
