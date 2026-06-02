<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getTvSeasonById, type TvSeasonCollectionItem, type TvSeasonDetail } from '../api/tvSeason'
import { getUserTvSeason, saveUserTvSeason, type UserTvSeasonResponse } from '../api/userTvSeason'
import type { PersonItem } from '../api/movie'
import { getToken, isLoggedIn } from '../utils/auth'

type UserStatus = 'WANT_TO_WATCH' | 'WATCHED' | 'NOT_INTERESTED'

const route = useRoute()
const season = ref<TvSeasonDetail | null>(null)
const userSeason = ref<UserTvSeasonResponse | null>(null)
const isUserLoggedIn = ref(isLoggedIn())
const loading = ref(false)
const errorMessage = ref('')
const saving = ref(false)
const visibleCastCount = ref(12)
const collectionExpanded = ref(false)
const collectionInitialCount = 4
const showWatchedPanel = ref(false)
const watchedRating = ref<number | null>(null)
const watchedYear = ref(new Date().getFullYear())
const watchedMonth = ref(new Date().getMonth() + 1)
const watchedDay = ref(new Date().getDate())
const hoveredRating = ref<number | null>(null)

const displayCast = computed(() => season.value?.cast.slice(0, visibleCastCount.value) ?? [])
const hasMoreCast = computed(() => (season.value?.cast.length ?? 0) > visibleCastCount.value)
const collectionSeasons = computed(() => season.value?.collectionSeasons ?? [])
const displayCollectionSeasons = computed(() => collectionExpanded.value
  ? collectionSeasons.value
  : collectionSeasons.value.slice(0, collectionInitialCount))
const hasMoreCollectionSeasons = computed(() => collectionSeasons.value.length > collectionInitialCount)
const releaseYear = computed(() => season.value?.year || season.value?.airDate?.slice(0, 4) || '年份未知')
const displayTitle = computed(() => season.value?.seasonCount === 1 ? season.value.showName : season.value?.title)
const watchedDateDisplay = computed(() => buildWatchedDateValue().replaceAll('-', '/'))
const heroStyle = computed(() => {
  if (!season.value?.posterPath) return {}
  return { '--detail-backdrop': `url("${getImageUrl(season.value.posterPath, 'w500')}")` }
})

async function loadPage() {
  isUserLoggedIn.value = isLoggedIn()
  loading.value = true
  errorMessage.value = ''
  userSeason.value = null
  visibleCastCount.value = 12
  collectionExpanded.value = false
  try {
    const id = Number(route.params.id)
    season.value = await getTvSeasonById(id)
    document.title = `${displayTitle.value} - 剧集`

    const token = getToken()
    if (token) {
      userSeason.value = await getUserTvSeason(id, token)
    }
  } catch (error) {
    console.error(error)
    errorMessage.value = '加载剧集详情失败'
  } finally {
    loading.value = false
  }
}

function getImageUrl(path: string | null, size: string) {
  return path ? `https://image.tmdb.org/t/p/${size}${path}` : ''
}

function formatScore(value: number | null | undefined) {
  return value === null || value === undefined ? '暂无' : Number(value).toFixed(2)
}

function formatCount(value: number | null | undefined) {
  return value === null || value === undefined ? '暂无' : Number(value).toLocaleString('zh-CN')
}

function formatDate(value: string | null | undefined) {
  return value ? value.slice(0, 10).replaceAll('-', '/') : '未知'
}

function formatIsoDate(value: string | null | undefined) {
  return value ? value.slice(0, 10) : '未知'
}

function formatList(values: string[] | null | undefined) {
  return values && values.length ? values.join(' / ') : '暂无'
}

function getDisplayVoteAverage(item: TvSeasonDetail) {
  return item.imdbVoteAverage ?? item.voteAverage
}

function getDisplayVoteCount(item: TvSeasonDetail) {
  return item.imdbVoteCount ?? item.voteCount
}

function getCollectionPosterUrl(item: TvSeasonCollectionItem) {
  return getImageUrl(item.posterPath, 'w185')
}

function getCollectionDisplayVoteAverage(item: TvSeasonCollectionItem) {
  return item.imdbVoteAverage ?? item.voteAverage
}

function formatYear(value: string | null | undefined) {
  return value ? value.slice(0, 4) : '未知'
}

function getPersonRole(person: PersonItem) {
  return person.character ? `饰 ${person.character}` : person.job || person.originalName || ''
}

function getStatusText(status: UserStatus) {
  if (status === 'WANT_TO_WATCH') return '想看'
  if (status === 'WATCHED') return '已看过'
  return '不想看'
}

function getStatusClass(status: UserStatus) {
  if (status === 'WANT_TO_WATCH') return 'want'
  if (status === 'WATCHED') return 'watched'
  return 'not-interested'
}

function getRatingValue(value: number | null | undefined) {
  if (value === null || value === undefined) return 0
  return Math.max(0, Math.min(5, Math.round(value)))
}

