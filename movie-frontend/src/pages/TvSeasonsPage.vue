<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { getGenres } from '../api/genre'
import { deleteTvSeason, getTvSeasons, type TvSeasonItem } from '../api/tvSeason'
import { saveUserTvSeason, type UserTvSeasonRequest } from '../api/userTvSeason'
import { getToken, isLoggedIn } from '../utils/auth'
import { request } from '../utils/request'

type UserStatusFilter = 'DEFAULT' | 'WANT_TO_WATCH' | 'WATCHED' | 'NOT_INTERESTED' | 'UNMARKED'
type UserStatus = 'WANT_TO_WATCH' | 'WATCHED' | 'NOT_INTERESTED'
type RatingRangeHandle = 'min' | 'max'
type RatingRangeDragState = {
  handle: RatingRangeHandle | null
  startX: number
  startMin: number
  startMax: number
}

const categories = ref<{ id: number | null; name: string }[]>([{ id: null, name: '全部' }])
const selectedCategory = ref<{ id: number | null; name: string }>({ id: null, name: '全部' })
const regions = [
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
  { code: 'TH', name: '泰国' },
  { code: 'OTHER', name: '其他' }
]
const selectedRegion = ref<{ code: string | null; name: string }>(regions[0])
const years = ref<number[]>([])
const selectedYear = ref<number | null>(null)
const keyword = ref('')
const ratingMin = ref(0)
const ratingMax = ref(10)
const ratingTickMarks = Array.from({ length: 11 }, (_, index) => index)
const ratingLabelMarks = [0, 5, 10]
const ratingRangeControlRef = ref<HTMLElement | null>(null)
const ratingRangeDragState = ref<RatingRangeDragState | null>(null)
const ratingRangePopoverOpen = ref(false)
const userRatingMin = ref(1)
const userRatingMax = ref(5)
const userRatingTickMarks = [1, 2, 3, 4, 5]
const userRatingRangeControlRef = ref<HTMLElement | null>(null)
const userRatingRangeDragState = ref<RatingRangeDragState | null>(null)
const userRatingRangePopoverOpen = ref(false)
const isUserLoggedIn = ref(isLoggedIn())
const isAdmin = ref(false)

const sortOptions = [
  { sortBy: 'CREATED_AT', direction: 'desc', name: '默认' },
  { sortBy: 'AIR_DATE', direction: 'desc', name: '首播日期' },
  { sortBy: 'VOTE_COUNT', direction: 'desc', name: '评分人数' },
  { sortBy: 'VOTE_AVERAGE', direction: 'desc', name: '评分' }
]
const selectedSort = ref(sortOptions[0])
const statusOptions: { value: UserStatusFilter | null; name: string }[] = [
  { value: 'DEFAULT', name: '默认' },
  { value: null, name: '全部' },
  { value: 'WANT_TO_WATCH', name: '想看' },
  { value: 'WATCHED', name: '看过' },
  { value: 'NOT_INTERESTED', name: '不想看' },
  { value: 'UNMARKED', name: '未标记' }
]
const selectedStatus = ref<{ value: UserStatusFilter | null; name: string }>(statusOptions[0])

const tvSeasons = ref<TvSeasonItem[]>([])
const loading = ref(false)
const errorMessage = ref('')
const page = ref(0)
const size = ref(20)
const totalPages = ref(0)
const totalElements = ref(0)
const savingId = ref<number | null>(null)
const activeActionSeasonId = ref<number | null>(null)
const watchedEditorSeasonId = ref<number | null>(null)
const watchedRating = ref<number | null>(null)
const hoveredWatchedRating = ref<number | null>(null)
const watchedYear = ref(new Date().getFullYear())
const watchedMonth = ref(new Date().getMonth() + 1)
const watchedDay = ref(new Date().getDate())
const jumpPage = ref('')

let searchTimer: number | undefined

const visiblePages = computed(() => {
  const total = totalPages.value
  const current = page.value
  const items: (number | string)[] = []
  if (total <= 7) {
    for (let i = 0; i < total; i++) items.push(i)
    return items
  }
  if (current <= 3) return [0, 1, 2, 3, 4, '...', total - 1]
  if (current >= total - 4) return [0, '...', total - 5, total - 4, total - 3, total - 2, total - 1]
  return [0, '...', current - 1, current, current + 1, '...', total - 1]
})
const recentYears = computed(() => years.value.slice(0, 10))
const olderYears = computed(() => years.value.slice(10))
const isOlderYearSelected = computed(() =>
  selectedYear.value !== null && !recentYears.value.includes(selectedYear.value)
)
const hasRatingRangeFilter = computed(() => ratingMin.value !== 0 || ratingMax.value !== 10)
const ratingRangeLabel = computed(() => {
  if (!hasRatingRangeFilter.value) {
    return '全部'
  }
  return ratingMin.value === ratingMax.value
    ? `${ratingMin.value}分`
    : `${ratingMin.value}-${ratingMax.value}分`
})
const ratingRangeStart = computed(() => `${(ratingMin.value / 10) * 100}%`)
const ratingRangeEnd = computed(() => `${100 - (ratingMax.value / 10) * 100}%`)
const showUserRatingRange = computed(() =>
  isUserLoggedIn.value && selectedStatus.value.value === 'WATCHED'
)
const hasUserRatingRangeFilter = computed(() =>
  showUserRatingRange.value && (userRatingMin.value !== 1 || userRatingMax.value !== 5)
)
const userRatingRangeLabel = computed(() => {
  if (!hasUserRatingRangeFilter.value) return '全部'
  return userRatingMin.value === userRatingMax.value
    ? `${userRatingMin.value}星`
    : `${userRatingMin.value}-${userRatingMax.value}星`
})
const userRatingRangeStart = computed(() => `${((userRatingMin.value - 1) / 4) * 100}%`)
const userRatingRangeEnd = computed(() => `${100 - ((userRatingMax.value - 1) / 4) * 100}%`)
const watchedDateDisplay = computed(() => buildWatchedDateValue().replaceAll('-', '/'))

