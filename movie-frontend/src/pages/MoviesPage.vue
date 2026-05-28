<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref, computed, watch } from 'vue'
import { getMovies, type MovieItem } from '../api/movie'
import { getGenres } from '../api/genre'
import { saveUserMovie, type UserMovieRequest, type UserMovieStatus } from '../api/userMovie'
import { getToken, isLoggedIn } from '../utils/auth'

type UserMovieStatusFilter = 'DEFAULT' | 'WANT_TO_WATCH' | 'WATCHED' | 'NOT_INTERESTED' | 'UNMARKED'
type MarkedUserMovieStatus = 'WANT_TO_WATCH' | 'WATCHED' | 'NOT_INTERESTED'
type RatingRangeHandle = 'min' | 'max'
type RatingRangeDragState = {
  handle: RatingRangeHandle | null
  startX: number
  startMin: number
  startMax: number
}

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
  { value: 'DEFAULT', name: '默认' },
  { value: null, name: '全部' },
  { value: 'WANT_TO_WATCH', name: '想看' },
  { value: 'WATCHED', name: '看过' },
  { value: 'NOT_INTERESTED', name: '不想看' },
  { value: 'UNMARKED', name: '未标记' }
])
const selectedUserMovieStatus = ref<{ value: UserMovieStatusFilter | null; name: string }>({
  value: 'DEFAULT',
  name: '默认'
})
const userRatingMin = ref(1)
const userRatingMax = ref(5)
const movieRatingMin = ref(1)
const movieRatingMax = ref(10)
const movieRatingLabelMarks = [1, 5, 10]
const isUserLoggedIn = ref(isLoggedIn())
const ratingRangeControlRef = ref<HTMLElement | null>(null)
const ratingRangeDragState = ref<RatingRangeDragState | null>(null)
const ratingRangePopoverOpen = ref(false)
const movieRatingRangeControlRef = ref<HTMLElement | null>(null)
const movieRatingRangeDragState = ref<RatingRangeDragState | null>(null)
const movieRatingRangePopoverOpen = ref(false)

const movies = ref<MovieItem[]>([])
const loading = ref(false)
const errorMessage = ref('')
const keyword = ref('')

const page = ref(0)
const size = ref(20)
const totalPages = ref(0)
const totalElements = ref(0)
const activeActionMovieId = ref<number | null>(null)
const watchedEditorMovieId = ref<number | null>(null)
const watchedRating = ref<number | null>(null)
const hoveredWatchedRating = ref<number | null>(null)
const watchedYear = ref(new Date().getFullYear())
const watchedMonth = ref(new Date().getMonth() + 1)
const watchedDay = ref(new Date().getDate())
const savingMovieId = ref<number | null>(null)

let searchTimer: number | undefined

const jumpPage = ref('')
const watchedDateDisplay = computed(() => buildWatchedDateValue().replaceAll('-', '/'))
const showUserRatingRange = computed(() =>
  isUserLoggedIn.value && selectedUserMovieStatus.value.value === 'WATCHED'
)
const hasUserRatingRangeFilter = computed(() =>
  showUserRatingRange.value && (userRatingMin.value !== 1 || userRatingMax.value !== 5)
)
const userRatingRangeLabel = computed(() => {
  if (!hasUserRatingRangeFilter.value) {
    return '全部'
  }
  return userRatingMin.value === userRatingMax.value
    ? `${userRatingMin.value}星`
    : `${userRatingMin.value}-${userRatingMax.value}星`
})
const userRatingRangeStart = computed(() => `${((userRatingMin.value - 1) / 4) * 100}%`)
const userRatingRangeEnd = computed(() => `${100 - ((userRatingMax.value - 1) / 4) * 100}%`)
const hasMovieRatingRangeFilter = computed(() => movieRatingMin.value !== 1 || movieRatingMax.value !== 10)
const movieRatingRangeLabel = computed(() => {
  if (!hasMovieRatingRangeFilter.value) {
    return '全部'
  }
  return movieRatingMin.value === movieRatingMax.value
    ? `${movieRatingMin.value}分`
    : `${movieRatingMin.value}-${movieRatingMax.value}分`
})
const movieRatingRangeStart = computed(() => `${((movieRatingMin.value - 1) / 9) * 100}%`)
const movieRatingRangeEnd = computed(() => `${100 - ((movieRatingMax.value - 1) / 9) * 100}%`)
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
const recentYears = computed(() => years.value.slice(0, 10))
const olderYears = computed(() => years.value.slice(10))
const isOlderYearSelected = computed(() =>
  selectedYear.value !== null && !recentYears.value.includes(selectedYear.value)
)

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
      userRatingMin: hasUserRatingRangeFilter.value ? userRatingMin.value : undefined,
      userRatingMax: hasUserRatingRangeFilter.value ? userRatingMax.value : undefined,
      movieRatingMin: hasMovieRatingRangeFilter.value ? movieRatingMin.value : undefined,
      movieRatingMax: hasMovieRatingRangeFilter.value ? movieRatingMax.value : undefined,
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

function handleYearClick(year: number | null) {
  selectedYear.value = year
  resetPageAndLoadMovies()
}

function handleOlderYearChange(event: Event) {
  const value = (event.target as HTMLSelectElement).value
  if (!value) {
    return
  }

  selectedYear.value = Number(value)
  resetPageAndLoadMovies()
}

function handleSortClick(sort: { sortBy: string; direction: string; name: string }) {
  selectedSort.value = sort
  resetPageAndLoadMovies()
}

function handleUserMovieStatusClick(status: { value: UserMovieStatusFilter | null; name: string }) {
  selectedUserMovieStatus.value = status
  selectedSort.value = sortOptions.value[0]
  ratingRangePopoverOpen.value = false
  if (status.value !== 'WATCHED') {
    resetUserRatingRange(false)
  }
  resetPageAndLoadMovies()
}