function getRecordDate() {
  if (!userSeason.value) return ''
  if (userSeason.value.status === 'WATCHED') {
    return userSeason.value.watchedDate ? formatDate(userSeason.value.watchedDate) : ''
  }
  return userSeason.value.updatedAt ? formatDate(userSeason.value.updatedAt) : ''
}

function getAuthenticatedToken() {
  const token = getToken()
  if (!token) {
    window.location.href = `/login?redirect=${encodeURIComponent(`/tv-seasons/${season.value?.id ?? ''}`)}`
    return null
  }
  return token
}

function getTodayString() {
  const date = new Date()
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function getDaysInMonth(year: number, month: number) {
  return new Date(year, month, 0).getDate()
}

function setWatchedDateParts(dateStr: string | null) {
  const value = dateStr || getTodayString()
  const [year, month, day] = value.slice(0, 10).split('-').map(Number)
  if (!year || !month || !day || month < 1 || month > 12 || day < 1 || day > getDaysInMonth(year, month)) {
    setWatchedDateParts(getTodayString())
    return
  }
  watchedYear.value = year
  watchedMonth.value = month
  watchedDay.value = day
}

function buildWatchedDateValue() {
  return [
    String(watchedYear.value).padStart(4, '0'),
    String(watchedMonth.value).padStart(2, '0'),
    String(watchedDay.value).padStart(2, '0')
  ].join('-')
}

function openNativeDatePicker(event: MouseEvent) {
  const picker = (event.currentTarget as HTMLElement).querySelector<HTMLInputElement>('input[type="date"]')
  if (!picker) return
  if (typeof picker.showPicker === 'function') picker.showPicker()
  else picker.click()
}

function handleNativeDateChange(event: Event) {
  const value = (event.target as HTMLInputElement).value
  if (value) setWatchedDateParts(value)
}

async function markSeason(status: Exclude<UserStatus, 'WATCHED'>) {
  if (!season.value) return
  const token = getAuthenticatedToken()
  if (!token) return

  saving.value = true
  try {
    await saveUserTvSeason(season.value.id, { status }, token)
    userSeason.value = await getUserTvSeason(season.value.id, token)
  } finally {
    saving.value = false
  }
}

function handleWatched() {
  if (!getAuthenticatedToken()) return
  showWatchedPanel.value = true
  if (userSeason.value?.status === 'WATCHED') {
    watchedRating.value = userSeason.value.rating
    setWatchedDateParts(userSeason.value.watchedDate)
  } else {
    watchedRating.value = null
    setWatchedDateParts(getTodayString())
  }
}

async function confirmWatched() {
  if (!season.value) return
  const token = getAuthenticatedToken()
  if (!token) return
  saving.value = true
  try {
    await saveUserTvSeason(season.value.id, {
      status: 'WATCHED',
      rating: watchedRating.value,
      watchedDate: buildWatchedDateValue()
    }, token)
    userSeason.value = await getUserTvSeason(season.value.id, token)
    showWatchedPanel.value = false
  } finally {
    saving.value = false
  }
}

function expandCast() {
  visibleCastCount.value += 12
}

function toggleCollectionExpanded() {
  collectionExpanded.value = !collectionExpanded.value
}

watch([watchedYear, watchedMonth], () => {
  const maxDay = getDaysInMonth(watchedYear.value, watchedMonth.value)
  if (watchedDay.value > maxDay) watchedDay.value = maxDay
})

watch(() => route.params.id, () => {
  void loadPage()
}, { immediate: true })
</script>

<template>
  <div class="tv-detail-page">
    <div v-if="loading" class="status">正在加载...</div>
    <div v-else-if="errorMessage" class="status error">{{ errorMessage }}</div>

    <template v-else-if="season">
      <section class="detail-hero" :style="heroStyle">
        <div class="hero-inner detail-hero-inner">
          <div class="poster-box poster-section">
            <img v-if="season.posterPath" :src="getImageUrl(season.posterPath, 'w500')" :alt="displayTitle" />
            <div v-else class="poster-placeholder">{{ displayTitle?.slice(0, 1) }}</div>
          </div>

          <div class="hero-main hero-summary">
            <div class="title-row">
              <h1>{{ displayTitle }}</h1>
              <span>{{ releaseYear }}</span>
            </div>
            <p class="original-title">{{ season.originalTitle || season.originalShowName || '原名未知' }}</p>
            <div class="meta-tags">
              <span v-for="genre in season.genres.slice(0, 3)" :key="genre">{{ genre }}</span>
              <span>{{ formatList(season.countries) }}</span>
              <span>{{ season.episodeCount ?? '未知' }} 集</span>
              <a v-if="season.imdbParentId" :href="`https://www.imdb.com/title/${season.imdbParentId}/`" target="_blank" rel="noreferrer">
                IMDb {{ season.imdbParentId }}
              </a>
            </div>
          </div>

          <div class="score-grid">
            <div class="score-card">
              <span class="score-label">评分</span>
              <strong>{{ formatScore(getDisplayVoteAverage(season)) }}</strong>
            </div>
            <div class="score-card">
              <span class="score-label">评分人数</span>
              <strong>{{ formatCount(getDisplayVoteCount(season)) }}</strong>
            </div>
            <div class="score-card">
              <span class="score-label">首播</span>
              <strong class="release-score">{{ formatIsoDate(season.airDate) }}</strong>
            </div>
          </div>

          <div v-if="isUserLoggedIn" class="action-panel">
            <div class="actions hero-actions">
              <button class="action-btn" :class="{ active: userSeason?.status === 'WANT_TO_WATCH' }" :disabled="saving" @click="markSeason('WANT_TO_WATCH')">想看</button>
              <button class="action-btn watched-action" :class="{ active: userSeason?.status === 'WATCHED' }" :disabled="saving" @click="handleWatched">看过</button>
              <button class="action-btn" :class="{ active: userSeason?.status === 'NOT_INTERESTED' }" :disabled="saving" @click="markSeason('NOT_INTERESTED')">不想看</button>
            </div>
            <div v-if="userSeason" class="record-pill hero-record" :class="getStatusClass(userSeason.status)">
              <span>{{ getStatusText(userSeason.status) }}</span>
              <span v-if="getRecordDate()">{{ getRecordDate() }}</span>
              <span v-if="userSeason.status === 'WATCHED'" class="record-stars">
                <span v-for="star in 5" :key="star" :class="{ filled: star <= getRatingValue(userSeason.rating) }">★</span>
              </span>
            </div>
          </div>
        </div>
      </section>

      <main class="detail-layout detail-content">
        <div class="main-column detail-main">
          <section class="detail-section">
            <h2>剧情简介</h2>
            <p class="overview overview-text">{{ season.overview || '暂无简介' }}</p>
          </section>

          <section class="detail-section">
            <h2>基础信息</h2>
            <div class="facts-grid">
              <div class="fact-item"><span>剧名</span><strong>{{ season.showName }}</strong></div>
              <div class="fact-item"><span>季数</span><strong>第 {{ season.seasonNumber }} 季</strong></div>
              <div class="fact-item"><span>原名</span><strong>{{ season.originalTitle || '未知' }}</strong></div>
              <div class="fact-item"><span>类型</span><strong>{{ formatList(season.genres) }}</strong></div>
              <div class="fact-item"><span>国家/地区</span><strong>{{ formatList(season.countries) }}</strong></div>
              <div class="fact-item"><span>集数</span><strong>{{ season.episodeCount ?? '未知' }} 集</strong></div>
            </div>
          </section>

          <section v-if="collectionSeasons.length > 1" class="detail-section collection-section">
            <div class="collection-header">
              <div class="collection-title-row">
                <h2>同系列作品</h2>
                <span class="collection-pill">{{ season.showName }}</span>
              </div>
            </div>
            <div class="collection-strip">
              <div class="collection-track">
                <RouterLink
                  v-for="item in displayCollectionSeasons"
                  :key="item.id"
                  class="collection-card"
                  :to="`/tv-seasons/${item.id}`"
                >
                  <div class="collection-thumb">
                    <img v-if="getCollectionPosterUrl(item)" :src="getCollectionPosterUrl(item)" :alt="item.seasonName" />
                    <div v-else class="collection-thumb-placeholder">{{ item.seasonName.slice(0, 1) }}</div>
                    <span class="collection-order">{{ item.seasonNumber }}</span>
                  </div>
                  <div class="collection-card-info">
                    <div class="collection-card-title">{{ item.seasonName }}</div>
                    <div class="collection-card-date">{{ formatDate(item.airDate) }}</div>
                  </div>
                  <div class="collection-card-side">
                    <div class="collection-card-year">{{ formatYear(item.airDate) }}</div>
                    <div class="collection-card-rating">{{ formatScore(getCollectionDisplayVoteAverage(item)) }}</div>
                  </div>
                </RouterLink>
              </div>
            </div>
            <div v-if="hasMoreCollectionSeasons" class="expand-wrap collection-expand-wrap">
              <button type="button" class="expand-btn" @click="toggleCollectionExpanded">
                {{ collectionExpanded ? '收起' : `展开全部 ${collectionSeasons.length} 季` }}
              </button>
            </div>
          </section>

          <div class="mobile-credit-sections">
            <section v-if="season.directors.length" class="detail-section">
              <h2>导演</h2>
              <RouterLink v-for="person in season.directors" :key="`mobile-director-${person.personId}`" class="compact-person" :to="`/persons/${person.personId}`">
                <img v-if="person.profilePath" :src="getImageUrl(person.profilePath, 'w185')" :alt="person.name" />
                <div v-else class="compact-placeholder">{{ person.name.slice(0, 1) }}</div>
                <strong>{{ person.name }}</strong>
              </RouterLink>
            </section>

            <section v-if="season.writers.length" class="detail-section">
              <h2>编剧</h2>
              <RouterLink v-for="person in season.writers" :key="`mobile-writer-${person.personId}`" class="compact-person" :to="`/persons/${person.personId}`">
                <img v-if="person.profilePath" :src="getImageUrl(person.profilePath, 'w185')" :alt="person.name" />
                <div v-else class="compact-placeholder">{{ person.name.slice(0, 1) }}</div>
                <strong>{{ person.name }}</strong>
              </RouterLink>
            </section>

            <section v-if="season.producers.length" class="detail-section">
              <h2>制片人</h2>
              <RouterLink v-for="person in season.producers" :key="`mobile-producer-${person.personId}`" class="compact-person" :to="`/persons/${person.personId}`">
                <img v-if="person.profilePath" :src="getImageUrl(person.profilePath, 'w185')" :alt="person.name" />
                <div v-else class="compact-placeholder">{{ person.name.slice(0, 1) }}</div>
                <strong>{{ person.name }}</strong>
              </RouterLink>
            </section>
          </div>

          <section class="detail-section">
            <h2>主要演员</h2>
            <div v-if="displayCast.length" class="people-grid">
              <RouterLink v-for="person in displayCast" :key="person.personId" class="person-card" :to="`/persons/${person.personId}`">
                <img v-if="person.profilePath" class="person-avatar" :src="getImageUrl(person.profilePath, 'w185')" :alt="person.name" />
                <div v-else class="avatar-placeholder person-avatar placeholder">{{ person.name.slice(0, 1) }}</div>
                <div class="person-info">
                  <strong class="person-name">{{ person.name }}</strong>
                  <span class="person-role">{{ getPersonRole(person) }}</span>
                </div>
              </RouterLink>
            </div>
            <p v-else class="empty">暂无演员信息</p>
            <button v-if="hasMoreCast" class="expand-btn" @click="expandCast">展开更多</button>
          </section>
        </div>

        <aside class="side-column detail-sidebar">
          <section v-if="userSeason" class="side-panel my-record-panel">
            <h2>我的记录</h2>
            <div class="record-card" :class="getStatusClass(userSeason.status)">
              <span class="record-status">{{ getStatusText(userSeason.status) }}</span>
              <div v-if="getRecordDate()" class="record-date">标记日期：{{ getRecordDate() }}</div>
              <div v-if="userSeason.status === 'WATCHED'" class="side-stars">
                <span v-for="star in 5" :key="star" :class="{ filled: star <= getRatingValue(userSeason.rating) }">★</span>
              </div>
            </div>
          </section>
          <section v-if="season.directors.length" class="side-panel">
            <h2>导演</h2>
            <RouterLink v-for="person in season.directors" :key="person.personId" class="compact-person" :to="`/persons/${person.personId}`">
              <img v-if="person.profilePath" :src="getImageUrl(person.profilePath, 'w185')" :alt="person.name" />
              <div v-else class="compact-placeholder">{{ person.name.slice(0, 1) }}</div>
              <strong>{{ person.name }}</strong>
            </RouterLink>
          </section>

          <section v-if="season.writers.length" class="side-panel">
            <h2>编剧</h2>
            <RouterLink v-for="person in season.writers" :key="person.personId" class="compact-person" :to="`/persons/${person.personId}`">
              <img v-if="person.profilePath" :src="getImageUrl(person.profilePath, 'w185')" :alt="person.name" />
              <div v-else class="compact-placeholder">{{ person.name.slice(0, 1) }}</div>
              <strong>{{ person.name }}</strong>
            </RouterLink>
          </section>

          <section v-if="season.producers.length" class="side-panel">
            <h2>制片人</h2>
            <RouterLink v-for="person in season.producers" :key="person.personId" class="compact-person" :to="`/persons/${person.personId}`">
              <img v-if="person.profilePath" :src="getImageUrl(person.profilePath, 'w185')" :alt="person.name" />
              <div v-else class="compact-placeholder">{{ person.name.slice(0, 1) }}</div>
              <strong>{{ person.name }}</strong>
            </RouterLink>
          </section>
        </aside>
      </main>
    </template>
  </div>

  <div v-if="showWatchedPanel" class="watched-modal-overlay" @click="showWatchedPanel = false">
    <div class="watched-modal" @click.stop>
      <button type="button" class="watched-modal-close" @click="showWatchedPanel = false">×</button>
      <div class="watched-modal-header">
        <h3>标记为看过</h3>
        <p>可以为这一季添加评分和观看日期</p>
      </div>
      <div class="watched-form-section">
        <label class="watched-label">评分</label>
        <div class="watched-stars-row">
          <div class="watched-stars" @mouseleave="hoveredRating = null">
            <button
              v-for="star in 5"
              :key="star"
              type="button"
              class="watched-star-btn"
              :class="{ active: star <= (hoveredRating ?? watchedRating ?? 0) }"
              @mouseenter="hoveredRating = star"
              @click="watchedRating = star"
            >
              ★
            </button>
          </div>
          <span class="watched-rating-text">
            {{ hoveredRating != null ? `${hoveredRating} 星` : watchedRating != null ? `${watchedRating} 星` : '未评分' }}
          </span>
        </div>
        <button v-if="watchedRating != null" type="button" class="clear-rating-btn" @click="watchedRating = null">
          清除评分
        </button>
      </div>
      <div class="watched-form-section">
        <label class="watched-label">观看日期</label>
        <div class="watched-date-input-shell" @click.stop="openNativeDatePicker">
          <button type="button" class="watched-date-display-button">{{ watchedDateDisplay }}</button>
          <button type="button" class="watched-date-picker-button" aria-label="选择观看日期"></button>
          <input class="native-date-input" type="date" :value="buildWatchedDateValue()" @change="handleNativeDateChange" />
        </div>
      </div>
      <div class="watched-modal-actions">
        <button type="button" class="watched-cancel-btn" @click="showWatchedPanel = false">取消</button>
        <button type="button" class="watched-confirm-btn" :disabled="saving" @click="confirmWatched">保存</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.tv-detail-page {
  display: grid;
  gap: 0;
}

.status {
  padding: 24px 0;
  color: #6b7280;
}

.status.error {
  color: #d93025;
}

.detail-hero {
  position: relative;
  overflow: hidden;
  border-radius: 8px;
  background: #101923;
  color: #fff;
}

.detail-hero::before {
  content: '';
  position: absolute;
  inset: -28px;
  background-image:
    linear-gradient(90deg, rgba(13, 18, 27, 0.96), rgba(13, 18, 27, 0.72) 42%, rgba(13, 18, 27, 0.9)),
    var(--detail-backdrop, linear-gradient(#101923, #101923));
  background-position: center;
  background-size: cover;
  filter: blur(16px) saturate(1.08);
  transform: scale(1.05);
}

.hero-inner {
  position: relative;
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  grid-template-rows: auto auto auto;
  gap: 0 34px;
  min-height: 460px;
  padding: 44px 44px 40px;
  align-content: end;
}

.poster-box {
  grid-row: 1 / 4;
  width: 260px;
  align-self: end;
}

.poster-box img,
.poster-placeholder {
  width: 100%;
  aspect-ratio: 2 / 3;
  border-radius: 8px;
  object-fit: cover;
  box-shadow: 0 22px 48px rgba(0, 0, 0, 0.34);
}

.hero-main,
.score-grid,
.action-panel {
  grid-column: 2;
}

.hero-main {
  min-width: 0;
}

.poster-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #d1d5db;
  color: #374151;
  font-size: 42px;
  font-weight: 900;
}

.title-row {
  display: flex;
  align-items: baseline;
  gap: 14px;
  flex-wrap: wrap;
}

.title-row h1 {
  margin: 0;
  font-size: 42px;
  line-height: 1.15;
}

.title-row span {
  color: rgba(255, 255, 255, 0.68);
  font-size: 28px;
  font-weight: 700;
}

.original-title {
  margin: 8px 0 0;
  color: rgba(255, 255, 255, 0.72);
  font-size: 17px;
}

.meta-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 22px;
}

.meta-tags span,
.meta-tags a {
  min-height: 34px;
  padding: 0 13px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.12);
  color: rgba(255, 255, 255, 0.88);
  text-decoration: none;
  border: 1px solid rgba(255, 255, 255, 0.12);
  font-size: 14px;
  font-weight: 650;
}

