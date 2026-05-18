<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { getMovies, type MovieItem } from '../api/movie'
import { getGenres } from '../api/genre'
import { isLoggedIn } from '../utils/auth'

type UserMovieStatusFilter = 'WANT_TO_WATCH' | 'WATCHED' | 'NOT_INTERESTED' | 'UNMARKED'

const categories = ref<{ id: number | null; name: string }[]>([
  { id: null, name: '全部' }
])
const selectedCategory = ref<{ id: number | null; name: string }>({
  id: null,
  name: '全部'
})

const regions = ref([
  { code: null, name: '全部' },
  { code: 'CN', name: '大陆' },
  { code: 'HK', name: '香港' },
  { code: 'TW', name: '台湾' },
  { code: 'US', name: '美国' },
  { code: 'GB', name: '英国' },
  { code: 'JP', name: '日本' },
  { code: 'KR', name: '韩国' },
  { code: 'FR', name: '法国' },
  { code: 'DE', name: '德国' },
  { code: 'IT', name: '意大利' },
  { code: 'ES', name: '西班牙' },
  { code: 'IN', name: '印度' },
  { code: 'RU', name: '俄罗斯' },
  { code: 'BR', name: '巴西' },
  { code: 'TH', name: '泰国' },
  { code: 'OTHER', name: '其他' }
])
const selectedRegion = ref<{ code: string | null; name: string }>({
  code: null,
  name: '全部'
})

const years = ref<number[]>([])
const selectedYear = ref<number | null>(null)

const sortOptions = ref([
  { sortBy: 'CREATED_AT', direction: 'desc', name: '默认' },
  { sortBy: 'RELEASE_DATE', direction: 'desc', name: '上映日期' },
  { sortBy: 'VOTE_COUNT', direction: 'desc', name: '评分人数' },
  { sortBy: 'VOTE_AVERAGE', direction: 'desc', name: '评分' }
])
const selectedSort = ref({
  sortBy: 'CREATED_AT',
  direction: 'desc',
  name: '默认'
})

const userMovieStatusOptions = ref<{ value: UserMovieStatusFilter | null; name: string }[]>([
  { value: null, name: '全部' },
  { value: 'WANT_TO_WATCH', name: '想看' },
  { value: 'WATCHED', name: '看过' },
  { value: 'NOT_INTERESTED', name: '不想看' },
  { value: 'UNMARKED', name: '未标记' }
])
const selectedUserMovieStatus = ref<{ value: UserMovieStatusFilter | null; name: string }>({
  value: null,
  name: '全部'
})
const isUserLoggedIn = ref(isLoggedIn())

const movies = ref<MovieItem[]>([])
const loading = ref(false)
const errorMessage = ref('')
const keyword = ref('')

const page = ref(0)
const size = ref(20)
const totalPages = ref(0)
const totalElements = ref(0)

let searchTimer: number | undefined

const jumpPage = ref('')
const visiblePages = computed(() => {
  const total = totalPages.value
  const current = page.value
  const pages: (number | string)[] = []

  if (total <= 7) {
    for (let i = 0; i < total; i++) {
      pages.push(i)
    }
    return pages
  }

  if (current <= 3) {
    pages.push(0, 1, 2, 3, 4, '...', total - 1)
    return pages
  }

  if (current >= total - 4) {
    pages.push(0, '...', total - 5, total - 4, total - 3, total - 2, total - 1)
    return pages
  }

  pages.push(0, '...', current - 1, current, current + 1, '...', total - 1)
  return pages
})

async function loadGenres() {
  const data = await getGenres()
  categories.value = [
    { id: null, name: '全部' },
    ...data.map(item => ({
      id: item.id,
      name: item.name
    }))
  ]
}

function initYears() {
  const currentYear = new Date().getFullYear()
  const startYear = 1900

  years.value = []
  for (let year = currentYear; year >= startYear; year--) {
    years.value.push(year)
  }
}

async function loadMovies() {
  loading.value = true
  errorMessage.value = ''

  try {
    const data = await getMovies({
      title: keyword.value.trim(),
      genreId: selectedCategory.value.id ?? undefined,
      year: selectedYear.value ?? undefined,
      regionCode: selectedRegion.value.code ?? undefined,
      userMovieStatus: isUserLoggedIn.value ? selectedUserMovieStatus.value.value ?? undefined : undefined,
      page: page.value,
      size: size.value,
      sortBy: selectedSort.value.sortBy,
      direction: selectedSort.value.direction
    })

    movies.value = data.content
    totalPages.value = data.totalPages
    totalElements.value = data.totalElements
  } catch (error) {
    errorMessage.value = '加载电影失败'
    console.error(error)
  } finally {
    loading.value = false
  }
}