function initYears() {
  const currentYear = new Date().getFullYear()
  years.value = []
  for (let year = currentYear; year >= 1950; year--) years.value.push(year)
}

async function loadGenres() {
  const data = await getGenres('TV')
  categories.value = [{ id: null, name: '全部' }, ...data.map(item => ({ id: item.id, name: item.name }))]
}

async function loadTvSeasons() {
  loading.value = true
  errorMessage.value = ''
  try {
    const data = await getTvSeasons({
      title: keyword.value.trim() || undefined,
      genreId: selectedCategory.value.id ?? undefined,
      year: selectedYear.value ?? undefined,
      regionCode: selectedRegion.value.code ?? undefined,
      userTvSeasonStatus: isUserLoggedIn.value ? selectedStatus.value.value ?? undefined : undefined,
      userRatingMin: hasUserRatingRangeFilter.value ? userRatingMin.value : undefined,
      userRatingMax: hasUserRatingRangeFilter.value ? userRatingMax.value : undefined,
      tvSeasonRatingMin: hasRatingRangeFilter.value ? ratingMin.value : undefined,
      tvSeasonRatingMax: hasRatingRangeFilter.value ? ratingMax.value : undefined,
      page: page.value,
      size: size.value,
      sortBy: selectedSort.value.sortBy,
      direction: selectedSort.value.direction
    })
    tvSeasons.value = data.content
    totalPages.value = data.totalPages
    totalElements.value = data.totalElements
  } catch (error) {
    console.error(error)
    errorMessage.value = '加载剧集失败'
  } finally {
    loading.value = false
  }
}

async function loadAdminStatus() {
  isAdmin.value = false

  if (!isUserLoggedIn.value) {
    return
  }

  try {
    const response = await request('/users/me')
    if (!response.ok) {
      return
    }

    const data = await response.json()
    isAdmin.value = Boolean(data.isAdmin)
  } catch (error) {
    console.error(error)
    isAdmin.value = false
  }
}

function resetPageAndLoad() {
  page.value = 0
  void loadTvSeasons()
}

function handleYearClick(year: number | null) {
  selectedYear.value = year
  resetPageAndLoad()
}

function handleOlderYearChange(event: Event) {
  const value = (event.target as HTMLSelectElement).value
  if (!value) {
    return
  }

  selectedYear.value = Number(value)
  resetPageAndLoad()
}

function handleUserStatusClick(status: { value: UserStatusFilter | null; name: string }) {
  selectedStatus.value = status
  userRatingRangePopoverOpen.value = false
  if (status.value !== 'WATCHED') resetUserRatingRange(false)
  resetPageAndLoad()
}

function handleUserStatusOptionClick(
  status: { value: UserStatusFilter | null; name: string },
  event: MouseEvent
) {
  if (showUserRatingRange.value && status.value === 'WATCHED') {
    event.stopPropagation()
    toggleUserRatingRangePopover()
    return
  }

  handleUserStatusClick(status)
}

function resetUserRatingRange(shouldLoad = true) {
  userRatingMin.value = 1
  userRatingMax.value = 5
  if (shouldLoad && showUserRatingRange.value) resetPageAndLoad()
}

function clampUserRating(value: number) {
  return Math.min(5, Math.max(1, value))
}

function getUserRatingPointPosition(star: number) {
  return `${((star - 1) / 4) * 100}%`
}

function getUserRatingLabelStyle(star: number) {
  let transform = 'translateX(-50%)'
  if (star === 1) transform = 'translateX(0)'
  if (star === 5) transform = 'translateX(-100%)'

  return {
    left: getUserRatingPointPosition(star),
    transform
  }
}

function setUserRatingRangeControlRef(element: unknown) {
  userRatingRangeControlRef.value = element instanceof HTMLElement ? element : null
}

function getUserRatingFromPointer(event: PointerEvent) {
  if (!userRatingRangeControlRef.value) return userRatingMin.value

  const rect = userRatingRangeControlRef.value.getBoundingClientRect()
  const ratio = Math.min(1, Math.max(0, (event.clientX - rect.left) / rect.width))
  return clampUserRating(Math.round(ratio * 4) + 1)
}

function chooseUserRatingRangeHandle(value: number): RatingRangeHandle | null {
  if (userRatingMin.value === userRatingMax.value) {
    if (value < userRatingMin.value) return 'min'
    if (value > userRatingMax.value) return 'max'
    return null
  }

  const minDistance = Math.abs(value - userRatingMin.value)
  const maxDistance = Math.abs(value - userRatingMax.value)
  if (minDistance < maxDistance) return 'min'
  if (maxDistance < minDistance) return 'max'
  return value < userRatingMin.value ? 'min' : 'max'
}

