<script setup lang="ts">
import { onMounted, ref, computed, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getMovieById, type MovieDetail } from '@/api/movie'
import {getUserMovie, saveUserMovie, type UserMovieResponse} from '@/api/userMovie'

const route = useRoute()

const movie = ref<MovieDetail | null>(null)
const loading = ref(false)
const errorMessage = ref('')

const visibleCastCount = ref(12)
const castListRef = ref<HTMLElement | null>(null)
const castListMaxHeight = ref('none')

const displayCast = computed(() => {
  if (!movie.value || !movie.value.cast) {
    return []
  }

  return movie.value.cast.slice(0, visibleCastCount.value)
})

const hasMoreCast = computed(() => {
  if (!movie.value || !movie.value.cast) {
    return false
  }

  return visibleCastCount.value < movie.value.cast.length
})

const userMovie = ref<UserMovieResponse | null>(null)

const showWatchedPanel = ref(false)
const watchedRating = ref<number | null>(null)
const watchedYear = ref(new Date().getFullYear())
const watchedMonth = ref(new Date().getMonth() + 1)
const watchedDay = ref(new Date().getDate())

const hoveredRating = ref<number | null>(null)

const dateYearOptions = computed(() => {
  const currentYear = new Date().getFullYear()
  const options: number[] = []

  for (let year = currentYear; year >= 1900; year--) {
    options.push(year)
  }

  return options
})
const dateMonthOptions = Array.from({ length: 12 }, (_, index) => index + 1)
const dateDayOptions = computed(() => {
  const totalDays = getDaysInMonth(watchedYear.value, watchedMonth.value)
  return Array.from({ length: totalDays }, (_, index) => index + 1)
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

function getImageUrl(path: string | null, size: string | null) {
  if (!path) {
    return ''
  }
  return `https://image.tmdb.org/t/p/${size}${path}`
}

function formatVoteAverage(value: number | null) {
  if (value === null || value === undefined) {
    return '暂无'
  }
  return Number(value).toFixed(2)
}

function formatList(list: string[] | null | undefined) {
  if (!list || list.length === 0) {
    return '暂无'
  }
  return list.join(' / ')
}

async function expandCast() {
  if (!movie.value || !movie.value.cast) {
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
  // 如果之前有数据，带出来（编辑场景）
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

function formatStatusDate(dateStr: string) {
  if (!dateStr) return ''

  const date = new Date(dateStr)

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')

  return `${year}-${month}-${day}`
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
    <div v-else-if="movie">
      <div class="movie-detail-card">
        <div class="movie-detail">
          <div class="poster-section">
            <img
                v-if="movie.posterPath"
                :src="getImageUrl(movie.posterPath, 'w500')"
                :alt="movie.title"
                class="poster"
            />
            <div v-else class="poster placeholder">No Image</div>
          </div>

          <div class="info-section">
            <h1 class="title">
              {{ movie.title }}
              <span v-if="movie.releaseDate">
                ({{ movie.releaseDate.slice(0, 4) }})
              </span>
            </h1>
            <div v-if="userMovie" class="movie-status-line">
              <span
                class="movie-status-badge"
                :class="{
                  watched: userMovie.status === 'WATCHED',
                  want: userMovie.status === 'WANT_TO_WATCH',
                  notInterested: userMovie.status === 'NOT_INTERESTED'
                }"
              >
                {{ getStatusText(userMovie.status) }}
              </span>

              <template v-if="userMovie.status === 'WATCHED'">
                <span v-if="userMovie.rating != null" class="movie-status-rating">
                  <span
                    v-for="star in 5"
                    :key="star"
                    class="status-star"
                    :class="{ filled: star <= userMovie.rating }"
                  >
                    ★
                  </span>
                  <span class="status-rating-text">{{ userMovie.rating }} 分</span>
                </span>

                <span v-if="userMovie.watchedDate" class="movie-status-date">
                  {{ formatStatusDate(userMovie.watchedDate) }}
                </span>
              </template>

              <template v-else-if="userMovie.status === 'WANT_TO_WATCH'">
                <span v-if="userMovie.updatedAt" class="movie-status-date">
                  {{ formatStatusDate(userMovie.updatedAt) }}
                </span>
              </template>
            </div>

            <p><strong>原名：</strong>{{ movie.originalTitle || '未知' }}</p>
            <p><strong>评分：</strong>{{ formatVoteAverage(movie.voteAverage) }}</p>
            <p><strong>评分人数：</strong>{{ movie.voteCount ?? '暂无' }}</p>
            <p><strong>上映日期：</strong>{{ movie.releaseDate || '未知' }}</p>
            <p><strong>片长：</strong>{{ movie.runtime ? `${movie.runtime} 分钟` : '未知' }}</p>
            <p><strong>类型：</strong>{{ formatList(movie.genres) }}</p>
            <p><strong>国家/地区：</strong>{{ formatList(movie.countries) }}</p>
            <p><strong>公司：</strong>{{ formatList(movie.companies) }}</p>
            <p><strong>IMDb ID：</strong>{{ movie.imdbId || '暂无' }}</p>
          </div>
        </div>
        <div class="action-bar">
          <div class="movie-action-buttons">
            <button v-if="userMovie?.status !== 'WANT_TO_WATCH'"
              class="status-btn want-btn" 
              @click="handleWantToWatch"
            >
              想看
            </button>
            <button v-if="userMovie?.status !== 'NOT_INTERESTED'"
              class="status-btn not-interested-btn"
              @click="handleNotInterested"
            >
              不想看
            </button>
            <button class="status-btn watched-btn" :class="{ active: userMovie?.status === 'WATCHED' }"
              @click="handleWatched"
            >
              {{ userMovie?.status === 'WATCHED' ? '修改评分' : '看过' }}
            </button>
          </div>
        </div>
        <div class="overview-block">
          <h3>剧情简介</h3>
          <p>{{ movie.overview || '暂无简介' }}</p>
        </div>
        <div class="people-block">
          <h3>导演</h3>
          <div v-if="movie.directors && movie.directors.length > 0" class="director-list">
            <div
                v-for="person in movie.directors"
                :key="`director-${person.personId}`"
                class="person-card"
            >
              <img
                  v-if="person.profilePath"
                  :src="getImageUrl(person.profilePath, 'w185')"
                  :alt="person.name"
                  class="person-avatar"
              />
              <div v-else class="person-avatar placeholder">No Image</div>

              <div class="person-info">
                <div class="person-name">{{ person.name }}</div>
<!--                <div class="person-role">{{'导演' }}</div>-->
              </div>
            </div>
          </div>
          <p v-else>暂无导演信息</p>
        </div>
        <div class="people-block">
          <h3>演员</h3>

          <div
              v-if="movie.cast && movie.cast.length > 0"
              class="cast-section"
          >
            <div
                ref="castListRef"
                class="people-list cast-list"
                :style="{ maxHeight: castListMaxHeight }"
            >
              <div
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
                <div v-else class="person-avatar placeholder"></div>

                <div class="person-info">
                  <div class="person-name">{{ person.name || '未知姓名' }}</div>
                  <div class="person-role">{{ person.originalName || '未知姓名' }}</div>
                  <div class="person-role">{{ person.character ? `饰演 ${person.character}` : '' }}</div>
                </div>
              </div>
            </div>

            <div v-if="hasMoreCast" class="expand-wrap">
              <button class="expand-btn" @click="expandCast">
                展开更多
              </button>
            </div>
          </div>

          <p v-else>暂无演员信息</p>
        </div>
      </div>
    </div>
  </div>
  <div v-if="showWatchedPanel" class="watched-modal-overlay" @click="showWatchedPanel = false">
    <div class="watched-modal" @click.stop>
      <button class="watched-modal-close" @click="showWatchedPanel = false">×</button>

      <div class="watched-modal-header">
        <h3>标记为看过</h3>
        <p>可以为这部电影添加评分和观看日期</p>
      </div>

      <div class="watched-form-section">
        <label class="watched-label">评分</label>

        <div class="watched-stars-row">
          <div
            class="watched-stars"
            @mouseleave="hoveredRating = null"
          >
            <button
              v-for="star in 5"
              :key="star"
              type="button"
              class="watched-star-btn"
              :class="{
                active: star <= (hoveredRating ?? watchedRating ?? 0)
              }"
              @mouseenter="hoveredRating = star"
              @click="watchedRating = star"
            >
              ★
            </button>
          </div>

          <span class="watched-rating-text">
            {{
              hoveredRating != null
                ? `${hoveredRating} 分`
                : watchedRating != null
                  ? `${watchedRating} 分`
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
        <div class="watched-date-selects">
          <select v-model.number="watchedYear">
            <option v-for="year in dateYearOptions" :key="year" :value="year">
              {{ year }}年
            </option>
          </select>
          <select v-model.number="watchedMonth">
            <option v-for="month in dateMonthOptions" :key="month" :value="month">
              {{ String(month).padStart(2, '0') }}月
            </option>
          </select>
          <select v-model.number="watchedDay">
            <option v-for="day in dateDayOptions" :key="day" :value="day">
              {{ String(day).padStart(2, '0') }}日
            </option>
          </select>
        </div>
      </div>

      <div class="watched-modal-actions">
        <button class="watched-cancel-btn" type="button" @click="showWatchedPanel = false">
          取消
        </button>
        <button class="watched-confirm-btn" type="button" @click="confirmWatched">
          保存
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.movie-detail-page {
  padding: 8px 0 24px;
}

.status {
  padding: 24px 0;
  color: #666;
}

.status.error {
  color: #d93025;
}

.movie-detail {
  display: flex;
  gap: 28px;
  align-items: flex-start;
}

.poster-section {
  width: 300px;
  flex-shrink: 0;
}

.poster {
  width: 100%;
  display: block;
  object-fit: cover;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.12);
}

.poster.placeholder {
  width: 100%;
  aspect-ratio: 2 / 3;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #e5e7eb;
  color: #999;
}

.info-section {
  flex: 1;
  min-width: 0;
}

.title {
  margin: 0 0 18px;
  font-size: 30px;
  line-height: 1.3;
  color: #111;
}

.info-section p {
  margin: 0 0 10px;
  line-height: 1.7;
  color: #333;
}

.overview-block {
  margin-top: 24px;
}

.overview-block h3 {
  margin: 0 0 12px;
  font-size: 20px;
  color: #111;
}

.overview-block p {
  line-height: 1.8;
  color: #444;
  white-space: pre-line;
}

.people-block {
  margin-top: 28px;
}

.people-block h3 {
  margin: 0 0 14px;
  font-size: 20px;
  color: #111;
}

.people-list {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 14px;
}

.person-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  padding: 8px;
  border-radius: 10px;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.person-card:hover {
  box-shadow: 0 12px 16px rgba(0, 0, 0, 0.10);
}

.person-avatar {
  width: 100%;
  aspect-ratio: 2 / 3;
  object-fit: cover;
  display: block;
  background: #e5e7eb;
}

.person-avatar.placeholder {
  background: #e5e7eb;
}

.person-info {
  margin-top: 10px;
}

.person-name {
  font-size: 14px;
  font-weight: 600;
  color: #111;
  line-height: 1.4;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.person-role {
  margin-top: 4px;
  font-size: 12px;
  color: #666;
  line-height: 1.4;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.director-list {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.director-list .person-card {
  width: 160px;
}

.director-list .person-avatar {
  aspect-ratio: 1 / 1.2;
}

.movie-detail-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 24px;
  margin-bottom: 28px;
}

.cast-section {
  margin-top: 4px;
}

.cast-list {
  overflow: hidden;
  transition: max-height 0.35s ease;
  padding-bottom: 2px;
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

.action-bar {
  margin-top: 20px;
  display: flex;
  gap: 12px;
}

.status-btn {
  padding: 10px 18px;
  border: 1px solid transparent;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-weight: 600;
}

.status-btn:hover {
  transform: translateY(-1px);
}

/* 想看 */
.want-btn {
  background: #fff8db;
  color: #9a7400;
  border-color: #f4df8a;
}

.want-btn:hover {
  background: #fff1b8;
  border-color: #e8c84f;
  color: #7a5b00;
}

.want-btn.active {
  background: #ffd86b;
  border-color: #e0b93f;
  color: #5f4700;
  box-shadow: 0 8px 18px rgba(255, 216, 107, 0.35);
}

/* 看过 */
.watched-btn {
  background: #e3f7df;
  color: #2f7a35;
  border-color: #b7e2b1;
}

.watched-btn:hover {
  background: #cdefc7;
  border-color: #84c97c;
  color: #245f29;
}

.watched-btn.active {
  background: #8fdc86;
  border-color: #63b85b;
  color: #18461d;
  box-shadow: 0 8px 18px rgba(143, 220, 134, 0.35);
}

/* 不想看 */
.not-interested-btn {
  background: #f6e2de;
  color: #9a4a3a;
  border-color: #e3b8af;
}

.not-interested-btn:hover {
  background: #f1cfc8;
  border-color: #d68f80;
  color: #7e3628;
}

.not-interested-btn.active {
  background: #e59d8d;
  border-color: #cb7867;
  color: #572117;
  box-shadow: 0 8px 18px rgba(229, 157, 141, 0.3);
}

.movie-status-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 700;
}

.movie-status-badge.want {
  background: #fff1b8;
  color: #7a5b00;
}

.movie-status-badge.watched {
  background: #cdefc7;
  color: #245f29;
}

.movie-status-badge.notInterested {
  background: #f1cfc8;
  color: #7e3628;
}

.movie-status-badge.empty {
  background: #f2f2f2;
  color: #666;
}

.movie-status-line {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin: 10px 0 16px;
}

.movie-status-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
  line-height: 1;
}

.movie-status-badge.watched {
  background: #dff4dc;
  color: #256c2a;
}

.movie-status-badge.want {
  background: #fff4c7;
  color: #8a6d00;
}

.movie-status-badge.notInterested {
  background: #f3d9d3;
  color: #9a4a3a;
}

.movie-status-rating {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 4px 10px;
  border-radius: 999px;
  background: #fafafa;
}

.status-star {
  font-size: 15px;
  line-height: 1;
  color: #d0d0d0;
}

.status-star.filled {
  color: #f5c84c;
}

.status-rating-text {
  margin-left: 4px;
  font-size: 13px;
  font-weight: 600;
  color: #666;
}

.movie-status-date {
  font-size: 13px;
  color: #777;
}

/* .star {
  font-size: 16px;
  color: #d0d0d0;
  line-height: 1;
}

.star.filled {
  color: #f5c84c;
} */

.movie-status-date {
  color: #666;
  font-size: 14px;
}

.movie-action-buttons {
  display: flex;
  gap: 8px;   /* 控制按钮间距 */
  flex-wrap: wrap;
  margin-top: 12px;
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

.watched-stars {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.watched-star-btn {
  border: none;
  background: transparent;
  font-size: 30px;
  line-height: 1;
  color: #d6d6d6;
  cursor: pointer;
  padding: 0;
  transition: transform 0.15s ease, color 0.2s ease;
}

.watched-star-btn:hover {
  transform: translateY(-1px);
  color: #f3c64d;
}

.watched-star-btn.active {
  color: #f3c64d;
}

.clear-rating-btn {
  margin-left: 4px;
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

.watched-date-selects {
  display: grid;
  grid-template-columns: 1.15fr 0.9fr 0.9fr;
  gap: 10px;
}

.watched-date-selects select {
  width: 100%;
  height: 44px;
  padding: 0 14px;
  border: 1px solid #e3e3e3;
  border-radius: 12px;
  font-size: 14px;
  color: #333;
  background: #fff;
  box-sizing: border-box;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.watched-date-selects select:focus {
  outline: none;
  border-color: #8fdc86;
  box-shadow: 0 0 0 3px rgba(143, 220, 134, 0.2);
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

.watched-stars-row {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.watched-rating-text {
  font-size: 14px;
  font-weight: 600;
  color: #666;
  min-width: 48px;
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

@media (max-width: 900px) {
  .people-list {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .movie-detail-page {
    padding: 0 0 20px;
  }

  .movie-detail-card {
    padding: 16px;
    margin-bottom: 18px;
  }

  .movie-detail {
    flex-direction: column;
    gap: 18px;
  }

  .poster-section {
    width: min(68vw, 260px);
    margin: 0 auto;
  }

  .title {
    margin-bottom: 14px;
    font-size: 24px;
  }

  .info-section p {
    margin-bottom: 8px;
    line-height: 1.55;
  }

  .overview-block,
  .people-block {
    margin-top: 22px;
  }

  .overview-block h3,
  .people-block h3 {
    font-size: 18px;
  }

  .people-list {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 10px;
  }

  .person-card {
    padding: 6px;
    border-radius: 8px;
  }

  .person-info {
    margin-top: 8px;
  }

  .person-name {
    font-size: 13px;
  }

  .person-role {
    font-size: 11px;
  }

  .director-list {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 10px;
  }

  .director-list .person-card {
    width: auto;
  }

  .action-bar {
    margin-top: 16px;
  }

  .movie-action-buttons {
    width: 100%;
  }

  .status-btn {
    flex: 1 1 30%;
    min-width: 96px;
    padding: 10px 12px;
    white-space: nowrap;
  }

  .movie-status-line {
    gap: 8px;
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
  .poster-section {
    width: min(76vw, 240px);
  }

  .title {
    font-size: 22px;
  }

  .people-list,
  .director-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .status-btn {
    flex-basis: 100%;
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