function handleUserMovieStatusOptionClick(
  status: { value: UserMovieStatusFilter | null; name: string },
  event: MouseEvent
) {
  if (showUserRatingRange.value && status.value === 'WATCHED') {
    event.stopPropagation()
    toggleRatingRangePopover()
    return
  }

  handleUserMovieStatusClick(status)
}

function resetUserRatingRange(shouldLoad = true) {
  userRatingMin.value = 1
  userRatingMax.value = 5
  if (shouldLoad && showUserRatingRange.value) {
    resetPageAndLoadMovies()
  }
}

function clampRating(value: number) {
  return Math.min(5, Math.max(1, value))
}

function setRatingRangeControlRef(element: unknown) {
  ratingRangeControlRef.value = element instanceof HTMLElement ? element : null
}

function getRatingFromPointer(event: PointerEvent) {
  if (!ratingRangeControlRef.value) {
    return userRatingMin.value
  }

  const rect = ratingRangeControlRef.value.getBoundingClientRect()
  const ratio = Math.min(1, Math.max(0, (event.clientX - rect.left) / rect.width))
  return clampRating(Math.round(ratio * 4) + 1)
}

function chooseRatingRangeHandle(value: number): RatingRangeHandle | null {
  if (userRatingMin.value === userRatingMax.value) {
    if (value < userRatingMin.value) {
      return 'min'
    }
    if (value > userRatingMax.value) {
      return 'max'
    }
    return null
  }

  const minDistance = Math.abs(value - userRatingMin.value)
  const maxDistance = Math.abs(value - userRatingMax.value)
  if (minDistance < maxDistance) {
    return 'min'
  }
  if (maxDistance < minDistance) {
    return 'max'
  }
  return value < userRatingMin.value ? 'min' : 'max'
}

function updateRatingRangeHandle(handle: RatingRangeHandle, value: number) {
  const rating = clampRating(value)
  if (handle === 'min') {
    userRatingMin.value = Math.min(rating, userRatingMax.value)
  } else {
    userRatingMax.value = Math.max(rating, userRatingMin.value)
  }
}

function startRatingRangeDrag(event: PointerEvent, requestedHandle?: RatingRangeHandle) {
  event.preventDefault()

  const pointedRating = getRatingFromPointer(event)
  const handlesOverlap = userRatingMin.value === userRatingMax.value
  const handle = requestedHandle && !handlesOverlap
    ? requestedHandle
    : chooseRatingRangeHandle(pointedRating)

  ratingRangeDragState.value = {
    handle,
    startX: event.clientX,
    startMin: userRatingMin.value,
    startMax: userRatingMax.value
  }

  if (handle) {
    updateRatingRangeHandle(handle, pointedRating)
  }

  window.addEventListener('pointermove', handleRatingRangePointerMove)
  window.addEventListener('pointerup', finishRatingRangeDrag)
}

function handleRatingRangePointerMove(event: PointerEvent) {
  const dragState = ratingRangeDragState.value
  if (!dragState) {
    return
  }

  let handle = dragState.handle
  if (!handle) {
    const movement = event.clientX - dragState.startX
    if (Math.abs(movement) < 2) {
      return
    }
    handle = movement < 0 ? 'min' : 'max'
    dragState.handle = handle
  }

  updateRatingRangeHandle(handle, getRatingFromPointer(event))
}

function finishRatingRangeDrag() {
  const dragState = ratingRangeDragState.value
  ratingRangeDragState.value = null
  window.removeEventListener('pointermove', handleRatingRangePointerMove)
  window.removeEventListener('pointerup', finishRatingRangeDrag)

  if (
    dragState &&
    showUserRatingRange.value &&
    (dragState.startMin !== userRatingMin.value || dragState.startMax !== userRatingMax.value)
  ) {
    resetPageAndLoadMovies()
  }
}

function toggleRatingRangePopover() {
  ratingRangePopoverOpen.value = !ratingRangePopoverOpen.value
  if (ratingRangePopoverOpen.value) {
    movieRatingRangePopoverOpen.value = false
  }
}

function handleRatingRangeKeydown(handle: RatingRangeHandle, event: KeyboardEvent) {
  if (event.key !== 'ArrowLeft' && event.key !== 'ArrowDown' && event.key !== 'ArrowRight' && event.key !== 'ArrowUp') {
    return
  }

  event.preventDefault()
  const delta = event.key === 'ArrowLeft' || event.key === 'ArrowDown' ? -1 : 1
  const nextValue = handle === 'min'
    ? userRatingMin.value + delta
    : userRatingMax.value + delta
  const previousMin = userRatingMin.value
  const previousMax = userRatingMax.value

  updateRatingRangeHandle(handle, nextValue)
  if (previousMin !== userRatingMin.value || previousMax !== userRatingMax.value) {
    resetPageAndLoadMovies()
  }
}

function clampMovieRating(value: number) {
  return Math.min(10, Math.max(1, value))
}

function getMovieRatingPointPosition(score: number) {
  return `${((score - 1) / 9) * 100}%`
}

function getMovieRatingLabelStyle(score: number) {
  let transform = 'translateX(-50%)'
  if (score === 1) {
    transform = 'translateX(0)'
  } else if (score === 10) {
    transform = 'translateX(-100%)'
  }

  return {
    left: getMovieRatingPointPosition(score),
    transform
  }
}

function setMovieRatingRangeControlRef(element: unknown) {
  movieRatingRangeControlRef.value = element instanceof HTMLElement ? element : null
}