function handleCategoryClick(category: { id: number | null; name: string }) {
  selectedCategory.value = category
  resetPageAndLoadMovies()
}

function handleRegionClick(region: {code: string | null; name: string }) {
  selectedRegion.value = region
  resetPageAndLoadMovies()
}

function handleYearChange() {
  resetPageAndLoadMovies()
}

function handleSortClick(sort: { sortBy: string; direction: string; name: string }) {
  selectedSort.value = sort
  resetPageAndLoadMovies()
}

function handleUserMovieStatusClick(status: { value: UserMovieStatusFilter | null; name: string }) {
  selectedUserMovieStatus.value = status
  selectedSort.value = sortOptions.value[0]
  resetPageAndLoadMovies()
}

function handleSearch() {
  resetPageAndLoadMovies()
}



function goToPreviousPage() {
  if (page.value > 0) {
    page.value--
    if (searchTimer) {
      clearTimeout(searchTimer)
    }
    loadMovies()
  }
}

function goToNextPage() {
  if (page.value < totalPages.value - 1) {
    page.value++
    if (searchTimer) {
      clearTimeout(searchTimer)
    }
    loadMovies()
  }
}

function goToPage(targetPage: number) {
  if (targetPage < 0 || targetPage >= totalPages.value || targetPage === page.value) {
    return
  }
  page.value = targetPage
  if (searchTimer) {
    clearTimeout(searchTimer)
  }
  loadMovies()
}

function handleJumpPage() {
  const pageNumber = Number(jumpPage.value)

  if (!Number.isInteger(pageNumber)) {
    return
  }

  if (pageNumber < 1 || pageNumber > totalPages.value) {
    return
  }

  goToPage(pageNumber - 1)
  jumpPage.value = ''
}

function getPosterUrl(path: string | null) {
  if (!path) {
    return ''
  }
  return `https://image.tmdb.org/t/p/w500${path}`
}

function formatWatchedDate(dateStr: string | null) {
  if (!dateStr) {
    return ''
  }
  return dateStr.slice(0, 10)
}

function handleKeywordInput() {
  page.value = 0

  if (searchTimer) {
    clearTimeout(searchTimer)
  }

  searchTimer = window.setTimeout(() => {
    loadMovies()
  }, 500)
}

function resetPageAndLoadMovies() {
  page.value = 0

  if (searchTimer) {
    clearTimeout(searchTimer)
  }

  loadMovies()
}

onMounted(async () => {
  isUserLoggedIn.value = isLoggedIn()
  await loadGenres()
  initYears()
  await loadMovies()
})
</script>

<template>
  <div class="movies-page">
    <div class="filter-bar">
      <div class="filter-row">
        <span class="label">类型：</span>
        <div class="options">
          <span
              v-for="item in categories"
              :key="item.id ?? 'all'"
              class="option"
              :class="{ active: selectedCategory.id === item.id }"
              @click="handleCategoryClick(item)"
          >
            {{ item.name }}
          </span>
        </div>
      </div>

      <div class="filter-row">
        <span class="label">地区：</span>
        <div class="options">
          <span
            v-for="item in regions"
            :key="item.code ?? 'all'"
            class="option"
            :class="{ active: selectedRegion.code === item.code }"
            @click="handleRegionClick(item)"
          >
            {{ item.name }}
          </span>
        </div>
      </div>

      <div class="filter-row">
        <span class="label">排序：</span>
        <div class="options">
          <span
              v-for="item in sortOptions"
              :key="item.sortBy"
              class="option"
              :class="{ active: selectedSort.sortBy === item.sortBy }"
              @click="handleSortClick(item)"
          >
            {{ item.name }}
          </span>
        </div>
      </div>

      <div v-if="isUserLoggedIn" class="filter-row">
        <span class="label">状态：</span>
        <div class="options">
          <span
              v-for="item in userMovieStatusOptions"
              :key="item.value ?? 'all'"
              class="option"
              :class="{ active: selectedUserMovieStatus.value === item.value }"
              @click="handleUserMovieStatusClick(item)"
          >
            {{ item.name }}
          </span>
        </div>
      </div>
    </div>

    <div class="toolbar">
      <select v-model="selectedYear" @change="handleYearChange">
        <option :value="null">全部年份</option>
        <option v-for="year in years" :key="year" :value="year">
          {{ year }}
        </option>
      </select>

      <input
          v-model="keyword"
          type="text"
          placeholder="请输入电影标题"
          @input="handleKeywordInput"
          @keyup.enter="handleSearch"
      />