function updateUserRatingRangeHandle(handle: RatingRangeHandle, value: number) {
  const rating = clampUserRating(value)
  if (handle === 'min') {
    userRatingMin.value = Math.min(rating, userRatingMax.value)
  } else {
    userRatingMax.value = Math.max(rating, userRatingMin.value)
  }
}

function startUserRatingRangeDrag(event: PointerEvent, requestedHandle?: RatingRangeHandle) {
  event.preventDefault()

  const pointedRating = getUserRatingFromPointer(event)
  const handlesOverlap = userRatingMin.value === userRatingMax.value
  const handle = requestedHandle && !handlesOverlap
    ? requestedHandle
    : chooseUserRatingRangeHandle(pointedRating)

  userRatingRangeDragState.value = {
    handle,
    startX: event.clientX,
    startMin: userRatingMin.value,
    startMax: userRatingMax.value
  }

  if (handle) updateUserRatingRangeHandle(handle, pointedRating)

  window.addEventListener('pointermove', handleUserRatingRangePointerMove)
  window.addEventListener('pointerup', finishUserRatingRangeDrag)
}

function handleUserRatingRangePointerMove(event: PointerEvent) {
  const dragState = userRatingRangeDragState.value
  if (!dragState) return

  let handle = dragState.handle
  if (!handle) {
    const movement = event.clientX - dragState.startX
    if (Math.abs(movement) < 2) return
    handle = movement < 0 ? 'min' : 'max'
    dragState.handle = handle
  }

  updateUserRatingRangeHandle(handle, getUserRatingFromPointer(event))
}

function finishUserRatingRangeDrag() {
  const dragState = userRatingRangeDragState.value
  userRatingRangeDragState.value = null
  window.removeEventListener('pointermove', handleUserRatingRangePointerMove)
  window.removeEventListener('pointerup', finishUserRatingRangeDrag)

  if (
    dragState &&
    showUserRatingRange.value &&
    (dragState.startMin !== userRatingMin.value || dragState.startMax !== userRatingMax.value)
  ) {
    resetPageAndLoad()
  }
}

function toggleUserRatingRangePopover() {
  userRatingRangePopoverOpen.value = !userRatingRangePopoverOpen.value
  if (userRatingRangePopoverOpen.value) ratingRangePopoverOpen.value = false
}

function handleUserRatingRangeKeydown(handle: RatingRangeHandle, event: KeyboardEvent) {
  if (event.key !== 'ArrowLeft' && event.key !== 'ArrowDown' && event.key !== 'ArrowRight' && event.key !== 'ArrowUp') {
    return
  }

  event.preventDefault()
  const delta = event.key === 'ArrowLeft' || event.key === 'ArrowDown' ? -1 : 1
  const nextValue = handle === 'min' ? userRatingMin.value + delta : userRatingMax.value + delta
  const previousMin = userRatingMin.value
  const previousMax = userRatingMax.value

  updateUserRatingRangeHandle(handle, nextValue)
  if (previousMin !== userRatingMin.value || previousMax !== userRatingMax.value) resetPageAndLoad()
}

function clampRating(value: number) {
  return Math.min(10, Math.max(0, value))
}

function getRatingPointPosition(score: number) {
  return `${(score / 10) * 100}%`
}

function getRatingLabelStyle(score: number) {
  let transform = 'translateX(-50%)'
  if (score === 0) {
    transform = 'translateX(0)'
  } else if (score === 10) {
    transform = 'translateX(-100%)'
  }

  return {
    left: getRatingPointPosition(score),
    transform
  }
}

function setRatingRangeControlRef(element: unknown) {
  ratingRangeControlRef.value = element instanceof HTMLElement ? element : null
}

function getRatingFromPointer(event: PointerEvent) {
  if (!ratingRangeControlRef.value) {
    return ratingMin.value
  }

  const rect = ratingRangeControlRef.value.getBoundingClientRect()
  const ratio = Math.min(1, Math.max(0, (event.clientX - rect.left) / rect.width))
  return clampRating(Math.round(ratio * 10))
}

function chooseRatingRangeHandle(value: number): RatingRangeHandle | null {
  if (ratingMin.value === ratingMax.value) {
    if (value < ratingMin.value) return 'min'
    if (value > ratingMax.value) return 'max'
    return null
  }

  const minDistance = Math.abs(value - ratingMin.value)
  const maxDistance = Math.abs(value - ratingMax.value)
  if (minDistance < maxDistance) return 'min'
  if (maxDistance < minDistance) return 'max'
  return value < ratingMin.value ? 'min' : 'max'
}

function updateRatingRangeHandle(handle: RatingRangeHandle, value: number) {
  const rating = clampRating(value)
  if (handle === 'min') {
    ratingMin.value = Math.min(rating, ratingMax.value)
  } else {
    ratingMax.value = Math.max(rating, ratingMin.value)
  }
}

function startRatingRangeDrag(event: PointerEvent, requestedHandle?: RatingRangeHandle) {
  event.preventDefault()

  const pointedRating = getRatingFromPointer(event)
  const handlesOverlap = ratingMin.value === ratingMax.value
  const handle = requestedHandle && !handlesOverlap
    ? requestedHandle
    : chooseRatingRangeHandle(pointedRating)

  ratingRangeDragState.value = {
    handle,
    startX: event.clientX,
    startMin: ratingMin.value,
    startMax: ratingMax.value
  }

  if (handle) {
    updateRatingRangeHandle(handle, pointedRating)
  }

  window.addEventListener('pointermove', handleRatingRangePointerMove)
  window.addEventListener('pointerup', finishRatingRangeDrag)
}