function getMovieRatingFromPointer(event: PointerEvent) {
  if (!movieRatingRangeControlRef.value) {
    return movieRatingMin.value
  }

  const rect = movieRatingRangeControlRef.value.getBoundingClientRect()
  const ratio = Math.min(1, Math.max(0, (event.clientX - rect.left) / rect.width))
  return clampMovieRating(Math.round(ratio * 9) + 1)
}

function chooseMovieRatingRangeHandle(value: number): RatingRangeHandle | null {
  if (movieRatingMin.value === movieRatingMax.value) {
    if (value < movieRatingMin.value) {
      return 'min'
    }
    if (value > movieRatingMax.value) {
      return 'max'
    }
    return null
  }

  const minDistance = Math.abs(value - movieRatingMin.value)
  const maxDistance = Math.abs(value - movieRatingMax.value)
  if (minDistance < maxDistance) {
    return 'min'
  }
  if (maxDistance < minDistance) {
    return 'max'
  }
  return value < movieRatingMin.value ? 'min' : 'max'
}

function updateMovieRatingRangeHandle(handle: RatingRangeHandle, value: number) {
  const rating = clampMovieRating(value)
  if (handle === 'min') {
    movieRatingMin.value = Math.min(rating, movieRatingMax.value)
  } else {
    movieRatingMax.value = Math.max(rating, movieRatingMin.value)
  }
}

function startMovieRatingRangeDrag(event: PointerEvent, requestedHandle?: RatingRangeHandle) {
  event.preventDefault()

  const pointedRating = getMovieRatingFromPointer(event)
  const handlesOverlap = movieRatingMin.value === movieRatingMax.value
  const handle = requestedHandle && !handlesOverlap
    ? requestedHandle
    : chooseMovieRatingRangeHandle(pointedRating)

  movieRatingRangeDragState.value = {
    handle,
    startX: event.clientX,
    startMin: movieRatingMin.value,
    startMax: movieRatingMax.value
  }

  if (handle) {
    updateMovieRatingRangeHandle(handle, pointedRating)
  }

  window.addEventListener('pointermove', handleMovieRatingRangePointerMove)
  window.addEventListener('pointerup', finishMovieRatingRangeDrag)
}

function handleMovieRatingRangePointerMove(event: PointerEvent) {
  const dragState = movieRatingRangeDragState.value
  if (!dragState) {
    return
  }

  let handle = dragState.handle
  if (!handle) {
    const movement = event.clientX - dragState.startX
    if (Math.abs(movement) < 2) {
      return
    }
    handle = movement < 0 ? 'min' : 'max'
    dragState.handle = handle
  }

  updateMovieRatingRangeHandle(handle, getMovieRatingFromPointer(event))
}

function finishMovieRatingRangeDrag() {
  const dragState = movieRatingRangeDragState.value
  movieRatingRangeDragState.value = null
  window.removeEventListener('pointermove', handleMovieRatingRangePointerMove)
  window.removeEventListener('pointerup', finishMovieRatingRangeDrag)

  if (
    dragState &&
    (dragState.startMin !== movieRatingMin.value || dragState.startMax !== movieRatingMax.value)
  ) {
    resetPageAndLoadMovies()
  }
}

function toggleMovieRatingRangePopover() {
  movieRatingRangePopoverOpen.value = !movieRatingRangePopoverOpen.value
  if (movieRatingRangePopoverOpen.value) {
    ratingRangePopoverOpen.value = false
  }
}

function handleMovieRatingRangeKeydown(handle: RatingRangeHandle, event: KeyboardEvent) {
  if (event.key !== 'ArrowLeft' && event.key !== 'ArrowDown' && event.key !== 'ArrowRight' && event.key !== 'ArrowUp') {
    return
  }

  event.preventDefault()
  const delta = event.key === 'ArrowLeft' || event.key === 'ArrowDown' ? -1 : 1
  const nextValue = handle === 'min'
    ? movieRatingMin.value + delta
    : movieRatingMax.value + delta
  const previousMin = movieRatingMin.value
  const previousMax = movieRatingMax.value

  updateMovieRatingRangeHandle(handle, nextValue)
  if (previousMin !== movieRatingMin.value || previousMax !== movieRatingMax.value) {
    resetPageAndLoadMovies()
  }
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

function formatStatusDate(dateStr: string | null) {
  if (!dateStr) {
    return ''
  }
  return dateStr.slice(0, 10).replaceAll('-', '/')
}

function getDisplayVoteAverage(movie: MovieItem) {
  return movie.imdbVoteAverage ?? movie.voteAverage
}

function formatDisplayVoteAverage(movie: MovieItem) {
  return Number(getDisplayVoteAverage(movie) ?? 0).toFixed(2)
}

function getStatusLabel(status: MarkedUserMovieStatus | null) {
  if (status === 'WANT_TO_WATCH') {
    return '想看'
  }
  if (status === 'WATCHED') {
    return '看过'
  }
  if (status === 'NOT_INTERESTED') {
    return '不想看'
  }
  return ''
}

function getStatusClass(status: MarkedUserMovieStatus | null) {
  return status ? `status-${status.toLowerCase().replaceAll('_', '-')}` : ''
}

function getStatusDate(movie: MovieItem) {
  if (movie.userMovieStatus === 'WATCHED') {
    return movie.userWatchedDate ?? movie.userMovieUpdatedAt
  }
  return movie.userMovieUpdatedAt
}

function getRatingValue(rating: number | null) {
  if (rating === null) {
    return 0
  }
  return Math.max(0, Math.min(5, Math.round(rating)))
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

function closeActionPanels() {
  activeActionMovieId.value = null
  watchedEditorMovieId.value = null
  hoveredWatchedRating.value = null
  ratingRangePopoverOpen.value = false
  movieRatingRangePopoverOpen.value = false
}

function toggleActionMenu(movieId: number) {
  watchedEditorMovieId.value = null
  activeActionMovieId.value = activeActionMovieId.value === movieId ? null : movieId
}

function openWatchedEditor(movie: MovieItem) {
  activeActionMovieId.value = null
  watchedEditorMovieId.value = movie.id
  watchedRating.value = movie.userRating ?? null
  hoveredWatchedRating.value = null
  setWatchedDateParts(movie.userWatchedDate)
}

async function saveMovieStatus(movieId: number, data: UserMovieRequest) {
  const token = getToken()
  if (!token || savingMovieId.value !== null) {
    return
  }

  savingMovieId.value = movieId

  try {
    await saveUserMovie(movieId, data, token)
    closeActionPanels()
    await loadMovies()
  } catch (error) {
    console.error(error)
    window.alert('保存电影状态失败，请稍后再试')
  } finally {
    savingMovieId.value = null
  }
}

async function handleQuickStatus(movie: MovieItem, status: UserMovieStatus) {
  if (status === 'WATCHED') {
    openWatchedEditor(movie)
    return
  }

  await saveMovieStatus(movie.id, { status })
}

async function confirmWatchedStatus(movieId: number) {
  await saveMovieStatus(movieId, {
    status: 'WATCHED',
    rating: watchedRating.value,
    watchedDate: buildWatchedDateValue()
  })
}

watch([watchedYear, watchedMonth], () => {
  const maxDay = getDaysInMonth(watchedYear.value, watchedMonth.value)
  if (watchedDay.value > maxDay) {
    watchedDay.value = maxDay
  }
})

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
  document.addEventListener('click', closeActionPanels)
  isUserLoggedIn.value = isLoggedIn()
  await loadGenres()
  initYears()
  await loadMovies()
})