.score-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  max-width: 740px;
  margin-top: 22px;
}

.score-grid div {
  min-height: 78px;
  padding: 13px 16px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.1);
}

.score-grid span {
  display: block;
  color: rgba(255, 255, 255, 0.64);
  font-size: 13px;
}

.score-grid strong {
  display: block;
  margin-top: 8px;
  font-size: 24px;
  font-weight: 800;
  line-height: 1.1;
}

.score-card .release-score {
  font-size: 21px;
  white-space: nowrap;
}

.action-panel {
  margin-top: 24px;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.actions button {
  height: 42px;
  padding: 0 20px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
  cursor: pointer;
  font-size: 15px;
  font-weight: 750;
  transition: transform 0.18s ease, background 0.18s ease, border-color 0.18s ease;
}

.actions button:hover {
  transform: translateY(-1px);
  background: rgba(255, 255, 255, 0.18);
}

.actions button.active {
  border-color: #80dc78;
  background: #86dc7f;
  color: #143819;
}

.record-pill {
  width: fit-content;
  max-width: 100%;
  min-height: 42px;
  padding: 0 14px;
  margin-top: 12px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.12);
  color: rgba(255, 255, 255, 0.88);
  font-size: 14px;
  font-weight: 700;
}

.record-pill.watched {
  background: rgba(16, 185, 129, 0.18);
  color: #d9ffe0;
}