function handleRatingRangePointerMove(event: PointerEvent) {
  const dragState = ratingRangeDragState.value
  if (!dragState) return

  let handle = dragState.handle
  if (!handle) {
    const movement = event.clientX - dragState.startX
    if (Math.abs(movement) < 2) return
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

  if (dragState && (dragState.startMin !== ratingMin.value || dragState.startMax !== ratingMax.value)) {
    resetPageAndLoad()
  }
}

function toggleRatingRangePopover() {
  ratingRangePopoverOpen.value = !ratingRangePopoverOpen.value
  if (ratingRangePopoverOpen.value) userRatingRangePopoverOpen.value = false
}

function handleRatingRangeKeydown(handle: RatingRangeHandle, event: KeyboardEvent) {
  if (event.key !== 'ArrowLeft' && event.key !== 'ArrowDown' && event.key !== 'ArrowRight' && event.key !== 'ArrowUp') {
    return
  }

  event.preventDefault()
  const delta = event.key === 'ArrowLeft' || event.key === 'ArrowDown' ? -1 : 1
  const nextValue = handle === 'min' ? ratingMin.value + delta : ratingMax.value + delta
  const previousMin = ratingMin.value
  const previousMax = ratingMax.value

  updateRatingRangeHandle(handle, nextValue)
  if (previousMin !== ratingMin.value || previousMax !== ratingMax.value) {
    resetPageAndLoad()
  }
}

function getPosterUrl(path: string | null) {
  return path ? `https://image.tmdb.org/t/p/w500${path}` : ''
}

function getDisplayVoteAverage(item: TvSeasonItem) {
  return item.imdbVoteAverage ?? item.voteAverage
}

function getDisplayVoteCount(item: TvSeasonItem) {
  return item.imdbVoteCount ?? item.voteCount
}

function getDisplayTitle(item: TvSeasonItem) {
  return item.seasonCount === 1 ? item.showName : item.title
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

function getStatusText(status: UserStatus | null) {
  if (status === 'WANT_TO_WATCH') return '想看'
  if (status === 'WATCHED') return '看过'
  if (status === 'NOT_INTERESTED') return '不想看'
  return ''
}

function getStatusClass(status: UserStatus | null) {
  return status ? `status-${status.toLowerCase().replaceAll('_', '-')}` : ''
}

function getStatusDate(item: TvSeasonItem) {
  if (item.userTvSeasonStatus === 'WATCHED') {
    return item.userWatchedDate ?? item.userTvSeasonUpdatedAt
  }
  return item.userTvSeasonUpdatedAt
}

function getRatingValue(rating: number | null) {
  if (rating === null) return 0
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

  if (typeof picker.showPicker === 'function') {
    picker.showPicker()
  } else {
    picker.click()
  }
}

function handleNativeDateChange(event: Event) {
  const value = (event.target as HTMLInputElement).value
  if (value) setWatchedDateParts(value)
}

function closeActionPanels() {
  activeActionSeasonId.value = null
  watchedEditorSeasonId.value = null
  hoveredWatchedRating.value = null
  ratingRangePopoverOpen.value = false
  userRatingRangePopoverOpen.value = false
}

function toggleActionMenu(seasonId: number) {
  watchedEditorSeasonId.value = null
  activeActionSeasonId.value = activeActionSeasonId.value === seasonId ? null : seasonId
}

function openWatchedEditor(item: TvSeasonItem) {
  activeActionSeasonId.value = null
  watchedEditorSeasonId.value = item.id
  watchedRating.value = item.userRating ?? null
  hoveredWatchedRating.value = null
  setWatchedDateParts(item.userWatchedDate)
}

async function saveSeasonStatus(seasonId: number, data: UserTvSeasonRequest) {
  const token = getToken()
  if (!token) {
    window.location.href = `/login?redirect=${encodeURIComponent('/tv-seasons')}`
    return
  }

  if (savingId.value !== null) return
  savingId.value = seasonId

  try {
    await saveUserTvSeason(seasonId, data, token)
    closeActionPanels()
    await loadTvSeasons()
  } catch (error) {
    console.error(error)
    window.alert('保存剧集状态失败，请稍后再试')
  } finally {
    savingId.value = null
  }
}

async function markSeason(item: TvSeasonItem, status: UserStatus) {
  if (status === 'WATCHED') {
    openWatchedEditor(item)
    return
  }

  await saveSeasonStatus(item.id, { status })
}

async function handleDeleteSeason(item: TvSeasonItem) {
  if (!isAdmin.value || savingId.value !== null) {
    return
  }

  savingId.value = item.id

  try {
    await deleteTvSeason(item.id)
    tvSeasons.value = tvSeasons.value.filter(season => season.id !== item.id)
    totalElements.value = Math.max(0, totalElements.value - 1)
    totalPages.value = Math.ceil(totalElements.value / size.value)
    closeActionPanels()

    if (tvSeasons.value.length === 0 && page.value > 0) {
      page.value -= 1
      await loadTvSeasons()
    }
  } catch (error) {
    console.error(error)
    window.alert('删除剧集失败，请稍后再试')
  } finally {
    savingId.value = null
  }
}

async function confirmWatchedStatus(seasonId: number) {
  await saveSeasonStatus(seasonId, {
    status: 'WATCHED',
    rating: watchedRating.value,
    watchedDate: buildWatchedDateValue()
  })
}

function goToPage(target: number) {
  if (target < 0 || target >= totalPages.value || target === page.value) return
  page.value = target
  void loadTvSeasons()
}

function handleJumpPage() {
  const target = Number(jumpPage.value)
  if (!Number.isInteger(target) || target < 1 || target > totalPages.value) return
  goToPage(target - 1)
  jumpPage.value = ''
}

watch(keyword, () => {
  if (searchTimer) window.clearTimeout(searchTimer)
  searchTimer = window.setTimeout(resetPageAndLoad, 350)
})

watch([watchedYear, watchedMonth], () => {
  const maxDay = getDaysInMonth(watchedYear.value, watchedMonth.value)
  if (watchedDay.value > maxDay) watchedDay.value = maxDay
})

onMounted(async () => {
  document.addEventListener('click', closeActionPanels)
  isUserLoggedIn.value = isLoggedIn()
  await loadAdminStatus()
  initYears()
  await loadGenres()
  await loadTvSeasons()
})

onBeforeUnmount(() => {
  document.removeEventListener('click', closeActionPanels)
  window.removeEventListener('pointermove', handleRatingRangePointerMove)
  window.removeEventListener('pointerup', finishRatingRangeDrag)
  window.removeEventListener('pointermove', handleUserRatingRangePointerMove)
  window.removeEventListener('pointerup', finishUserRatingRangeDrag)
})
</script>

<template>
  <div class="tv-page">
    <section class="filter-panel">
      <div class="filter-row">
        <span class="label">类型：</span>
        <div class="options">
          <button
            v-for="category in categories"
            :key="category.id ?? 'all'"
            class="option"
            :class="{ active: selectedCategory.id === category.id }"
            @click="selectedCategory = category; resetPageAndLoad()"
          >
            {{ category.name }}
          </button>
        </div>
      </div>

      <div class="filter-row">
        <span class="label">地区：</span>
        <div class="options">
          <button
            v-for="region in regions"
            :key="region.code ?? 'all'"
            class="option"
            :class="{ active: selectedRegion.code === region.code }"
            @click="selectedRegion = region; resetPageAndLoad()"
          >
            {{ region.name }}
          </button>
        </div>
      </div>

      <div class="filter-row">
        <span class="label">年份：</span>
        <div class="options year-options">
          <button class="option" :class="{ active: selectedYear === null }" @click="handleYearClick(null)">全部</button>
          <!-- <span
            class="option"
            :class="{ active: selectedYear === null }"
            @click="handleYearClick(null)"
          >
            全部
          </span> -->
          <button
            v-for="year in recentYears"
            :key="year"
            class="option year-number-option"
            :class="{ active: selectedYear === year }"
            @click="handleYearClick(year)"
          >
            {{ year }}
          </button>
          <select
            class="older-year-select"
            :class="{ active: isOlderYearSelected }"
            :value="isOlderYearSelected ? selectedYear : ''"
            @change="handleOlderYearChange"
          >
            <option value="">更早</option>
            <option v-for="year in olderYears" :key="year" :value="year">{{ year }}</option>
          </select>
        </div>
      </div>

      <div v-if="isUserLoggedIn" class="filter-row user-status-filter-row">
        <span class="label">状态：</span>
        <div class="options">
          <template
            v-for="status in statusOptions"
            :key="status.value ?? 'all'"
          >
            <span
              class="option user-status-option"
              :class="{
                active: selectedStatus.value === status.value,
                'watched-rating-option': showUserRatingRange && status.value === 'WATCHED'
              }"
              @click="handleUserStatusOptionClick(status, $event)"
            >
              <span>{{ status.name }}</span>
              <template v-if="showUserRatingRange && status.value === 'WATCHED'">
                <span class="mobile-status-rating-separator">·</span>
                <strong class="mobile-status-rating-label">{{ userRatingRangeLabel }}</strong>
                <span class="rating-filter-arrow mobile-status-rating-arrow"></span>
              </template>
            </span>

            <div
              v-if="showUserRatingRange && status.value === 'WATCHED'"
              class="rating-filter-inline user-rating-filter"
              @click.stop
            >
              <button
                type="button"
                class="rating-filter-trigger"
                :class="{ active: userRatingRangePopoverOpen || hasUserRatingRangeFilter }"
                @click="toggleUserRatingRangePopover"
              >
                <span>我的评分</span>
                <strong>{{ userRatingRangeLabel }}</strong>
                <span class="rating-filter-arrow"></span>
              </button>

              <div v-if="userRatingRangePopoverOpen" class="rating-range-popover">
                <div class="rating-range-popover-title">我的评分</div>
                <div class="rating-range-panel">
                  <div
                    :ref="setUserRatingRangeControlRef"
                    class="rating-range-control"
                    :style="{
                      '--range-start': userRatingRangeStart,
                      '--range-end': userRatingRangeEnd
                    }"
                    @pointerdown="startUserRatingRangeDrag"
                  >
                    <div class="rating-range-track">
                      <span
                        v-for="star in userRatingTickMarks"
                        :key="star"
                        class="rating-range-tick"
                        :style="{ left: getUserRatingPointPosition(star) }"
                      ></span>
                    </div>
                    <div class="rating-range-active"></div>
                    <button
                      type="button"
                      class="rating-range-handle rating-range-handle-min"
                      :class="{ dragging: userRatingRangeDragState?.handle === 'min' }"
                      aria-label="最低我的评分"
                      :aria-valuenow="userRatingMin"
                      aria-valuemin="1"
                      aria-valuemax="5"
                      @pointerdown.stop="startUserRatingRangeDrag($event, 'min')"
                      @keydown="handleUserRatingRangeKeydown('min', $event)"
                    ></button>
                    <button
                      type="button"
                      class="rating-range-handle rating-range-handle-max"
                      :class="{ dragging: userRatingRangeDragState?.handle === 'max' }"
                      aria-label="最高我的评分"
                      :aria-valuenow="userRatingMax"
                      aria-valuemin="1"
                      aria-valuemax="5"
                      @pointerdown.stop="startUserRatingRangeDrag($event, 'max')"
                      @keydown="handleUserRatingRangeKeydown('max', $event)"
                    ></button>
                  </div>
                  <div class="rating-range-labels">
                    <span
                      v-for="star in userRatingTickMarks"
                      :key="star"
                      :style="getUserRatingLabelStyle(star)"
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
          <button
            v-for="sort in sortOptions"
            :key="sort.sortBy"
            class="option"
            :class="{ active: selectedSort.sortBy === sort.sortBy }"
            @click="selectedSort = sort; resetPageAndLoad()"
          >
            {{ sort.name }}
          </button>
        </div>
      </div>

      <div class="filter-row movie-rating-filter-row">
        <span class="label">评分：</span>
        <div class="options movie-rating-options">
          <div class="rating-filter-inline movie-rating-filter" @click.stop>
            <button
              type="button"
              class="rating-filter-trigger"
              :class="{ active: ratingRangePopoverOpen || hasRatingRangeFilter }"
              @click="toggleRatingRangePopover"
            >
              <strong>{{ ratingRangeLabel }}</strong>
              <span class="rating-filter-arrow"></span>
            </button>

            <div v-if="ratingRangePopoverOpen" class="rating-range-popover">
              <div class="rating-range-popover-title">评分</div>
              <div class="rating-range-panel">
                <div
                  :ref="setRatingRangeControlRef"
                  class="rating-range-control"
                  :style="{
                    '--range-start': ratingRangeStart,
                    '--range-end': ratingRangeEnd
                  }"
                  @pointerdown="startRatingRangeDrag"
                >
                  <div class="rating-range-track">
                    <span
                      v-for="score in ratingTickMarks"
                      :key="score"
                      class="rating-range-tick"
                      :style="{ left: getRatingPointPosition(score) }"
                    ></span>
                  </div>
                  <div class="rating-range-active"></div>
                  <button
                    type="button"
                    class="rating-range-handle rating-range-handle-min"
                    :class="{ dragging: ratingRangeDragState?.handle === 'min' }"
                    aria-label="最低评分"
                    :aria-valuenow="ratingMin"
                    aria-valuemin="0"
                    aria-valuemax="10"
                    @pointerdown.stop="startRatingRangeDrag($event, 'min')"
                    @keydown="handleRatingRangeKeydown('min', $event)"
                  ></button>
                  <button
                    type="button"
                    class="rating-range-handle rating-range-handle-max"
                    :class="{ dragging: ratingRangeDragState?.handle === 'max' }"
                    aria-label="最高评分"
                    :aria-valuenow="ratingMax"
                    aria-valuemin="0"
                    aria-valuemax="10"
                    @pointerdown.stop="startRatingRangeDrag($event, 'max')"
                    @keydown="handleRatingRangeKeydown('max', $event)"
                  ></button>
                </div>
                <div class="rating-range-labels">
                  <span
                    v-for="score in ratingLabelMarks"
                    :key="score"
                    :style="getRatingLabelStyle(score)"
                    :class="{ active: score >= ratingMin && score <= ratingMax }"
                  >
                    {{ score }}分
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="filter-row">
        <span class="label">搜索：</span>
        <div class="options search-options">
          <input v-model="keyword" class="search-input" type="search" placeholder="搜索剧名或季标题" />
        </div>
      </div>
    </section>

    <div class="summary">共 {{ totalElements }} 个剧集季</div>
    <div v-if="loading" class="status">正在加载...</div>
    <div v-else-if="errorMessage" class="status error">{{ errorMessage }}</div>

    <div v-else class="season-grid">
      <article
        v-for="item in tvSeasons"
        :key="item.id"
        class="season-card"
        :class="{ actionOpen: activeActionSeasonId === item.id || watchedEditorSeasonId === item.id }"
      >
        <div class="poster-shell">
          <a class="poster-link" :href="`/tv-seasons/${item.id}`" target="_blank" rel="noopener noreferrer">
            <div class="poster-wrap">
              <img v-if="item.posterPath" :src="getPosterUrl(item.posterPath)" :alt="getDisplayTitle(item)" />
              <div v-else class="poster-placeholder">{{ getDisplayTitle(item).slice(0, 1) }}</div>
              <div class="episode-badge">{{ item.episodeCount ?? '-' }} 集</div>
              <div
                v-if="item.userTvSeasonStatus"
                class="poster-status-overlay"
                :class="getStatusClass(item.userTvSeasonStatus)"
              >
                <div v-if="item.userTvSeasonStatus === 'WATCHED'" class="poster-status-stars">
                  <span
                    v-for="star in 5"
                    :key="star"
                    :class="{ filled: star <= getRatingValue(item.userRating) }"
                  >
                    ★
                  </span>
                </div>
                <div class="poster-status-line">
                  <span class="poster-status-pill">
                    {{ getStatusText(item.userTvSeasonStatus) }}
                  </span>
                  <span v-if="getStatusDate(item)" class="poster-status-date">
                    {{ formatDate(getStatusDate(item)) }}
                  </span>
                </div>
              </div>
            </div>
          </a>

          <button
            v-if="isUserLoggedIn"
            type="button"
            class="poster-action-button"
            :class="{ active: activeActionSeasonId === item.id || watchedEditorSeasonId === item.id }"
            aria-label="标记剧集"
            @click.stop="toggleActionMenu(item.id)"
          >
            <span></span>
            <span></span>
            <span></span>
          </button>

          <div
            v-if="activeActionSeasonId === item.id"
            class="season-action-menu"
            @click.stop
          >
            <button type="button" :disabled="savingId === item.id" @click="markSeason(item, 'WANT_TO_WATCH')">
              <span class="action-icon">＋</span>
              想看
            </button>
            <button type="button" :disabled="savingId === item.id" @click="markSeason(item, 'WATCHED')">
              <span class="action-icon">✓</span>
              看过
            </button>
            <button type="button" :disabled="savingId === item.id" @click="markSeason(item, 'NOT_INTERESTED')">
              <span class="action-icon">－</span>
              不想看
            </button>
            <button
              v-if="isAdmin"
              type="button"
              class="danger-action"
              :disabled="savingId === item.id"
              @click="handleDeleteSeason(item)"
            >
              <span class="action-icon">×</span>
              删除
            </button>
          </div>

          <div
            v-if="watchedEditorSeasonId === item.id"
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
                :disabled="savingId === item.id"
                @click="confirmWatchedStatus(item.id)"
              >
                {{ savingId === item.id ? '保存中' : '保存' }}
              </button>
            </div>
          </div>
        </div>

        <a class="season-info-link" :href="`/tv-seasons/${item.id}`" target="_blank" rel="noopener noreferrer">
          <div class="season-meta">
            <div class="season-title">
              {{ getDisplayTitle(item) }}<span v-if="item.year"> ({{ item.year }})</span>
            </div>
            <div class="season-rating">{{ formatScore(getDisplayVoteAverage(item)) }}</div>
          </div>
        </a>

        <div class="card-body">
          <a class="title-link" :href="`/tv-seasons/${item.id}`" target="_blank" rel="noopener noreferrer">
            <h2>{{ item.title }}</h2>
          </a>
          <p>{{ item.originalTitle || item.originalShowName || '原名未知' }}</p>
          <div class="meta-line">
            <span>{{ item.year || '年份未知' }}</span>
            <span>{{ item.episodeCount ?? '未知' }} 集</span>
          </div>
          <div class="score-line">
            <strong>{{ formatScore(getDisplayVoteAverage(item)) }}</strong>
            <span>{{ formatCount(getDisplayVoteCount(item)) }} 人</span>
          </div>
          <div class="card-actions">
            <button :disabled="savingId === item.id" @click="markSeason(item, 'WANT_TO_WATCH')">想看</button>
            <button :disabled="savingId === item.id" @click="markSeason(item, 'WATCHED')">看过</button>
            <button :disabled="savingId === item.id" @click="markSeason(item, 'NOT_INTERESTED')">不想看</button>
          </div>
          <div v-if="item.userWatchedDate || item.userTvSeasonUpdatedAt" class="record-date">
            {{ formatDate(item.userWatchedDate || item.userTvSeasonUpdatedAt) }}
          </div>
        </div>
      </article>
    </div>

    <div class="pagination">
      <button :disabled="page === 0" @click="goToPage(page - 1)">上一页</button>
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
      <button :disabled="page >= totalPages - 1" @click="goToPage(page + 1)">下一页</button>
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
.tv-page {
  display: grid;
  gap: 16px;
}