onBeforeUnmount(() => {
  document.removeEventListener('click', closeActionPanels)
  window.removeEventListener('pointermove', handleRatingRangePointerMove)
  window.removeEventListener('pointerup', finishRatingRangeDrag)
  window.removeEventListener('pointermove', handleMovieRatingRangePointerMove)
  window.removeEventListener('pointerup', finishMovieRatingRangeDrag)
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
        <span class="label">年份：</span>
        <div class="options year-options">
          <span
            class="option"
            :class="{ active: selectedYear === null }"
            @click="handleYearClick(null)"
          >
            全部
          </span>
          <span
            v-for="year in recentYears"
            :key="year"
            class="option year-number-option"
            :class="{ active: selectedYear === year }"
            @click="handleYearClick(year)"
          >
            {{ year }}
          </span>

          <select
            class="older-year-select"
            :class="{ active: isOlderYearSelected }"
            :value="isOlderYearSelected ? selectedYear : ''"
            @change="handleOlderYearChange"
          >
            <option value="">更早年份</option>
            <option
              v-for="year in olderYears"
              :key="year"
              :value="year"
            >
              {{ year }}
            </option>
          </select>
        </div>
      </div>

      <div v-if="isUserLoggedIn" class="filter-row user-status-filter-row">
        <span class="label">状态：</span>
        <div class="options">
          <template
            v-for="item in userMovieStatusOptions"
            :key="item.value ?? 'all'"
          >
            <span
              class="option user-status-option"
              :class="{
                active: selectedUserMovieStatus.value === item.value,
                'watched-rating-option': showUserRatingRange && item.value === 'WATCHED'
              }"
              @click="handleUserMovieStatusOptionClick(item, $event)"
            >
              <span>{{ item.name }}</span>
              <template v-if="showUserRatingRange && item.value === 'WATCHED'">
                <span class="mobile-status-rating-separator">·</span>
                <strong class="mobile-status-rating-label">{{ userRatingRangeLabel }}</strong>
                <span class="rating-filter-arrow mobile-status-rating-arrow"></span>
              </template>
            </span>

            <div
              v-if="showUserRatingRange && item.value === 'WATCHED'"
              class="rating-filter-inline user-rating-filter"
              @click.stop
            >
              <button
                type="button"
                class="rating-filter-trigger"
                :class="{ active: ratingRangePopoverOpen || hasUserRatingRangeFilter }"
                @click="toggleRatingRangePopover"
              >
                <span>我的评分</span>
                <strong>{{ userRatingRangeLabel }}</strong>
                <span class="rating-filter-arrow"></span>
              </button>

              <div
                v-if="ratingRangePopoverOpen"
                class="rating-range-popover"
              >
                <div class="rating-range-popover-title">我的评分</div>
                <div class="rating-range-panel">
                  <div
                    :ref="setRatingRangeControlRef"
                    class="rating-range-control"
                    :style="{
                      '--range-start': userRatingRangeStart,
                      '--range-end': userRatingRangeEnd
                    }"
                    @pointerdown="startRatingRangeDrag"
                  >
                    <div class="rating-range-track">
                      <span
                        v-for="star in 5"
                        :key="star"
                        class="rating-range-tick"
                      ></span>
                    </div>
                    <div class="rating-range-active"></div>
                    <button
                      type="button"
                      class="rating-range-handle rating-range-handle-min"
                      :class="{ dragging: ratingRangeDragState?.handle === 'min' }"
                      aria-label="最低我的评分"
                      :aria-valuenow="userRatingMin"
                      aria-valuemin="1"
                      aria-valuemax="5"
                      @pointerdown.stop="startRatingRangeDrag($event, 'min')"
                      @keydown="handleRatingRangeKeydown('min', $event)"
                    ></button>
                    <button
                      type="button"
                      class="rating-range-handle rating-range-handle-max"
                      :class="{ dragging: ratingRangeDragState?.handle === 'max' }"
                      aria-label="最高我的评分"
                      :aria-valuenow="userRatingMax"
                      aria-valuemin="1"
                      aria-valuemax="5"
                      @pointerdown.stop="startRatingRangeDrag($event, 'max')"
                      @keydown="handleRatingRangeKeydown('max', $event)"
                    ></button>
                  </div>
                  <div class="rating-range-labels">
                    <span
                      v-for="star in 5"
                      :key="star"
                      :class="{ active: star >= userRatingMin && star <= userRatingMax }"
                    >
                      {{ star }}星
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </template>
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

      <div class="filter-row movie-rating-filter-row">
        <span class="label">评分：</span>
        <div class="options movie-rating-options">
          <div class="rating-filter-inline movie-rating-filter" @click.stop>
            <button
              type="button"
              class="rating-filter-trigger"
              :class="{ active: movieRatingRangePopoverOpen || hasMovieRatingRangeFilter }"
              @click="toggleMovieRatingRangePopover"
            >
              <strong>{{ movieRatingRangeLabel }}</strong>
              <span class="rating-filter-arrow"></span>
            </button>

            <div
              v-if="movieRatingRangePopoverOpen"
              class="rating-range-popover"
            >
              <div class="rating-range-popover-title">评分</div>
              <div class="rating-range-panel">
                <div
                  :ref="setMovieRatingRangeControlRef"
                  class="rating-range-control"
                  :style="{
                    '--range-start': movieRatingRangeStart,
                    '--range-end': movieRatingRangeEnd
                  }"
                  @pointerdown="startMovieRatingRangeDrag"
                >
                  <div class="rating-range-track">
                    <span
                      v-for="score in 10"
                      :key="score"
                      class="rating-range-tick"
                      :style="{ left: getMovieRatingPointPosition(score) }"
                    ></span>
                  </div>
                  <div class="rating-range-active"></div>
                  <button
                    type="button"
                    class="rating-range-handle rating-range-handle-min"
                    :class="{ dragging: movieRatingRangeDragState?.handle === 'min' }"
                    aria-label="最低评分"
                    :aria-valuenow="movieRatingMin"
                    aria-valuemin="1"
                    aria-valuemax="10"
                    @pointerdown.stop="startMovieRatingRangeDrag($event, 'min')"
                    @keydown="handleMovieRatingRangeKeydown('min', $event)"
                  ></button>
                  <button
                    type="button"
                    class="rating-range-handle rating-range-handle-max"
                    :class="{ dragging: movieRatingRangeDragState?.handle === 'max' }"
                    aria-label="最高评分"
                    :aria-valuenow="movieRatingMax"
                    aria-valuemin="1"
                    aria-valuemax="10"
                    @pointerdown.stop="startMovieRatingRangeDrag($event, 'max')"
                    @keydown="handleMovieRatingRangeKeydown('max', $event)"
                  ></button>
                </div>
                <div class="rating-range-labels">
                  <span
                    v-for="score in movieRatingLabelMarks"
                    :key="score"
                    :style="getMovieRatingLabelStyle(score)"
                    :class="{ active: score >= movieRatingMin && score <= movieRatingMax }"
                  >
                    {{ score }}分
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="filter-row search-filter-row">
        <span class="label">搜索：</span>
        <div class="options search-options">
          <input
          v-model="keyword"
          type="text"
          class="title-search-input"
          placeholder="请输入电影标题"
          @input="handleKeywordInput"
          @keyup.enter="handleSearch"
          />
        </div>
      </div>
    </div>

    <div class="summary">
      共 {{ totalElements }} 部电影
      <span v-if="selectedCategory.name !== '全部'">｜当前类型：{{ selectedCategory.name }}</span>
      <span v-if="selectedYear !== null">｜当前年份：{{ selectedYear }}</span>
      <span v-if="selectedRegion.name !== '全部'">｜当前地区：{{ selectedRegion.name }}</span>
      <span v-if="hasMovieRatingRangeFilter">｜当前评分：{{ movieRatingRangeLabel }}</span>
      <span v-if="selectedSort.name !== '默认'">｜当前排序：{{ selectedSort.name }}