.record-pill.want {
  background: rgba(245, 200, 76, 0.18);
  color: #fff2bd;
}

.record-pill.not-interested {
  background: rgba(229, 115, 115, 0.18);
  color: #ffe0dc;
}

.record-stars,
.side-stars {
  display: inline-flex;
  color: rgba(255, 255, 255, 0.35);
}

.record-stars .filled,
.side-stars .filled {
  color: #f6c84e;
}

.detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 24px;
  margin-top: 24px;
  align-items: start;
}

.detail-section,
.side-panel {
  border: 1px solid #e7ebf0;
  border-radius: 8px;
  background: #fff;
}

.detail-section {
  padding: 24px;
}

.detail-section + .detail-section,
.side-panel + .side-panel {
  margin-top: 18px;
}

.main-column > .mobile-credit-sections + .detail-section {
  margin-top: 18px;
}

.detail-section h2,
.side-panel h2 {
  margin: 0 0 14px;
  color: #172033;
  font-size: 22px;
  line-height: 1.25;
}

.overview {
  margin: 0;
  color: #3f4a5c;
  font-size: 15px;
  line-height: 1.85;
  white-space: pre-line;
}

.facts-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 18px;
}

.facts-grid div {
  min-height: 50px;
  display: grid;
  align-content: center;
  border-bottom: 1px solid #eef1f5;
}

