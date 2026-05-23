<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getMovieById, type MovieDetail, type PersonItem } from '@/api/movie'
import { getUserMovie, saveUserMovie, type UserMovieResponse } from '@/api/userMovie'

const route = useRoute()

const movie = ref<MovieDetail | null>(null)
const loading = ref(false)
const errorMessage = ref('')

const visibleCastCount = ref(12)
const castListRef = ref<HTMLElement | null>(null)
const castListMaxHeight = ref('none')

const userMovie = ref<UserMovieResponse | null>(null)

const showWatchedPanel = ref(false)
const watchedRating = ref<number | null>(null)
const watchedYear = ref(new Date().getFullYear())
const watchedMonth = ref(new Date().getMonth() + 1)
const watchedDay = ref(new Date().getDate())
const hoveredRating = ref<number | null>(null)

const displayCast = computed(() => {
  if (!movie.value?.cast) {
    return []
  }

  return movie.value.cast.slice(0, visibleCastCount.value)
})

const displayDirectors = computed(() => movie.value?.directors ?? [])

const hasMoreCast = computed(() => {
  if (!movie.value?.cast) {
    return false
  }

  return visibleCastCount.value < movie.value.cast.length
})

const watchedDateDisplay = computed(() => buildWatchedDateValue().replaceAll('-', '/'))

const releaseYear = computed(() => {
  if (!movie.value?.releaseDate) {
    return '年份未知'
  }

  return movie.value.releaseDate.slice(0, 4)
})

const releaseDateDisplay = computed(() => formatReleaseDate(movie.value?.releaseDate))

const heroStyle = computed(() => {
  const styles: Record<string, string> = {}

  if (movie.value?.posterPath) {
    styles['--detail-backdrop'] = `url("${getImageUrl(movie.value.posterPath, 'w500')}")`
  }

  return styles
})

async function loadMovieDetail() {
  loading.value = true
  errorMessage.value = ''

  try {
    const id = Number(route.params.id)
    movie.value = await getMovieById(id)
  } catch (error) {
    console.error(error)
    errorMessage.value = '加载电影详情失败'
  } finally {
    loading.value = false
  }
}

function getImageUrl(path: string | null, size: string) {
  if (!path) {
    return ''
  }

  return `https://image.tmdb.org/t/p/${size}${path}`
}

function formatVoteAverage(value: number | null | undefined) {
  if (value === null || value === undefined) {
    return '暂无'
  }

  return Number(value).toFixed(2)
}

function formatVoteCount(value: number | null | undefined) {
  if (value === null || value === undefined) {
    return '暂无'
  }

  return Number(value).toLocaleString('zh-CN')
}

function formatRuntime(value: number | null | undefined) {
  if (!value) {
    return '未知'
  }

  return `${value} 分钟`
}

function formatRevenue(value: number | null | undefined) {
  if (!value) {
    return '暂无'
  }

  return `$${Number(value).toLocaleString('zh-CN')}`
}

function formatDate(value: string | null | undefined) {
  if (!value) {
    return '未知'
  }

  const [year, month, day] = value.slice(0, 10).split('-')
  if (!year || !month || !day) {
    return value
  }

  return `${year}/${month}/${day}`
}

function formatReleaseDate(value: string | null | undefined) {
  if (!value) {
    return '未知'
  }

  const [year, month, day] = value.slice(0, 10).split('-')
  if (!year || !month || !day) {
    return value
  }

  return `${year}-${month}-${day}`
}

function formatList(list: string[] | null | undefined) {
  if (!list || list.length === 0) {
    return '暂无'
  }

  return list.join(' / ')
}

function getRatingValue(value: number | null | undefined) {
  if (value === null || value === undefined) {
    return 0
  }

  return Math.max(0, Math.min(5, Math.round(value)))
}

function getStatusText(status: 'WANT_TO_WATCH' | 'WATCHED' | 'NOT_INTERESTED') {
  switch (status) {
    case 'WANT_TO_WATCH':
      return '想看'
    case 'WATCHED':
      return '已看过'
    case 'NOT_INTERESTED':
      return '不想看'
    default:
      return '未标记'
  }
}