</span>
    </div>

    <div v-if="loading" class="status">正在加载...</div>
    <div v-else-if="errorMessage" class="status error">{{ errorMessage }}</div>
    <div v-else-if="movies.length === 0" class="status">没有找到电影</div>

    <div v-else class="movie-list">
<!--      <div v-for="movie in movies" :key="movie.id" class="movie-card" @click="goToDetail(movie.id)">-->
      <div
        v-for="movie in movies"
        :key="movie.id"
        class="movie-card"
        :class="{ actionOpen: activeActionMovieId === movie.id || watchedEditorMovieId === movie.id }"
      >
        <div class="poster-shell">
          <a class="poster-link" :href="`/movies/${movie.id}`" target="_blank">
            <div class="poster-wrapper">
              <img
                  v-if="movie.posterPath"
                  :src="getPosterUrl(movie.posterPath)"
                  :alt="movie.title"
                  class="poster"
              />
              <div v-else class="poster placeholder">No Image</div>

              <div
                v-if="movie.userMovieStatus"
                class="poster-status-overlay"
                :class="getStatusClass(movie.userMovieStatus)"
              >
                <div class="poster-status-line">
                  <span class="poster-status-pill">
                    {{ getStatusLabel(movie.userMovieStatus) }}
                  </span>
                  <span v-if="getStatusDate(movie)" class="poster-status-date">
                    {{ formatStatusDate(getStatusDate(movie)) }}
                  </span>
                </div>
                <div v-if="movie.userMovieStatus === 'WATCHED'" class="poster-status-stars">
                  <span
                    v-for="star in 5"
                    :key="star"
                    :class="{ filled: star <= getRatingValue(movie.userRating) }"
                  >
                    ★
                  </span>
                </div>
              </div>
            </div>
          </a>

          <button
            v-if="isUserLoggedIn"
            type="button"
            class="poster-action-button"
            :class="{ active: activeActionMovieId === movie.id || watchedEditorMovieId === movie.id }"
            aria-label="标记电影"
            @click.stop="toggleActionMenu(movie.id)"
          >
            <span></span>
            <span></span>
            <span></span>
          </button>

          <div
            v-if="activeActionMovieId === movie.id"
            class="movie-action-menu"
            @click.stop
          >
            <button
              type="button"
              :disabled="savingMovieId === movie.id"
              @click="handleQuickStatus(movie, 'WANT_TO_WATCH')"
            >
              <span class="action-icon">＋</span>
              想看
            </button>
            <button
              type="button"
              :disabled="savingMovieId === movie.id"
              @click="handleQuickStatus(movie, 'WATCHED')"
            >
              <span class="action-icon">✓</span>
              看过
            </button>
            <button
              type="button"
              :disabled="savingMovieId === movie.id"
              @click="handleQuickStatus(movie, 'NOT_INTERESTED')"
            >
              <span class="action-icon">－</span>
              不想看
            </button>
          </div>

          <div
            v-if="watchedEditorMovieId === movie.id"
            class="watched-editor-popover"
            @click.stop
          >
            <div class="watched-editor-title">标记看过</div>
            <div class="watched-editor-field">
              <span>我的评分</span>
              <div class="watched-rating-picker" @mouseleave="hoveredWatchedRating = null">
                <button
                  v-for="star in 5"
                  :key="star"
                  type="button"
                  :class="{ selected: star <= (hoveredWatchedRating ?? watchedRating ?? 0) }"
                  @mouseenter="hoveredWatchedRating = star"
                  @click="watchedRating = star"
                >
                  ★
                </button>
              </div>
            </div>
            <div class="watched-editor-field">
              <span>看过日期</span>
              <div class="date-input-shell" @click.stop="openNativeDatePicker">
                <button type="button" class="date-display-button">
                  {{ watchedDateDisplay }}
                </button>
                <button type="button" class="date-picker-button" aria-label="选择看过日期"></button>
                <input
                  class="native-date-input"
                  type="date"
                  :value="buildWatchedDateValue()"
                  @change="handleNativeDateChange"
                />
              </div>
            </div>
            <div class="watched-editor-actions">
              <button type="button" class="cancel-btn" @click="closeActionPanels">取消</button>
              <button
                type="button"
                class="save-btn"
                :disabled="savingMovieId === movie.id"
                @click="confirmWatchedStatus(movie.id)"
              >
                {{ savingMovieId === movie.id ? '保存中' : '保存' }}
              </button>
            </div>
          </div>
        </div>

        <a class="movie-info-link" :href="`/movies/${movie.id}`" target="_blank">
          <div class="movie-meta">
            <div class="title-year">
              {{ movie.title }}<span v-if="movie.releaseDate"> ({{ movie.releaseDate.slice(0, 4) }})</span>
            </div>
            <div class="rating">
              {{ formatDisplayVoteAverage(movie) }}
            </div>
          </div>
        </a>
      </div>
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
  margin-bottom: 6px;
}