.facts-grid span {
  display: block;
  color: #7a8493;
  font-size: 13px;
}

.facts-grid strong {
  display: block;
  margin-top: 4px;
  color: #172033;
  line-height: 1.45;
  font-size: 15px;
  font-weight: 750;
}

.collection-section {
  overflow: hidden;
  padding: 0;
}

.collection-header {
  padding: 22px 24px 16px;
  border-bottom: 1px solid #eef1f5;
}

.collection-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.collection-title-row h2 {
  margin: 0;
}

.collection-pill {
  min-height: 28px;
  padding: 0 11px;
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  background: #edf5ff;
  color: #1677ff;
  font-size: 13px;
  font-weight: 800;
}

.collection-strip {
  padding: 18px 24px 24px;
}

.collection-track {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;
}

.collection-card {
  min-height: 66px;
  padding: 8px 12px 8px 8px;
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  border: 1px solid #e6ebf1;
  border-radius: 8px;
  background: #fbfcfe;
  color: inherit;
  text-decoration: none;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.collection-card:hover {
  transform: translateY(-2px);
  border-color: #b9d6ff;
  box-shadow: 0 14px 22px rgba(22, 119, 255, 0.12);
}

.collection-thumb {
  position: relative;
  width: 48px;
  height: 56px;
}

.collection-thumb img,
.collection-thumb-placeholder {
  width: 100%;
  height: 100%;
  border-radius: 6px;
  object-fit: cover;
  background: #dfe7f1;
}

.collection-thumb-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #516176;
  font-size: 18px;
  font-weight: 850;
}