function getStatusClass(status: 'WANT_TO_WATCH' | 'WATCHED' | 'NOT_INTERESTED') {
  switch (status) {
    case 'WANT_TO_WATCH':
      return 'want'
    case 'WATCHED':
      return 'watched'
    case 'NOT_INTERESTED':
      return 'not-interested'
    default:
      return ''
  }
}

function getRecordDate() {
  if (!userMovie.value) {
    return ''
  }

  if (userMovie.value.status === 'WATCHED') {
    return userMovie.value.watchedDate ? formatDate(userMovie.value.watchedDate) : ''
  }

  return userMovie.value.updatedAt ? formatDate(userMovie.value.updatedAt) : ''
}

function getPersonRole(person: PersonItem) {
  if (person.character) {
    return `饰 ${person.character}`
  }

  return person.job || person.originalName || ''
}

function getPersonSubtitle(person: PersonItem) {
  if (person.originalName && person.originalName !== person.name) {
    return person.originalName
  }

  return person.job || ''
}

async function expandCast() {
  if (!movie.value?.cast) {
    return
  }

  visibleCastCount.value += 12

  await nextTick()
  updateCastListMaxHeight()
}

function updateCastListMaxHeight() {
  if (!castListRef.value) {
    return
  }

  castListMaxHeight.value = `${castListRef.value.scrollHeight}px`
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

  if (
    !year ||
    !month ||
    !day ||
    month < 1 ||
    month > 12 ||
    day < 1 ||
    day > getDaysInMonth(year, month)
  ) {
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
  if (!picker) {
    return
  }

  if (typeof picker.showPicker === 'function') {
    picker.showPicker()
  } else {
    picker.click()
  }
}

function handleNativeDateChange(event: Event) {
  const value = (event.target as HTMLInputElement).value
  if (value) {
    setWatchedDateParts(value)
  }
}

async function handleWantToWatch() {
  const token = localStorage.getItem('token')
  if (!token) return
  if (movie.value) {
    await saveUserMovie(movie.value.id, {
      status: 'WANT_TO_WATCH'
    }, token)
    userMovie.value = await getUserMovie(movie.value.id, token)
  }
}

async function handleNotInterested() {
  const token = localStorage.getItem('token')
  if (!token) return
  if (movie.value) {
    await saveUserMovie(movie.value.id, {
      status: 'NOT_INTERESTED'
    }, token)

    userMovie.value = await getUserMovie(movie.value.id, token)
  }
}

function handleWatched() {
  showWatchedPanel.value = true

  if (userMovie.value?.status === 'WATCHED') {
    watchedRating.value = userMovie.value.rating
    setWatchedDateParts(userMovie.value.watchedDate)
  } else {
    watchedRating.value = null
    setWatchedDateParts(getTodayString())
  }
}

async function confirmWatched() {
  const token = localStorage.getItem('token')
  if (!token) return
  if (movie.value) {
    await saveUserMovie(
      movie.value.id,
      {
        status: 'WATCHED',
        rating: watchedRating.value,
        watchedDate: buildWatchedDateValue()
      },
      token
    )
    userMovie.value = await getUserMovie(movie.value.id, token)
  }
  showWatchedPanel.value = false
}

watch([watchedYear, watchedMonth], () => {
  const maxDay = getDaysInMonth(watchedYear.value, watchedMonth.value)
  if (watchedDay.value > maxDay) {
    watchedDay.value = maxDay
  }
})

onMounted(async () => {
  await loadMovieDetail()
  await nextTick()
  updateCastListMaxHeight()
  if (movie.value) {
    document.title = `${movie.value.title} - 详情`
  }

  const token = localStorage.getItem('token')
  if (!token) return

  try {
    if (movie.value) {
      userMovie.value = await getUserMovie(movie.value.id, token)
    }
  } catch (e) {
    console.error(e)
  }
})
</script>

<template>
  <div class="movie-detail-page">
    <div v-if="loading" class="status">正在加载...</div>
    <div v-else-if="errorMessage" class="status error">{{ errorMessage }}</div>

    <template v-else-if="movie">
      <section class="detail-hero" :style="heroStyle">
        <div class="detail-hero-inner">
          <div class="poster-section">
            <img
              v-if="movie.posterPath"
              :src="getImageUrl(movie.posterPath, 'w500')"
              :alt="movie.title"
              class="poster"
            />
            <div v-else class="poster placeholder">No Image</div>
          </div>

          <div class="hero-summary">
            <div class="title-row">
              <h1>{{ movie.title }}</h1>
              <span class="release-year">{{ releaseYear }}</span>
            </div>
            <div class="original-title">{{ movie.originalTitle || '原名未知' }}</div>

            <div class="meta-tags">
              <span
                v-for="genre in movie.genres.slice(0, 3)"
                :key="genre"
                class="meta-tag"
              >
                {{ genre }}
              </span>
              <span v-if="movie.countries.length" class="meta-tag">
                {{ formatList(movie.countries) }}
              </span>
              <span v-if="movie.runtime" class="meta-tag">{{ formatRuntime(movie.runtime) }}</span>
              <a
                v-if="movie.imdbId"
                class="meta-tag meta-link"
                :href="`https://www.imdb.com/title/${movie.imdbId}/`"
                target="_blank"
                rel="noreferrer"
              >
                IMDb {{ movie.imdbId }}
              </a>
            </div>
          </div>

          <div class="score-grid">
              <div class="score-card">
                <span class="score-label">TMDB评分</span>
                <strong>{{ formatVoteAverage(movie.voteAverage) }}</strong>
              </div>
              <div class="score-card">
                <span class="score-label">评分人数</span>
                <strong>{{ formatVoteCount(movie.voteCount) }}</strong>
              </div>
              <div class="score-card">
                <span class="score-label">上映</span>
                <strong class="release-score">{{ releaseDateDisplay }}</strong>
              </div>
              <div class="score-card runtime-score">
                <span class="score-label">片长</span>
                <strong>{{ formatRuntime(movie.runtime) }}</strong>
              </div>
          </div>

          <div class="action-panel">
              <div class="hero-actions">
                <button
                  type="button"
                  class="action-btn"
                  :class="{ active: userMovie?.status === 'WANT_TO_WATCH' }"
                  @click="handleWantToWatch"
                >
                  想看
                </button>
                <button
                  type="button"
                  class="action-btn watched-action"
                  :class="{ active: userMovie?.status === 'WATCHED' }"
                  @click="handleWatched"
                >
                  看过
                </button>
                <button
                  type="button"
                  class="action-btn"
                  :class="{ active: userMovie?.status === 'NOT_INTERESTED' }"
                  @click="handleNotInterested"
                >
                  不想看
                </button>
              </div>

              <div
                v-if="userMovie"
                class="hero-record"
                :class="getStatusClass(userMovie.status)"
              >
                <span>{{ getStatusText(userMovie.status) }}</span>
                <span v-if="getRecordDate()">{{ getRecordDate() }}</span>
                <span v-if="userMovie.status === 'WATCHED'" class="record-stars">
                  <span
                    v-for="star in 5"
                    :key="star"
                    :class="{ filled: star <= getRatingValue(userMovie.rating) }"
                  >
                    ★
                  </span>
                </span>
              </div>
          </div>
        </div>
      </section>

      <div class="detail-content">
        <div class="detail-main">
          <section class="detail-section">
            <h2>剧情简介</h2>
            <p class="overview-text">{{ movie.overview || '暂无简介' }}</p>
          </section>

          <section class="detail-section">
            <h2>基础信息</h2>
            <div class="facts-grid">
              <div class="fact-item">
                <span>原名</span>
                <strong>{{ movie.originalTitle || '未知' }}</strong>
              </div>
              <div class="fact-item">
                <span>制片公司</span>
                <strong>{{ formatList(movie.companies) }}</strong>
              </div>
              <div class="fact-item">
                <span>国家/地区</span>
                <strong>{{ formatList(movie.countries) }}</strong>
              </div>
              <div class="fact-item">
                <span>类型</span>
                <strong>{{ formatList(movie.genres) }}</strong>
              </div>
              <div class="fact-item">
                <span>上映日期</span>
                <strong>{{ releaseDateDisplay }}</strong>
              </div>
              <div class="fact-item">
                <span>票房</span>
                <strong>{{ formatRevenue(movie.revenue) }}</strong>
              </div>
            </div>
          </section>

          <section v-if="displayDirectors.length > 0" class="detail-section mobile-directors">
            <h2>导演</h2>
            <div class="compact-people-list">
              <div
                v-for="person in displayDirectors"
                :key="`mobile-director-${person.personId}`"
                class="compact-person"
              >
                <img
                  v-if="person.profilePath"
                  :src="getImageUrl(person.profilePath, 'w185')"
                  :alt="person.name"
                />
                <div v-else class="compact-avatar placeholder">{{ person.name?.slice(0, 1) || '?' }}</div>
                <div>
                  <strong>{{ person.name }}</strong>
                  <span>{{ getPersonSubtitle(person) }}</span>
                </div>
              </div>
            </div>
          </section>

          <section class="detail-section">
            <h2>主要演员</h2>
            <div v-if="movie.cast && movie.cast.length > 0" class="cast-section">
              <div
                ref="castListRef"
                class="people-grid cast-list"
                :style="{ maxHeight: castListMaxHeight }"
              >
                <article
                  v-for="person in displayCast"
                  :key="`cast-${person.personId}-${person.name}`"
                  class="person-card"
                >
                  <img
                    v-if="person.profilePath"
                    :src="getImageUrl(person.profilePath, 'w185')"
                    :alt="person.name"
                    class="person-avatar"
                  />
                  <div v-else class="person-avatar placeholder">
                    {{ person.name?.slice(0, 1) || '?' }}
                  </div>

                  <div class="person-info">
                    <div class="person-name">{{ person.name || '未知姓名' }}</div>
                    <div class="person-role">{{ getPersonRole(person) }}</div>
                  </div>
                </article>
              </div>

              <div v-if="hasMoreCast" class="expand-wrap">
                <button type="button" class="expand-btn" @click="expandCast">
                  展开更多
                </button>
              </div>
            </div>
            <p v-else class="empty-text">暂无演员信息</p>
          </section>
        </div>

        <aside class="detail-sidebar">
          <section v-if="userMovie" class="side-panel my-record-panel">
            <h2>我的记录</h2>
            <div class="record-card" :class="getStatusClass(userMovie.status)">
              <span class="record-status">{{ getStatusText(userMovie.status) }}</span>
              <div v-if="getRecordDate()" class="record-date">
                标记日期：{{ getRecordDate() }}
              </div>
              <div v-if="userMovie.status === 'WATCHED'" class="side-stars">
                <span
                  v-for="star in 5"
                  :key="star"
                  :class="{ filled: star <= getRatingValue(userMovie.rating) }"
                >
                  ★
                </span>
              </div>
            </div>
          </section>

          <section v-if="displayDirectors.length > 0" class="side-panel">
            <h2>导演</h2>
            <div class="compact-people-list">
              <div
                v-for="person in displayDirectors"
                :key="`director-${person.personId}`"
                class="compact-person"
              >
                <img
                  v-if="person.profilePath"
                  :src="getImageUrl(person.profilePath, 'w185')"
                  :alt="person.name"
                />
                <div v-else class="compact-avatar placeholder">{{ person.name?.slice(0, 1) || '?' }}</div>
                <div>
                  <strong>{{ person.name }}</strong>
                  <span>{{ getPersonSubtitle(person) }}</span>
                </div>
              </div>
            </div>
          </section>
        </aside>
      </div>
    </template>
  </div>

  <div v-if="showWatchedPanel" class="watched-modal-overlay" @click="showWatchedPanel = false">
    <div class="watched-modal" @click.stop>
      <button type="button" class="watched-modal-close" @click="showWatchedPanel = false">×</button>

      <div class="watched-modal-header">
        <h3>标记为看过</h3>
        <p>可以为这部电影添加评分和观看日期</p>
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
            {{
              hoveredRating != null
                ? `${hoveredRating} 星`
                : watchedRating != null
                  ? `${watchedRating} 星`
                  : '未评分'
            }}
          </span>
        </div>

        <button
          v-if="watchedRating != null"
          type="button"
          class="clear-rating-btn"
          @click="watchedRating = null"
        >
          清除评分
        </button>
      </div>

      <div class="watched-form-section">
        <label class="watched-label">观看日期</label>
        <div class="watched-date-input-shell" @click.stop="openNativeDatePicker">
          <button type="button" class="watched-date-display-button">
            {{ watchedDateDisplay }}
          </button>
          <button type="button" class="watched-date-picker-button" aria-label="选择观看日期"></button>
          <input
            class="native-date-input"
            type="date"
            :value="buildWatchedDateValue()"
            @change="handleNativeDateChange"
          />
        </div>
      </div>

      <div class="watched-modal-actions">
        <button type="button" class="watched-cancel-btn" @click="showWatchedPanel = false">
          取消
        </button>
        <button type="button" class="watched-confirm-btn" @click="confirmWatched">
          保存
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.movie-detail-page {
  color: #172033;
}

.status {
  padding: 24px 0;
  color: #666;
}

.status.error {
  color: #d93025;
}

.detail-hero {
  position: relative;
  overflow: hidden;
  background: #101923;
  color: #fff;
  border-radius: 8px;
}

.detail-hero::before {
  content: '';
  position: absolute;
  inset: -28px;
  background-image:
    linear-gradient(90deg, rgba(13, 18, 27, 0.96), rgba(13, 18, 27, 0.72) 42%, rgba(13, 18, 27, 0.9)),
    var(--detail-backdrop, linear-gradient(#101923, #101923));
  background-size: cover;
  background-position: center;
  filter: blur(16px) saturate(1.08);
  transform: scale(1.05);
}

.detail-hero-inner {
  position: relative;
  min-height: 460px;
  padding: 44px 44px 40px;
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  grid-template-rows: auto auto auto;
  gap: 0 34px;
  align-content: end;
}

.poster-section {
  grid-row: 1 / 4;
  width: 260px;
  align-self: end;
}

.poster {
  width: 100%;
  aspect-ratio: 2 / 3;
  display: block;
  object-fit: cover;
  border-radius: 8px;
  box-shadow: 0 24px 54px rgba(0, 0, 0, 0.38);
}

.poster.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #e5e7eb;
  color: #6b7280;
}

.hero-summary {
  grid-column: 2;
  min-width: 0;
  padding-bottom: 4px;
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
  letter-spacing: 0;
}

.release-year {
  color: rgba(255, 255, 255, 0.64);
  font-size: 28px;
  font-weight: 700;
}

.original-title {
  margin-top: 8px;
  color: rgba(255, 255, 255, 0.74);
  font-size: 17px;
}

.meta-tags {
  margin-top: 22px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.meta-tag {
  min-height: 34px;
  padding: 0 13px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.12);
  color: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(255, 255, 255, 0.12);
  font-size: 14px;
  font-weight: 650;
  text-decoration: none;
}

.meta-link:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.18);
}