.filter-row:last-child {
  margin-bottom: 0;
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

.year-number-option {
  width: 52px;
  min-width: 52px;
  flex: 0 0 52px;
  height: 28px;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-variant-numeric: tabular-nums;
}

.older-year-select {
  height: 28px;
  padding: 0 28px 0 10px;
  border: 1px solid transparent;
  border-radius: 4px;
  background: #f3f4f6;
  color: #111827;
  font-size: 13px;
  outline: none;
  cursor: pointer;
}

.older-year-select:hover {
  color: #1677ff;
}

.older-year-select.active {
  border-color: #1677ff;
  background: #1677ff;
  color: #fff;
}

.search-options {
  flex: 0 1 360px;
}

.title-search-input {
  width: 100%;
  height: 28px;
  padding: 0 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #f8fafc;
  color: #172033;
  font-size: 13px;
  outline: none;
}

.title-search-input:focus {
  border-color: #9dc8ff;
  background: #fff;
  box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.1);
}

.rating-filter-inline {
  position: relative;
  flex-shrink: 0;
}

.rating-filter-trigger {
  height: 28px;
  border: 1px solid transparent;
  border-radius: 4px;
  padding: 0 10px;
  background: transparent;
  color: #111827;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  white-space: nowrap;
}

.rating-filter-trigger:hover,
.rating-filter-trigger.active {
  border-color: #b7d5ff;
  background: #eef5ff;
  color: #1677ff;
}

.rating-filter-trigger strong {
  color: inherit;
  font-weight: 700;
}

.mobile-status-rating-separator,
.mobile-status-rating-label,
.mobile-status-rating-arrow {
  display: none;
}

.movie-rating-options {
  overflow: visible;
}

.movie-rating-filter .rating-filter-trigger {
  min-width: 72px;
  justify-content: center;
  background: #f3f4f6;
}

.rating-filter-arrow {
  width: 7px;
  height: 7px;
  border-right: 2px solid currentColor;
  border-bottom: 2px solid currentColor;
  transform: translateY(-2px) rotate(45deg);
}

.rating-range-popover {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  z-index: 40;
  width: 330px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 14px 18px 13px;
  background: #fff;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.16);
}

.rating-range-popover::before {
  content: '';
  position: absolute;
  top: -7px;
  left: 22px;
  width: 12px;
  height: 12px;
  border-left: 1px solid #e5e7eb;
  border-top: 1px solid #e5e7eb;
  background: #fff;
  transform: rotate(45deg);
}