<!--      <button @click="handleSearch">搜索</button>-->
    </div>

    <div class="summary">
      共 {{ totalElements }} 部电影
      <span v-if="selectedCategory.name !== '全部'">｜当前类型：{{ selectedCategory.name }}</span>
      <span v-if="selectedYear !== null">｜当前年份：{{ selectedYear }}</span>
      <span v-if="selectedRegion.name !== '全部'">｜当前地区：{{ selectedRegion.name }}</span>
      <span v-if="selectedSort.name !== '默认'">｜当前排序：{{ selectedSort.name }}
</span>
    </div>

    <div v-if="loading" class="status">正在加载...</div>
    <div v-else-if="errorMessage" class="status error">{{ errorMessage }}</div>
    <div v-else-if="movies.length === 0" class="status">没有找到电影</div>

    <div v-else class="movie-list">
<!--      <div v-for="movie in movies" :key="movie.id" class="movie-card" @click="goToDetail(movie.id)">-->
      <a v-for="movie in movies" :key="movie.id" class="movie-card"
         :href="`/movies/${movie.id}`" target="_blank">
        <div class="poster-wrapper">
          <img
              v-if="movie.posterPath"
              :src="getPosterUrl(movie.posterPath)"
              :alt="movie.title"
              class="poster"
          />
          <div v-else class="poster placeholder">No Image</div>
        </div>

        <div class="movie-meta">
          <div class="title-year">
            {{ movie.title }}<span v-if="movie.releaseDate"> ({{ movie.releaseDate.slice(0, 4) }})</span>
          </div>
          <div class="rating">
            {{ Number(movie.voteAverage ?? 0).toFixed(2) }}
          </div>
        </div>
        <div
          v-if="selectedUserMovieStatus.value === 'WATCHED' && (movie.userRating != null || movie.userWatchedDate)"
          class="watched-meta"
        >
          <span v-if="movie.userRating != null" class="watched-rating">
            <span class="watched-star">★</span>
            我的评分 {{ movie.userRating }}/5
          </span>
          <span
            v-if="movie.userRating != null && movie.userWatchedDate"
            class="watched-dot"
          ></span>
          <span v-if="movie.userWatchedDate" class="watched-date">
            {{ formatWatchedDate(movie.userWatchedDate) }} 看过
          </span>
        </div>
      </a>
    </div>

    <div class="pagination">
      <button :disabled="page === 0" @click="goToPreviousPage">上一页</button>

      <span
          v-for="(item, index) in visiblePages"
          :key="`${item}-${index}`"
          class="page-item"
          :class="{
            active: typeof item === 'number' && item === page,
            ellipsis: item === '...'
          }"
          @click="typeof item === 'number' && goToPage(item)"
      >
        {{ typeof item === 'number' ? item + 1 : item }}
      </span>

      <button :disabled="page >= totalPages - 1" @click="goToNextPage">下一页</button>

      <div class="jump-box">
        <span>跳至</span>
        <input v-model="jumpPage" type="text" />
        <span>页</span>
        <button @click="handleJumpPage">确定</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.movies-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.filter-bar {
  background: #fff;
  padding: 16px;
  border-radius: 8px;
}

.filter-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.label {
  font-size: 13px;
  line-height: 28px;
  flex-shrink: 0;
  white-space: nowrap;
}

.options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
}

.option {
  cursor: pointer;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 13px;
  white-space: nowrap;
}

.option:hover {
  color: #1677ff;
}

.option.active {
  background: #1677ff;
  color: #fff;
}

.toolbar {
  display: flex;
  gap: 12px;
  min-width: 0;
}

.toolbar input {
  width: 280px;
  height: 36px;
  padding: 0 12px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  outline: none;
}

.toolbar select {
  height: 36px;
  padding: 0 12px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  outline: none;
  background: #fff;
}

.toolbar button,
.pagination button {
  height: 36px;
  padding: 0 16px;
  border: none;
  border-radius: 6px;
  background: #1677ff;
  color: white;
  cursor: pointer;
}

.toolbar button:disabled,
.pagination button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.summary {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
}