.score-grid {
  grid-column: 2;
  margin-top: 22px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  max-width: 740px;
}

.score-card {
  min-height: 78px;
  padding: 13px 16px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.1);
}

.score-label {
  display: block;
  color: rgba(255, 255, 255, 0.62);
  font-size: 13px;
}

.score-card strong {
  display: block;
  margin-top: 7px;
  color: #fff;
  font-size: 24px;
  font-weight: 800;
  line-height: 1.1;
}

.score-card .release-score {
  font-size: 21px;
  white-space: nowrap;
}

.action-panel {
  grid-column: 2;
  margin-top: 24px;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.action-btn {
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

.action-btn:hover {
  transform: translateY(-1px);
  background: rgba(255, 255, 255, 0.18);
}

.action-btn.active,
.watched-action.active {
  border-color: #80dc78;
  background: #86dc7f;
  color: #143819;
}

.hero-record {
  margin-top: 12px;
  min-height: 42px;
  width: fit-content;
  max-width: 100%;
  padding: 0 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  color: rgba(255, 255, 255, 0.88);
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 700;
}

.hero-record.watched {
  background: rgba(16, 185, 129, 0.18);
  color: #d9ffe0;
}

.hero-record.want {
  background: rgba(245, 200, 76, 0.18);
  color: #fff2bd;
}

.hero-record.not-interested {
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

.detail-content {
  margin-top: 24px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 24px;
  align-items: start;
}

.detail-section,
.side-panel {
  background: #fff;
  border: 1px solid #e7ebf0;
  border-radius: 8px;
}

.detail-section {
  padding: 24px;
}

.detail-section + .detail-section {
  margin-top: 18px;
}

.detail-section h2,
.side-panel h2 {
  margin: 0 0 14px;
  color: #172033;
  font-size: 22px;
  line-height: 1.25;
}

.overview-text {
  margin: 0;
  color: #3f4a5c;
  font-size: 15px;
  line-height: 1.85;
  white-space: pre-line;
}

.facts-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 20px;
}

.fact-item {
  min-height: 50px;
  display: grid;
  align-content: center;
  border-bottom: 1px solid #eef1f5;
}

.fact-item span {
  color: #7a8493;
  font-size: 13px;
}

.fact-item strong {
  margin-top: 4px;
  color: #172033;
  font-size: 15px;
  font-weight: 750;
  line-height: 1.45;
}

.mobile-directors {
  display: none;
}

.people-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 14px;
}

.cast-list {
  overflow: hidden;
  transition: max-height 0.35s ease;
  padding-bottom: 2px;
}

.person-card {
  overflow: hidden;
  background: #fff;
  border: 1px solid #e7ebf0;
  border-radius: 8px;
  transition: box-shadow 0.2s ease, transform 0.2s ease;
}

.person-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 18px rgba(15, 23, 42, 0.1);
}