.rating-range-popover-title {
  margin-bottom: 8px;
  color: #111827;
  font-size: 14px;
  font-weight: 700;
}

.rating-range-panel {
  display: grid;
  grid-template-columns: 260px;
  align-items: start;
  row-gap: 8px;
  min-width: 0;
}

.rating-range-control {
  position: relative;
  width: 260px;
  height: 30px;
  cursor: pointer;
  --range-start: 0%;
  --range-end: 0%;
}

.rating-range-track,
.rating-range-active {
  position: absolute;
  left: 0;
  right: 0;
  top: 11px;
  height: 8px;
  border-radius: 999px;
}

.rating-range-track {
  background: #e5e7eb;
}

.rating-range-active {
  left: var(--range-start);
  right: var(--range-end);
  background: #1677ff;
}

.rating-range-tick {
  position: absolute;
  top: 50%;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #cbd5e1;
  transform: translate(-50%, -50%);
}

.rating-range-tick:nth-child(1) {
  left: 0;
}

.rating-range-tick:nth-child(2) {
  left: 25%;
}

.rating-range-tick:nth-child(3) {
  left: 50%;
}

.rating-range-tick:nth-child(4) {
  left: 75%;
}

.rating-range-tick:nth-child(5) {
  left: 100%;
}

.rating-range-handle {
  position: absolute;
  top: 15px;
  z-index: 3;
  width: 26px;
  height: 26px;
  border: 3px solid #1677ff;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 2px 8px rgba(22, 119, 255, 0.28);
  cursor: grab;
  transform: translate(-50%, -50%);
  touch-action: none;
}

.rating-range-handle:active,
.rating-range-handle.dragging {
  cursor: grabbing;
}

.rating-range-handle:focus-visible {
  outline: 2px solid #93c5fd;
  outline-offset: 2px;
}

.rating-range-handle-min {
  left: var(--range-start);
}

.rating-range-handle-max {
  left: calc(100% - var(--range-end));
}

.rating-range-labels {
  grid-column: 1;
  position: relative;
  width: 260px;
  height: 20px;
  margin-top: -1px;
  color: #6b7280;
  font-size: 12px;
}

.rating-range-labels span {
  position: absolute;
  top: 0;
  transform: translateX(-50%);
  white-space: nowrap;
}

.rating-range-labels span:nth-child(1) {
  left: 0;
  transform: translateX(0);
}

.rating-range-labels span:nth-child(2) {
  left: 25%;
}

.rating-range-labels span:nth-child(3) {
  left: 50%;
}

.rating-range-labels span:nth-child(4) {
  left: 75%;
}

.rating-range-labels span:nth-child(5) {
  left: 100%;
  transform: translateX(-100%);
}

.rating-range-labels .active {
  color: #111827;
  font-weight: 700;
}

.pagination button {
  height: 36px;
  padding: 0 16px;
  border: none;
  border-radius: 6px;
  background: #1677ff;
  color: white;
  cursor: pointer;
}

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
  color: inherit;
  display: block;
  background: transparent;
  border: none;
  box-shadow: none;
  padding: 0;
  min-width: 0;
  position: relative;
  transition: transform 0.2s ease;
}

.movie-card:hover {
  transform: translateY(-4px);
}

.movie-card.actionOpen {
  z-index: 20;
}

.poster-shell {
  position: relative;
}

.poster-link,
.movie-info-link {
  display: block;
  color: inherit;
  text-decoration: none;
}

.poster-wrapper {
  position: relative;
  width: 100%;
  aspect-ratio: 2 / 3;
  overflow: hidden;
  border-radius: 10px;
  background: #eaeaea;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.poster-status-overlay {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 2;
  padding: 36px 10px 10px;
  background: linear-gradient(
    to top,
    rgba(17, 24, 39, 0.96),
    rgba(17, 24, 39, 0.74) 56%,
    rgba(17, 24, 39, 0)
  );
  color: #fff;
  pointer-events: none;
}

.poster-status-line {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.poster-status-pill {
  flex-shrink: 0;
  min-width: 44px;
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  line-height: 1;
}

.status-want-to-watch .poster-status-pill {
  background: #1677ff;
}

.status-watched .poster-status-pill {
  background: #22c55e;
}

.status-not-interested .poster-status-pill {
  min-width: 58px;
  background: #6b7280;
}

.poster-status-date {
  min-width: 0;
  color: #f9fafb;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.2;
  white-space: nowrap;
}

.poster-status-stars {
  display: flex;
  gap: 1px;
  margin-top: 5px;
  color: #9ca3af;
  font-size: 16px;
  line-height: 1;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.45);
}

.poster-status-stars .filled {
  color: #fbbf24;
}

.poster-action-button {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 3;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 50%;
  background: rgba(17, 24, 39, 0.62);
  color: #fff;
  cursor: pointer;
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  padding: 0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.18);
  backdrop-filter: blur(6px);
  opacity: 0;
  transform: translateY(-2px);
  transition: opacity 0.18s ease, transform 0.18s ease, background 0.18s ease;
}

.movie-card:hover .poster-action-button,
.poster-action-button.active {
  opacity: 1;
  transform: translateY(0);
}

.poster-action-button:hover,
.poster-action-button.active {
  background: rgba(22, 119, 255, 0.9);
}

.poster-action-button span {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: currentColor;
}

.movie-action-menu,
.watched-editor-popover {
  position: absolute;
  top: 42px;
  right: 8px;
  z-index: 5;
  background: #fff;
  border: 1px solid rgba(229, 231, 235, 0.95);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.18);
}

.movie-action-menu {
  width: 128px;
  border-radius: 6px;
  padding: 6px;
}