.status {
  padding: 24px 0;
  color: #666;
}

.status.error {
  color: #d93025;
}

/* ===== 电影列表 ===== */
.movie-list {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 18px 14px;
}

.movie-card {
  text-decoration: none;
  color: inherit;
  display: block;
  flex-direction: column;
  cursor: pointer;
  background: transparent;
  border: none;
  box-shadow: none;
  padding: 0;
  min-width: 0;
  transition: transform 0.2s ease;
}

.movie-card:hover {
  transform: translateY(-4px);
}

.poster-wrapper {
  width: 100%;
  aspect-ratio: 2 / 3;
  overflow: hidden;
  border-radius: 10px;
  background: #eaeaea;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.poster {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  border-radius: 0;
}

.poster.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: #e5e7eb;
  color: #999;
  font-size: 14px;
}

.movie-meta {
  margin-top: 10px;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  min-width: 0;
}

.title-year {
  flex: 1;
  min-width: 0;
  font-size: 17px;
  font-weight: 600;
  color: #111;
  line-height: 1.4;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.rating {
  flex-shrink: 0;
  font-size: 17px;
  font-weight: 700;
  color: #f5a623;
}

.watched-meta {
  margin-top: 5px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  min-height: 20px;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.4;
}

.watched-rating {
  color: #4b5563;
  font-weight: 600;
}

.watched-star {
  color: #f5a623;
}

.watched-dot {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: #d1d5db;
}

.watched-date {
  color: #6b7280;
}

/* ===== 分页 ===== */
.pagination {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.page-item {
  min-width: 36px;
  height: 36px;
  padding: 0 10px;
  border-radius: 6px;
  background: #fff;
  border: 1px solid #dcdfe6;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  user-select: none;
}

.page-item:hover {
  color: #1677ff;
  border-color: #1677ff;
}

.page-item.active {
  background: #1677ff;
  color: #fff;
  border-color: #1677ff;
}

.page-item.ellipsis {
  cursor: default;
  border: none;
  background: transparent;
  color: #999;
}

.jump-box {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: 8px;
}

.jump-box input {
  width: 60px;
  height: 36px;
  padding: 0 8px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  outline: none;
}

/* ===== 可选：稍窄屏幕自动降列数 ===== */
@media (max-width: 1200px) {
  .movie-list {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 992px) {
  .movie-list {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .movies-page {
    gap: 12px;
  }

  .filter-bar {
    padding: 12px;
    border-radius: 8px;
  }

  .filter-row {
    display: grid;
    grid-template-columns: auto minmax(0, 1fr);
    align-items: start;
    margin-bottom: 10px;
  }

  .filter-row:last-child {
    margin-bottom: 0;
  }

  .options {
    flex-wrap: nowrap;
    overflow-x: auto;
    padding-bottom: 2px;
    scrollbar-width: none;
  }

  .options::-webkit-scrollbar {
    display: none;
  }

  .option {
    background: #f3f4f6;
  }

  .toolbar {
    display: grid;
    grid-template-columns: 120px minmax(0, 1fr);
    gap: 8px;
  }

  .toolbar input,
  .toolbar select {
    width: 100%;
    min-width: 0;
  }

  .movie-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 16px 10px;
  }

  .poster-wrapper {
    border-radius: 8px;
  }

  .movie-meta {
    margin-top: 8px;
    display: block;
    gap: 6px;
  }

  .title-year,
  .rating {
    font-size: 14px;
  }

  .rating {
    display: inline-flex;
    margin-top: 2px;
  }

  .watched-meta {
    margin-top: 4px;
    font-size: 12px;
    gap: 5px;
  }

  .pagination {
    gap: 6px;
  }

  .pagination button:first-of-type {
    order: 1;
    flex: 1 0 calc(50% - 3px);
  }

  .pagination button:last-of-type {
    order: 2;
    flex: 1 0 calc(50% - 3px);
  }

  .page-item {
    order: 3;
    min-width: 32px;
    height: 32px;
    padding: 0 8px;
  }

  .pagination button {
    height: 32px;
    padding: 0 12px;
    white-space: nowrap;
  }

  .jump-box {
    order: 4;
    width: 100%;
    margin-left: 0;
    justify-content: flex-start;
  }
}

@media (max-width: 380px) {
  .toolbar {
    grid-template-columns: 1fr;
  }

  .title-year,
  .rating {
    font-size: 13px;
  }
}
</style>