.person-avatar {
  width: 100%;
  aspect-ratio: 2 / 3;
  object-fit: cover;
  display: block;
  background: #eef1f5;
}

.person-avatar.placeholder,
.compact-avatar.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(circle at 50% 26%, rgba(255, 255, 255, 0.52), transparent 24%),
    linear-gradient(145deg, #dfe6ee, #b9c6d5);
  color: #516176;
  font-weight: 850;
}

.person-avatar.placeholder {
  font-size: 32px;
}

.person-info {
  padding: 9px;
}

.person-name {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  color: #172033;
  font-size: 13px;
  font-weight: 750;
}

.person-role {
  margin-top: 4px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  color: #778190;
  font-size: 12px;
}

.expand-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}

.expand-btn {
  min-width: 140px;
  height: 40px;
  padding: 0 18px;
  border: 1px solid #dcdfe6;
  background: #fff;
  color: #333;
  font-size: 14px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.expand-btn:hover {
  border-color: #1677ff;
  color: #1677ff;
  background: #f8fbff;
}

.empty-text {
  margin: 0;
  color: #778190;
}

.detail-sidebar {
  display: grid;
  gap: 18px;
}

.side-panel {
  padding: 18px;
}

.record-card {
  padding: 14px;
  border-radius: 8px;
  background: #f7f8fa;
  border: 1px solid #e7ebf0;
}

.record-card.watched {
  background: #f6fbf6;
  border-color: #d9edd5;
}

.record-card.want {
  background: #fffaf0;
  border-color: #f1dfab;
}

.record-card.not-interested {
  background: #fff5f4;
  border-color: #ecc9c4;
}

.record-status {
  display: inline-flex;
  align-items: center;
  height: 28px;
  padding: 0 12px;
  border-radius: 999px;
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

.compact-people-list {
  display: grid;
  gap: 10px;
}

.compact-person {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
}

.compact-person img,
.compact-avatar {
  width: 48px;
  height: 58px;
  border-radius: 6px;
  object-fit: cover;
  background: #eef1f5;
}

.compact-person strong,
.compact-person span {
  display: block;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.compact-person strong {
  color: #172033;
  font-size: 14px;
}

.compact-person span {
  margin-top: 3px;
  color: #778190;
  font-size: 13px;
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
  background: #ffffff;
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
  font-size: 22px;
  font-weight: 700;
  color: #222;
}

.watched-modal-header p {
  margin: 8px 0 0;
  font-size: 14px;
  color: #777;
  line-height: 1.5;
}

.watched-form-section {
  margin-top: 22px;
}

.watched-label {
  display: block;
  margin-bottom: 10px;
  font-size: 14px;
  font-weight: 600;
  color: #444;
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
  border: none;
  background: transparent;
  font-size: 32px;
  line-height: 1;
  color: #d6d6d6;
  cursor: pointer;
  padding: 0;
  transition: transform 0.15s ease, color 0.2s ease;
}

.watched-star-btn:hover {
  transform: translateY(-1px) scale(1.04);
}

.watched-star-btn.active {
  color: #f3c64d;
}

.watched-rating-text {
  font-size: 14px;
  font-weight: 600;
  color: #666;
  min-width: 48px;
}

.clear-rating-btn {
  margin-top: 12px;
  padding: 6px 12px;
  border: none;
  border-radius: 999px;
  background: #f3f3f3;
  color: #666;
  font-size: 13px;
  cursor: pointer;
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
  border: 1px solid #e3e3e3;
  border-radius: 12px;
  background: #fff;
  box-sizing: border-box;
  overflow: hidden;
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
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
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
  .detail-hero-inner {
    grid-template-columns: 220px minmax(0, 1fr);
    padding: 34px;
  }

  .poster-section {
    width: 220px;
  }

  .title-row h1 {
    font-size: 34px;
  }

  .release-year {
    font-size: 24px;
  }

  .detail-content {
    grid-template-columns: 1fr;
  }

  .detail-sidebar {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .people-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
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

  .detail-hero-inner {
    min-height: 0;
    padding: 20px 14px 18px;
    grid-template-columns: 112px minmax(0, 1fr);
    grid-template-rows: auto auto auto;
    gap: 0 14px;
    align-content: start;
  }

  .poster-section {
    grid-column: 1;
    grid-row: 1;
    width: 112px;
    align-self: start;
  }

  .poster {
    box-shadow: 0 14px 32px rgba(0, 0, 0, 0.35);
  }

  .hero-summary {
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

  .release-year {
    color: rgba(255, 255, 255, 0.68);
    font-size: 18px;
    font-weight: 750;
  }

  .original-title {
    margin-top: 4px;
    color: rgba(255, 255, 255, 0.72);
    font-size: 13px;
  }

  .meta-tags {
    margin-top: 12px;
    gap: 8px;
  }

  .meta-tag {
    min-height: 30px;
    padding: 0 11px;
    font-size: 13px;
  }

  .meta-link {
    display: none;
  }

  .score-grid {
    grid-column: 1 / -1;
    grid-row: 2;
    margin-top: 16px;
    max-width: none;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 8px;
  }

  .runtime-score {
    display: none;
  }

  .score-card {
    min-height: 58px;
    padding: 9px 10px;
  }

  .score-label {
    font-size: 12px;
  }

  .score-card strong {
    margin-top: 6px;
    font-size: 18px;
  }

  .score-card .release-score {
    font-size: 15px;
  }

  .action-panel {
    grid-column: 1 / -1;
    grid-row: 3;
    margin-top: 14px;
    padding: 10px;
    border-radius: 8px;
    background: rgba(255, 255, 255, 0.08);
    border: 1px solid rgba(255, 255, 255, 0.1);
  }

  .hero-actions {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 8px;
  }

  .action-btn {
    width: 100%;
    height: 38px;
    padding: 0 10px;
    font-size: 14px;
  }

  .action-btn:hover {
    transform: none;
  }

  .hero-record {
    margin-top: 9px;
    width: 100%;
    min-height: 34px;
    padding: 0 11px;
    font-size: 13px;
  }

  .record-stars {
    margin-left: auto;
  }

  .detail-content {
    margin-top: 14px;
    display: block;
  }

  .detail-section {
    padding: 18px;
  }

  .detail-section + .detail-section {
    margin-top: 14px;
  }

  .detail-section h2 {
    font-size: 22px;
  }

  .overview-text {
    font-size: 15px;
    line-height: 1.8;
  }

  .facts-grid {
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .fact-item {
    min-height: 52px;
  }

  .mobile-directors {
    display: block;
  }

  .people-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 10px;
  }

  .person-card:hover {
    transform: none;
    box-shadow: none;
  }

  .person-info {
    padding: 8px;
  }

  .person-name {
    font-size: 13px;
  }

  .person-role {
    font-size: 11px;
  }

  .detail-sidebar {
    display: none;
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
  .detail-hero-inner {
    grid-template-columns: 112px minmax(0, 1fr);
  }

  .meta-tags {
    max-height: 105px;
    overflow: hidden;
  }

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