.collection-order {
  position: absolute;
  left: 6px;
  top: 50%;
  transform: translateY(-50%);
  width: 26px;
  height: 26px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(15, 23, 42, 0.82);
  color: #fff;
  font-size: 12px;
  font-weight: 850;
}

.collection-card-info {
  min-width: 0;
}

.collection-card-title {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  color: #172033;
  font-size: 15px;
  font-weight: 850;
}

.collection-card-date {
  margin-top: 5px;
  color: #8a94a6;
  font-size: 12px;
}

.collection-card-side {
  min-width: 54px;
  display: grid;
  gap: 6px;
  justify-items: end;
}

.collection-card-year {
  color: #6b7280;
  font-size: 13px;
  font-weight: 650;
}

.collection-card-rating {
  color: #f59e0b;
  font-size: 16px;
  font-weight: 850;
}

.collection-expand-wrap {
  margin: 0;
  padding: 0 24px 24px;
}

.people-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 14px;
}

.person-card,
.compact-person {
  color: inherit;
  text-decoration: none;
}

.person-card {
  overflow: hidden;
  border: 1px solid #e7ebf0;
  border-radius: 8px;
  background: #fff;
  transition: box-shadow 0.2s ease, transform 0.2s ease;
}

.person-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 18px rgba(15, 23, 42, 0.1);
}

.person-card img,
.avatar-placeholder {
  width: 100%;
  aspect-ratio: 2 / 3;
  object-fit: cover;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #eef1f5;
  color: #516176;
  font-size: 28px;
  font-weight: 900;
}