.movie-action-menu button {
  width: 100%;
  min-height: 34px;
  border: none;
  border-radius: 5px;
  background: transparent;
  color: #111827;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 10px;
  font-size: 14px;
  text-align: left;
}

.movie-action-menu button:hover {
  background: #eef5ff;
  color: #1677ff;
}

.movie-action-menu button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.action-icon {
  width: 16px;
  display: inline-flex;
  justify-content: center;
  font-weight: 700;
}

.watched-editor-popover {
  width: 220px;
  border-radius: 8px;
  padding: 12px;
}

.watched-editor-title {
  margin-bottom: 10px;
  color: #111827;
  font-size: 14px;
  font-weight: 700;
}

.watched-editor-field {
  display: grid;
  gap: 6px;
  margin-bottom: 10px;
  color: #4b5563;
  font-size: 12px;
}

.watched-rating-picker {
  display: flex;
  gap: 2px;
}

.watched-rating-picker button {
  width: 26px;
  height: 26px;
  border: none;
  background: transparent;
  color: #d1d5db;
  cursor: pointer;
  font-size: 18px;
  line-height: 1;
  padding: 0;
}

.watched-rating-picker button.selected {
  color: #f5a623;
}

.date-input-shell {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 32px;
  width: 100%;
  height: 32px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  background: #fff;
  overflow: hidden;
}

.date-input-shell:focus-within {
  border-color: #1677ff;
}

.date-display-button,
.date-picker-button {
  border: none;
  background: transparent;
  color: #111827;
  cursor: pointer;
}

.date-display-button {
  min-width: 0;
  padding: 0 8px;
  text-align: left;
  font-size: 13px;
}

.date-picker-button {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #111827;
}

.date-picker-button::before {
  content: '';
  width: 13px;
  height: 13px;
  border: 2px solid currentColor;
  border-radius: 2px;
}

.date-picker-button::after {
  content: '';
  position: absolute;
  top: 9px;
  width: 13px;
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

.watched-editor-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 2px;
}

.watched-editor-actions button {
  height: 30px;
  border-radius: 5px;
  padding: 0 12px;
  cursor: pointer;
  font-size: 13px;
}

.cancel-btn {
  border: 1px solid #dcdfe6;
  background: #fff;
  color: #4b5563;
}

.save-btn {
  border: 1px solid #1677ff;
  background: #1677ff;
  color: #fff;
}

.save-btn:disabled {
  cursor: not-allowed;
  opacity: 0.65;
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

  .filter-row:has(.rating-filter-inline) .options {
    flex-wrap: wrap;
    overflow: visible;
  }

  .filter-row.user-status-filter-row .options {
    flex-wrap: nowrap;
    overflow-x: auto;
    overflow-y: visible;
  }

  .user-status-option {
    display: inline-flex;
    align-items: center;
    gap: 5px;
  }

  .watched-rating-option {
    padding-right: 12px;
  }

  .mobile-status-rating-separator,
  .mobile-status-rating-label,
  .mobile-status-rating-arrow {
    display: inline-flex;
  }

  .mobile-status-rating-label {
    color: inherit;
    font-weight: 700;
  }

  .mobile-status-rating-arrow {
    flex: 0 0 auto;
    margin-left: 1px;
  }

  .user-rating-filter {
    width: 0;
    height: 0;
    flex: 0 0 0;
  }

  .user-rating-filter > .rating-filter-trigger {
    display: none;
  }

  .movie-rating-filter-row {
    position: relative;
  }

  .movie-rating-filter {
    position: static;
  }

  .movie-rating-filter .rating-range-popover {
    position: absolute;
    top: calc(100% + 8px);
    left: 0;
    right: 0;
    width: auto;
    transform: none;
  }

  .rating-range-popover {
    position: fixed;
    top: 50%;
    left: 50%;
    right: auto;
    width: min(340px, calc(100vw - 32px));
    transform: translate(-50%, -50%);
    z-index: 80;
  }

  .rating-range-popover::before {
    display: none;
  }

  .rating-range-panel {
    grid-template-columns: minmax(0, 1fr);
    width: 100%;
  }

  .rating-range-control,
  .rating-range-labels {
    width: 100%;
  }

  .rating-range-labels {
    font-size: 11px;
  }

  .year-options {
    flex-wrap: nowrap;
  }

  .year-number-option {
    width: 60px;
    min-width: 60px;
    flex-basis: 60px;
  }

  .older-year-select {
    flex: 0 0 auto;
  }

  .search-filter-row .options {
    overflow: visible;
  }

  .search-options {
    flex: 1 1 auto;
    width: 100%;
  }

  .movie-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 16px 10px;
  }

  .movie-card:hover {
    transform: none;
  }

  .poster-wrapper {
    border-radius: 8px;
  }

  .poster-action-button {
    opacity: 1;
    transform: translateY(0);
  }

  .poster-status-overlay {
    padding: 30px 8px 8px;
  }

  .poster-status-line {
    gap: 6px;
  }

  .poster-status-pill {
    min-width: 40px;
    height: 22px;
    padding: 0 8px;
    font-size: 12px;
  }

  .status-not-interested .poster-status-pill {
    min-width: 54px;
  }

  .poster-status-date {
    font-size: 12px;
  }

  .poster-status-stars {
    margin-top: 4px;
    font-size: 14px;
  }

  .movie-action-menu {
    width: 120px;
  }

  .watched-editor-popover {
    position: fixed;
    top: 50%;
    left: 50%;
    right: auto;
    width: min(300px, calc(100vw - 32px));
    max-height: calc(100vh - 96px);
    overflow-y: auto;
    transform: translate(-50%, -50%);
    z-index: 80;
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
  .title-year,
  .rating {
    font-size: 13px;
  }
}
</style>