.filter-panel {
  padding: 16px;
  border-radius: 8px;
  background: #fff;
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
  flex-shrink: 0;
  line-height: 28px;
  font-size: 13px;
  white-space: nowrap;
}

.options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
}

.option {
  padding: 4px 10px;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: #111827;
  cursor: pointer;
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

.select-control,
.number-input,
.search-input {
  height: 30px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #f8fafc;
  color: #111827;
  outline: none;
  font-size: 13px;
}

.select-control {
  padding: 0 10px;
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

.number-input {
  width: 74px;
  padding: 0 8px;
}

.search-options {
  flex: 0 1 360px;
}

.search-input {
  width: 100%;
  height: 28px;
  padding: 0 12px;
  color: #172033;
}

.search-input:focus {
  border-color: #9dc8ff;
  background: #fff;
  box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.1);
}

.compact-options {
  align-items: center;
}

.compact-options > .rating-filter + .number-input,
.compact-options > .rating-filter + .number-input + .range-separator,
.compact-options > .rating-filter + .number-input + .range-separator + .number-input {
  display: none;
}

.rating-filter-inline {
  position: relative;
  flex-shrink: 0;
}

.rating-filter-trigger {
  height: 28px;
  min-width: 72px;
  border: 1px solid transparent;
  border-radius: 4px;
  padding: 0 10px;
  background: transparent;
  color: #111827;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
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

.rating-range-labels .active {
  color: #111827;
  font-weight: 700;
}

.range-separator,
.sub-label,
.summary,
.record-date {
  color: #6b7280;
  font-size: 13px;
}

.status {
  color: #6b7280;
  padding: 24px 0;
}

.status.error {
  color: #d93025;
}

.season-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 18px 14px;
}

.season-card {
  min-width: 0;
  position: relative;
  transition: transform 0.2s ease;
}

.season-card:hover {
  transform: translateY(-4px);
}

.season-card.actionOpen {
  z-index: 20;
}

.poster-shell {
  position: relative;
}

.poster-link,
.title-link,
.season-info-link {
  display: block;
  color: inherit;
  text-decoration: none;
}

.poster-wrap {
  position: relative;
  aspect-ratio: 2 / 3;
  overflow: hidden;
  border-radius: 10px;
  background: #e5e7eb;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.poster-wrap img,
.poster-placeholder {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6b7280;
  font-size: 30px;
  font-weight: 800;
}

.episode-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  z-index: 2;
  min-height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  background: rgba(17, 24, 39, 0.72);
  color: #fff;
  font-size: 12px;
  font-weight: 800;
  backdrop-filter: blur(6px);
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
  margin-bottom: 5px;
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

.season-card:hover .poster-action-button,
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

.season-action-menu,
.watched-editor-popover {
  position: absolute;
  top: 42px;
  right: 8px;
  z-index: 5;
  border: 1px solid rgba(229, 231, 235, 0.95);
  background: #fff;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.18);
}

.season-action-menu {
  width: 128px;
  border-radius: 6px;
  padding: 6px;
}

.season-action-menu button {
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

.season-action-menu button:hover {
  background: #eef5ff;
  color: #1677ff;
}

.season-action-menu button.danger-action {
  color: #dc2626;
}

.season-action-menu button.danger-action:hover {
  background: #fef2f2;
  color: #b91c1c;
}

.season-action-menu button:disabled {
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

.season-meta {
  margin-top: 10px;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  min-width: 0;
}

.season-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  color: #111;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.season-rating {
  flex-shrink: 0;
  color: #f5a623;
  font-size: 17px;
  font-weight: 700;
}

.card-body {
  display: none;
}

.card-body h2 {
  margin: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  color: #111827;
  font-size: 16px;
  line-height: 1.35;
}

.card-body p {
  margin: 4px 0 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  color: #6b7280;
  font-size: 12px;
}

.meta-line,
.score-line {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-top: 6px;
  color: #6b7280;
  font-size: 13px;
}

.score-line strong {
  color: #f59e0b;
  font-size: 17px;
}

.card-actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
  margin-top: 8px;
}

.card-actions button {
  height: 32px;
  border: none;
  border-radius: 6px;
  background: #edf5ff;
  color: #1677ff;
  cursor: pointer;
  font-size: 13px;
  font-weight: 700;
}

.card-actions button:disabled,
.pagination button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.pagination button {
  height: 36px;
  padding: 0 16px;
  border: none;
  border-radius: 6px;
  background: #1677ff;
  color: #fff;
  cursor: pointer;
}

.pagination {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.page-item {
  min-width: 36px;
  height: 36px;
  padding: 0 10px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  cursor: pointer;
  user-select: none;
}

.page-item:hover {
  border-color: #1677ff;
  color: #1677ff;
}

.page-item.active {
  border-color: #1677ff;
  background: #1677ff;
  color: #fff;
}

.page-item.ellipsis {
  border: none;
  background: transparent;
  color: #999;
  cursor: default;
}

.jump-box {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: 8px;
  color: #6b7280;
  font-size: 13px;
}

.jump-box input {
  width: 60px;
  height: 36px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  padding: 0 8px;
  outline: none;
}

@media (max-width: 1200px) {
  .season-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 920px) {
  .season-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .tv-page {
    gap: 12px;
  }

  .filter-panel {
    padding: 12px;
    border-radius: 8px;
  }

  .filter-row {
    display: grid;
    grid-template-columns: auto minmax(0, 1fr);
    align-items: start;
    margin-bottom: 10px;
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
    white-space: nowrap;
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

  .search-options {
    flex: 1 1 auto;
    width: 100%;
    overflow: visible;
  }

  .season-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 16px 10px;
  }

  .season-card:hover {
    transform: none;
  }

  .poster-wrap {
    border-radius: 8px;
  }

  .poster-action-button {
    opacity: 1;
    transform: translateY(0);
  }

  .episode-badge {
    min-height: 22px;
    padding: 0 8px;
    font-size: 12px;
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
    margin-bottom: 4px;
    font-size: 14px;
  }

  .season-action-menu {
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

  .season-meta {
    margin-top: 8px;
    display: block;
  }

  .season-title,
  .season-rating {
    font-size: 14px;
  }

  .season-rating {
    display: inline-flex;
    margin-top: 2px;
  }

  .pagination {
    gap: 6px;
  }

  .pagination > button:first-child {
    order: 1;
    flex: 1 0 calc(50% - 3px);
  }

  .pagination > button:nth-last-child(2) {
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
  .season-title,
  .season-rating {
    font-size: 13px;
  }
}
</style>