.avatar-placeholder,
.compact-placeholder {
  background:
    radial-gradient(circle at 50% 26%, rgba(255, 255, 255, 0.52), transparent 24%),
    linear-gradient(145deg, #dfe6ee, #b9c6d5);
}

.person-info {
  padding: 9px;
}

.person-info strong,
.person-info span {
  display: block;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.person-info strong {
  color: #172033;
  font-size: 13px;
  font-weight: 750;
}

.person-info span {
  margin-top: 4px;
  color: #778190;
  font-size: 12px;
}

.expand-btn {
  display: block;
  min-width: 140px;
  height: 40px;
  margin: 16px auto 0;
  border: 1px solid #dcdfe6;
  border-radius: 999px;
  background: #fff;
  color: #333;
  cursor: pointer;
  transition: all 0.2s ease;
}

.expand-btn:hover {
  border-color: #1677ff;
  color: #1677ff;
  background: #f8fbff;
}

.side-panel {
  padding: 18px;
}

.compact-person {
  display: grid;
  grid-template-columns: 46px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  margin-top: 10px;
}

.compact-person img,
.compact-placeholder {
  width: 46px;
  height: 56px;
  border-radius: 6px;
  object-fit: cover;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #eef1f5;
  color: #516176;
  font-weight: 900;
}

.compact-person strong {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  color: #172033;
  font-size: 14px;
}

.empty {
  margin: 0;
  color: #778190;
}

.mobile-credit-sections {
  display: none;
}

.detail-sidebar {
  display: grid;
  gap: 18px;
}

.detail-sidebar .side-panel + .side-panel {
  margin-top: 0;
}

.record-card {
  padding: 14px;
  border: 1px solid #e7ebf0;
  border-radius: 8px;
  background: #f7f8fa;
}

.record-card.watched {
  border-color: #d9edd5;
  background: #f6fbf6;
}

.record-card.want {
  border-color: #f1dfab;
  background: #fffaf0;
}

.record-card.not-interested {
  border-color: #ecc9c4;
  background: #fff5f4;
}

.record-status {
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  background: #e8ecf1;
  color: #374151;
  font-size: 13px;
  font-weight: 800;
}

.record-card.watched .record-status {
  background: #83dc7d;
  color: #173f1c;
}

.record-card.want .record-status {
  background: #ffe28a;
  color: #674d00;
}

.record-card.not-interested .record-status {
  background: #f0b5ab;
  color: #6d2419;
}

.record-date {
  margin-top: 10px;
  color: #4b5563;
  font-size: 14px;
  font-weight: 650;
}

.side-stars {
  margin-top: 8px;
  color: #d8dee7;
  font-size: 21px;
}

.watched-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  z-index: 1000;
}

.watched-modal {
  position: relative;
  width: 100%;
  max-width: 420px;
  background: #fff;
  border-radius: 24px;
  padding: 28px 24px 22px;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.18);
}

.watched-modal-close {
  position: absolute;
  top: 14px;
  right: 14px;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 50%;
  background: #f5f5f5;
  color: #666;
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
  transition: all 0.2s ease;
}

.watched-modal-close:hover {
  background: #ebebeb;
  color: #333;
}

.watched-modal-header h3 {
  margin: 0;
  color: #222;
  font-size: 22px;
  font-weight: 700;
}

.watched-modal-header p {
  margin: 8px 0 0;
  color: #777;
  font-size: 14px;
  line-height: 1.5;
}

.watched-form-section {
  margin-top: 22px;
}

.watched-label {
  display: block;
  margin-bottom: 10px;
  color: #444;
  font-size: 14px;
  font-weight: 600;
}

.watched-stars-row {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.watched-stars {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.watched-star-btn {
  padding: 0;
  border: none;
  background: transparent;
  color: #d6d6d6;
  cursor: pointer;
  font-size: 32px;
  line-height: 1;
  transition: transform 0.15s ease, color 0.2s ease;
}

.watched-star-btn:hover {
  transform: translateY(-1px) scale(1.04);
}

.watched-star-btn.active {
  color: #f3c64d;
}

.watched-rating-text {
  min-width: 48px;
  color: #666;
  font-size: 14px;
  font-weight: 600;
}

.clear-rating-btn {
  margin-top: 12px;
  padding: 6px 12px;
  border: none;
  border-radius: 999px;
  background: #f3f3f3;
  color: #666;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s ease;
}

.clear-rating-btn:hover {
  background: #e9e9e9;
  color: #333;
}

.watched-date-input-shell {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 44px;
  width: 100%;
  height: 44px;
  overflow: hidden;
  border: 1px solid #e3e3e3;
  border-radius: 12px;
  background: #fff;
  box-sizing: border-box;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.watched-date-input-shell:focus-within {
  border-color: #8fdc86;
  box-shadow: 0 0 0 3px rgba(143, 220, 134, 0.2);
}

.watched-date-display-button,
.watched-date-picker-button {
  border: none;
  background: transparent;
  color: #333;
  cursor: pointer;
}

.watched-date-display-button {
  min-width: 0;
  padding: 0 14px;
  text-align: left;
  font-size: 14px;
}

.watched-date-picker-button {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.watched-date-picker-button::before {
  content: '';
  width: 14px;
  height: 14px;
  border: 2px solid currentColor;
  border-radius: 2px;
}

.watched-date-picker-button::after {
  content: '';
  position: absolute;
  top: 14px;
  width: 14px;
  border-top: 2px solid currentColor;
}

.native-date-input {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 1px;
  height: 1px;
  opacity: 0;
  pointer-events: none;
}

.watched-modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 28px;
}

.watched-cancel-btn,
.watched-confirm-btn {
  min-width: 88px;
  height: 42px;
  border: none;
  border-radius: 999px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.2s ease;
}

.watched-cancel-btn {
  background: #f3f3f3;
  color: #555;
}

.watched-cancel-btn:hover {
  background: #e8e8e8;
}

.watched-confirm-btn {
  background: #8fdc86;
  color: #18461d;
}

.watched-confirm-btn:hover {
  background: #7fd575;
}

@media (max-width: 1000px) {
  .hero-inner {
    grid-template-columns: 220px minmax(0, 1fr);
    padding: 34px;
  }

  .poster-box {
    width: 220px;
  }

  .title-row h1 {
    font-size: 34px;
  }

  .title-row span {
    font-size: 24px;
  }

  .detail-layout {
    grid-template-columns: 1fr;
  }

  .side-column {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 18px;
  }

  .side-panel + .side-panel {
    margin-top: 0;
  }

  .people-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .tv-detail-page {
    gap: 0;
  }

  .detail-hero {
    margin-inline: -2px;
    border-radius: 0;
  }

  .detail-hero::before {
    background-image:
      linear-gradient(180deg, rgba(16, 25, 35, 0.78), rgba(16, 25, 35, 0.96)),
      var(--detail-backdrop, linear-gradient(#101923, #101923));
    filter: blur(14px) saturate(1.05);
    transform: scale(1.08);
  }

  .hero-inner {
    min-height: 0;
    grid-template-columns: 112px minmax(0, 1fr);
    grid-template-rows: auto auto auto;
    gap: 0 14px;
    padding: 20px 14px 18px;
    align-content: start;
  }

  .poster-box {
    grid-column: 1;
    grid-row: 1;
    width: 112px;
    align-self: start;
  }

  .poster-box img,
  .poster-placeholder {
    box-shadow: 0 14px 32px rgba(0, 0, 0, 0.35);
  }

  .hero-main {
    grid-column: 2;
    grid-row: 1;
    min-width: 0;
    padding-bottom: 0;
  }

  .title-row {
    gap: 6px;
  }

  .title-row h1 {
    font-size: 25px;
    line-height: 1.18;
  }

  .title-row span {
    color: rgba(255, 255, 255, 0.68);
    font-size: 18px;
    font-weight: 750;
  }

  .original-title {
    margin: 4px 0 0;
    color: rgba(255, 255, 255, 0.72);
    font-size: 13px;
  }

  .meta-tags {
    margin-top: 12px;
    gap: 8px;
    max-height: 105px;
    overflow: hidden;
  }

  .meta-tags span,
  .meta-tags a {
    min-height: 30px;
    padding: 0 11px;
    font-size: 13px;
  }

  .meta-tags a {
    display: none;
  }

  .score-grid,
  .action-panel {
    grid-column: 1 / -1;
  }

  .score-grid {
    grid-row: 2;
    max-width: none;
    margin-top: 16px;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 8px;
  }

  .score-grid div {
    min-height: 58px;
    padding: 9px 10px;
  }

  .score-grid span {
    font-size: 12px;
  }

  .score-grid strong {
    margin-top: 6px;
    font-size: 18px;
  }

  .score-card .release-score {
    font-size: 15px;
  }

  .action-panel {
    grid-row: 3;
    margin-top: 14px;
    padding: 10px;
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 8px;
    background: rgba(255, 255, 255, 0.08);
  }

  .actions {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 8px;
  }

  .actions button {
    width: 100%;
    height: 38px;
    padding: 0 10px;
    font-size: 14px;
  }

  .actions button:hover {
    transform: none;
  }

  .record-pill {
    width: 100%;
    min-height: 34px;
    padding: 0 11px;
    margin-top: 9px;
    font-size: 13px;
  }

  .record-stars {
    margin-left: auto;
  }

  .detail-layout {
    display: block;
  }

  .detail-section + .detail-section {
    margin-top: 14px;
  }

  .main-column > .mobile-credit-sections + .detail-section {
    margin-top: 14px;
  }

  .detail-section {
    padding: 18px;
  }

  .collection-section {
    padding: 0;
  }

  .collection-header {
    padding: 18px 18px 12px;
  }

  .collection-strip {
    padding: 14px 18px 18px;
  }

  .collection-track {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .collection-card {
    gap: 10px;
  }

  .collection-card:hover {
    transform: none;
    box-shadow: none;
  }

  .collection-expand-wrap {
    padding: 0 18px 18px;
  }

  .detail-section h2 {
    font-size: 22px;
  }

  .overview {
    font-size: 15px;
    line-height: 1.8;
  }

  .facts-grid {
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .facts-grid div {
    min-height: 52px;
  }

  .side-column {
    display: none;
  }

  .mobile-credit-sections {
    display: block;
  }

  .people-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 10px;
  }

  .person-info {
    padding: 8px;
  }

  .person-info strong {
    font-size: 13px;
  }

  .person-info span {
    font-size: 11px;
  }

  .person-card:hover {
    transform: none;
    box-shadow: none;
  }

  .watched-modal-overlay {
    align-items: flex-end;
    padding: 12px;
  }

  .watched-modal {
    max-width: none;
    border-radius: 18px;
    padding: 24px 18px 18px;
  }

  .watched-modal-header h3 {
    font-size: 20px;
  }

  .watched-star-btn {
    font-size: 28px;
  }
}

@media (max-width: 430px) {
  .people-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .watched-modal-actions {
    gap: 8px;
  }

  .watched-cancel-btn,
  .watched-confirm-btn {
    flex: 1;
    min-width: 0;
  }
}
</style>
