<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getPersonById, type PersonDetail, type PersonMovieCredit } from '@/api/person'

const route = useRoute()

const person = ref<PersonDetail | null>(null)
const loading = ref(false)
const errorMessage = ref('')
const selectedRole = ref('ALL')
const biographyRef = ref<HTMLElement | null>(null)
const isBiographyExpanded = ref(false)
const canToggleBiography = ref(false)
let biographyResizeTimer: ReturnType<typeof setTimeout> | undefined
const mobileBiographyCollapsedHeight = 280

const roleOptions = [
  { key: 'ALL', label: '全部' },
  { key: 'Director', label: '导演' },
  { key: 'Writer', label: '编剧' },
  { key: 'Producer', label: '制片' },
  { key: 'Actor', label: '演员' }
]

const profileHeroStyle = computed(() => {
  const styles: Record<string, string> = {}

  if (person.value?.profilePath) {
    styles['--person-backdrop'] = `url("${getImageUrl(person.value.profilePath, 'w500')}")`
  }

  return styles
})

const birthYear = computed(() => {
  if (!person.value?.birthday) {
    return ''
  }

  return person.value.birthday.slice(0, 4)
})

const occupationTags = computed(() => {
  if (!person.value) {
    return []
  }

  const jobs = new Set(
    person.value.credits
      .flatMap((credit) => credit.jobs ?? [])
      .map((job) => normalizeJob(job))
  )

  const tags = roleOptions
    .filter((option) => option.key !== 'ALL' && jobs.has(option.key.toLowerCase()))
    .map((option) => option.label)

  if (tags.length > 0) {
    return tags
  }

  return person.value.knownForDepartment ? [getDepartmentLabel(person.value.knownForDepartment)] : []
})

const availableRoleTabs = computed(() => {
  if (!person.value) {
    return roleOptions.slice(0, 1)
  }

  return roleOptions.filter((option) => {
    if (option.key === 'ALL') {
      return true
    }

    return person.value?.credits.some((credit) => hasJob(credit, option.key))
  })
})

const filteredCredits = computed(() => {
  if (!person.value) {
    return []
  }

  if (selectedRole.value === 'ALL') {
    return person.value.credits
  }

  return person.value.credits.filter((credit) => hasJob(credit, selectedRole.value))
})

const topRatedCredits = computed(() => {
  if (!person.value) {
    return []
  }

  return [...person.value.credits]
    .filter((credit) => getDisplayVoteAverage(credit) !== null && getDisplayVoteAverage(credit) !== undefined)
    .sort((a, b) => Number(getDisplayVoteAverage(b)) - Number(getDisplayVoteAverage(a)))
    .slice(0, 5)
})

async function loadPersonDetail() {
  loading.value = true
  errorMessage.value = ''
  selectedRole.value = 'ALL'
  isBiographyExpanded.value = false
  canToggleBiography.value = false

  try {
    const id = Number(route.params.id)
    if (!Number.isFinite(id)) {
      throw new Error('Invalid person id')
    }

    person.value = await getPersonById(id)
    document.title = `${person.value.name} - 人物详情`
  } catch (error) {
    console.error(error)
    errorMessage.value = '加载人物详情失败'
  } finally {
    loading.value = false
  }

  if (person.value && !errorMessage.value) {
    await updateBiographyToggle()
  }
}

async function updateBiographyToggle() {
  await nextTick()

  const biographyElement = biographyRef.value
  if (!biographyElement || !person.value?.biography) {
    canToggleBiography.value = false
    return
  }

  const isMobile = window.matchMedia('(max-width: 760px)').matches
  if (!isMobile) {
    canToggleBiography.value = false
    isBiographyExpanded.value = false
    return
  }

  const wasExpanded = isBiographyExpanded.value
  if (wasExpanded) {
    isBiographyExpanded.value = false
    await nextTick()
  }

  canToggleBiography.value = biographyElement.scrollHeight > mobileBiographyCollapsedHeight + 2

  if (wasExpanded && canToggleBiography.value) {
    isBiographyExpanded.value = true
    await nextTick()
  }
}

function toggleBiography() {
  isBiographyExpanded.value = !isBiographyExpanded.value
}

function handleBiographyResize() {
  if (biographyResizeTimer) {
    clearTimeout(biographyResizeTimer)
  }

  biographyResizeTimer = setTimeout(() => {
    void updateBiographyToggle()
  }, 120)
}

function getImageUrl(path: string | null, size: string) {
  if (!path) {
    return ''
  }

  return `https://image.tmdb.org/t/p/${size}${path}`
}

function formatDate(value: string | null | undefined) {
  if (!value) {
    return '未知'
  }

  const [year, month, day] = value.slice(0, 10).split('-')
  if (!year || !month || !day) {
    return value
  }

  return `${year}-${month}-${day}`
}

function formatYear(value: string | null | undefined) {
  if (!value) {
    return '未知'
  }

  return value.slice(0, 4)
}

function formatVoteAverage(value: number | null | undefined) {
  if (value === null || value === undefined) {
    return '暂无'
  }

  return Number(value).toFixed(2)
}

function getDisplayVoteAverage(credit: PersonMovieCredit) {
  return credit.imdbVoteAverage ?? credit.voteAverage
}

function formatPopularity(value: number | null | undefined) {
  if (value === null || value === undefined) {
    return '暂无'
  }

  return Number(value).toFixed(2)
}

function getGenderText(value: number | null | undefined) {
  switch (value) {
    case 1:
      return '女性'
    case 2:
      return '男性'
    case 3:
      return '非二元'
    default:
      return '未知'
  }
}

function normalizeJob(job: string | null | undefined) {
  return (job ?? '').trim().toLowerCase()
}

function hasJob(credit: PersonMovieCredit, job: string) {
  return credit.jobs?.some((item) => normalizeJob(item) === normalizeJob(job)) ?? false
}

function getJobLabel(job: string | null | undefined) {
  switch (normalizeJob(job)) {
    case 'actor':
      return '演员'
    case 'director':
      return '导演'
    case 'writer':
      return '编剧'
    case 'producer':
      return '制片'
    default:
      return job || '参与'
  }
}

function getDepartmentLabel(value: string) {
  switch (normalizeJob(value)) {
    case 'acting':
      return '演员'
    case 'directing':
      return '导演'
    case 'writing':
      return '编剧'
    case 'production':
      return '制片'
    default:
      return value
  }
}

function getCreditRole(credit: PersonMovieCredit) {
  if (hasJob(credit, 'Actor') && credit.character) {
    return `饰 ${credit.character}`
  }

  if (!credit.jobs || credit.jobs.length === 0) {
    return '参与'
  }

  return credit.jobs.map((job) => getJobLabel(job)).join(' / ')
}

function getCreditPosterUrl(credit: PersonMovieCredit) {
  return credit.posterPath ? getImageUrl(credit.posterPath, 'w342') : ''
}

watch(() => route.params.id, () => {
  void loadPersonDetail()
}, {
  immediate: true
})

onMounted(() => {
  window.addEventListener('resize', handleBiographyResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleBiographyResize)

  if (biographyResizeTimer) {
    clearTimeout(biographyResizeTimer)
  }
})
</script>

<template>
  <div class="person-detail-page">
    <div v-if="loading" class="status">正在加载...</div>
    <div v-else-if="errorMessage" class="status error">{{ errorMessage }}</div>

    <template v-else-if="person">
      <section class="person-hero" :style="profileHeroStyle">
        <div class="person-hero-inner">
          <div class="profile-section">
            <img
              v-if="person.profilePath"
              :src="getImageUrl(person.profilePath, 'w500')"
              :alt="person.name"
              class="profile-image"
            />
            <div v-else class="profile-image placeholder">
              {{ person.name?.slice(0, 1) || '?' }}
            </div>
          </div>

          <div class="person-summary">
            <div class="person-title-row">
              <h1>{{ person.name }}</h1>
              <span v-if="birthYear" class="birth-year">{{ birthYear }}</span>
            </div>
            <div class="original-name">{{ person.originalName || '原名未知' }}</div>

            <div class="tag-row">
              <span v-for="tag in occupationTags" :key="tag" class="tag">{{ tag }}</span>
              <span class="tag">{{ getGenderText(person.gender) }}</span>
            </div>
          </div>

          <div class="person-stats">
            <div class="stat-card">
              <span>出生日期</span>
              <strong>{{ formatDate(person.birthday) }}</strong>
            </div>
            <div class="stat-card">
              <span>出生地</span>
              <strong>{{ person.placeOfBirth || '未知' }}</strong>
            </div>
            <div v-if="person.deathday" class="stat-card">
              <span>去世日期</span>
              <strong>{{ formatDate(person.deathday) }}</strong>
            </div>
            <div class="stat-card">
              <span>参与作品</span>
              <strong>{{ person.movieCreditCount || 0 }} 部</strong>
            </div>
            <div class="stat-card">
              <span>TMDB热度</span>
              <strong>{{ formatPopularity(person.popularity) }}</strong>
            </div>
          </div>
        </div>
      </section>

      <div class="person-content">
        <main class="person-main">
          <section class="detail-panel">
            <h2>人物简介</h2>
            <div
              class="biography-wrap"
              :class="{
                collapsible: canToggleBiography,
                expanded: isBiographyExpanded
              }"
            >
              <p ref="biographyRef" class="biography">{{ person.biography || '暂无人物简介' }}</p>
              <button
                v-if="canToggleBiography"
                type="button"
                class="biography-toggle"
                @click="toggleBiography"
              >
                {{ isBiographyExpanded ? '收起' : '展开' }}
              </button>
            </div>
          </section>

          <section class="detail-panel works-panel">
            <div class="panel-heading">
              <h2>参与作品</h2>
              <span>按上映日期倒序</span>
            </div>

            <div class="role-tabs">
              <button
                v-for="option in availableRoleTabs"
                :key="option.key"
                type="button"
                class="role-tab"
                :class="{ active: selectedRole === option.key }"
                @click="selectedRole = option.key"
              >
                {{ option.label }}
              </button>
            </div>

            <div v-if="filteredCredits.length > 0" class="work-grid">
              <RouterLink
                v-for="credit in filteredCredits"
                :key="credit.movieId"
                class="work-card"
                :to="`/movies/${credit.movieId}`"
                target="_blank"
                rel="noopener noreferrer"
              >
                <div class="work-poster">
                  <img
                    v-if="getCreditPosterUrl(credit)"
                    :src="getCreditPosterUrl(credit)"
                    :alt="credit.title"
                  />
                  <div v-else class="work-poster placeholder">No Image</div>
                  <span class="credit-role">{{ getCreditRole(credit) }}</span>
                </div>
                <div class="work-title">{{ credit.title }}</div>
                <div class="work-meta">
                  <span>{{ formatYear(credit.releaseDate) }}</span>
                  <strong>{{ formatVoteAverage(getDisplayVoteAverage(credit)) }}</strong>
                </div>
              </RouterLink>
            </div>
            <p v-else class="empty-text">暂无作品信息</p>
          </section>
        </main>

        <aside class="person-sidebar">
          <section v-if="topRatedCredits.length > 0" class="detail-panel side-panel">
            <h2>高分作品</h2>
            <div class="top-list">
              <RouterLink
                v-for="(credit, index) in topRatedCredits"
                :key="`top-${credit.movieId}`"
                class="top-item"
                :to="`/movies/${credit.movieId}`"
                target="_blank"
                rel="noopener noreferrer"
              >
                <span class="top-rank">{{ index + 1 }}</span>
                <span class="top-info">
                  <strong>{{ credit.title }}</strong>
                  <em>{{ getCreditRole(credit) }}</em>
                </span>
                <span class="top-score">{{ formatVoteAverage(getDisplayVoteAverage(credit)) }}</span>
              </RouterLink>
            </div>
          </section>
        </aside>
      </div>
    </template>
  </div>
</template>

<style scoped>
.person-detail-page {
  padding: 24px;
  background: #f3f4f6;
}

.status {
  padding: 42px 0;
  color: #666;
  text-align: center;
}

.status.error {
  color: #d93026;
}

.person-hero {
  position: relative;
  overflow: hidden;
  border-radius: 14px;
  background:
    linear-gradient(110deg, rgba(15, 23, 42, 0.98), rgba(34, 50, 72, 0.9)),
    var(--person-backdrop, linear-gradient(#111827, #263548));
  background-size: cover;
  background-position: center;
  color: #fff;
}

.person-hero::before {
  content: "";
  position: absolute;
  inset: 0;
  background:
    linear-gradient(90deg, rgba(17, 24, 39, 0.88), rgba(17, 24, 39, 0.78)),
    var(--person-backdrop, none);
  background-size: cover;
  background-position: center;
  filter: blur(18px) saturate(1.05);
  transform: scale(1.08);
  opacity: 0.52;
}

.person-hero-inner {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 190px minmax(0, 1fr);
  grid-template-areas:
    "profile summary"
    "profile stats";
  gap: 18px 26px;
  align-items: start;
  padding: 34px;
}

.profile-section {
  grid-area: profile;
  width: 190px;
}

.profile-image {
  width: 100%;
  aspect-ratio: 2 / 3;
  object-fit: cover;
  display: block;
  border-radius: 10px;
  background: #253041;
  box-shadow: 0 18px 42px rgba(0, 0, 0, 0.34);
}

.work-poster.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #e5e7eb;
  color: #999;
  font-size: 14px;
  font-weight: 400;
}

.profile-image.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(circle at 50% 26%, rgba(255, 255, 255, 0.52), transparent 24%),
    linear-gradient(145deg, #dfe6ee, #b9c6d5);
  color: #516176;
  font-weight: 850;
}

.profile-image.placeholder {
  font-size: 42px;
}

.person-summary {
  grid-area: summary;
  align-self: center;
  min-width: 0;
}

.person-title-row {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 14px;
}

.person-title-row h1 {
  max-width: 100%;
  margin: 0;
  overflow-wrap: break-word;
  color: #fff;
  font-size: clamp(30px, 3.4vw, 42px);
  line-height: 1.12;
}

.birth-year {
  color: #d8e5f7;
  font-size: 19px;
  font-weight: 850;
}

.original-name {
  margin-top: 8px;
  color: #cbd5e1;
  font-size: 15px;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}

.tag {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  color: #edf4ff;
  font-size: 13px;
  font-weight: 800;
  backdrop-filter: blur(10px);
}

.person-stats {
  grid-area: stats;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.stat-card {
  min-height: 76px;
  padding: 13px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(12px);
}

.stat-card span {
  display: block;
  margin-bottom: 8px;
  color: #b9c7d9;
  font-size: 12px;
  font-weight: 700;
}

.stat-card strong {
  display: -webkit-box;
  overflow: hidden;
  color: #fff;
  font-size: 18px;
  line-height: 1.32;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.person-content {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 22px;
  margin-top: 22px;
}

.detail-panel {
  padding: 20px;
  border: 1px solid #e4e8ef;
  border-radius: 12px;
  background: #fff;
}

.detail-panel + .detail-panel {
  margin-top: 22px;
}

.detail-panel h2 {
  margin: 0 0 14px;
  color: #111827;
  font-size: 20px;
}

.biography {
  margin: 0;
  color: #3f4654;
  font-size: 14px;
  line-height: 1.85;
}

.biography-wrap {
  position: relative;
}

.biography-toggle {
  display: none;
  position: relative;
  z-index: 1;
  margin-top: 10px;
  padding: 0;
  border: none;
  background: transparent;
  color: #1677ff;
  font-size: 14px;
  font-weight: 850;
  cursor: pointer;
}

.panel-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.panel-heading h2 {
  margin: 0;
}

.panel-heading span {
  color: #6b7280;
  font-size: 13px;
}

.role-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  overflow-x: auto;
  scrollbar-width: none;
}

.role-tabs::-webkit-scrollbar {
  display: none;
}

.role-tab {
  flex: 0 0 auto;
  height: 32px;
  padding: 0 13px;
  border: none;
  border-radius: 999px;
  background: #f1f3f6;
  color: #2b3340;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}

.role-tab.active {
  background: #1677ff;
  color: #fff;
}

.work-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
}

.work-card {
  min-width: 0;
  color: inherit;
  text-decoration: none;
}

.work-poster {
  position: relative;
  overflow: hidden;
  aspect-ratio: 2 / 3;
  border-radius: 9px;
  background: #d8dee7;
  box-shadow: 0 8px 24px rgba(16, 24, 40, 0.14);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.work-card:hover .work-poster {
  transform: translateY(-2px);
  box-shadow: 0 14px 26px rgba(16, 24, 40, 0.18);
}

.work-poster img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.credit-role {
  position: absolute;
  left: 8px;
  bottom: 8px;
  max-width: calc(100% - 16px);
  padding: 5px 8px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(12, 20, 33, 0.76);
  color: #fff;
  font-size: 12px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.work-title {
  margin-top: 8px;
  overflow: hidden;
  color: #111827;
  font-weight: 900;
  line-height: 1.32;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.work-meta {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  margin-top: 4px;
  color: #7a8493;
  font-size: 13px;
}

.work-meta strong,
.top-score {
  color: #f5a400;
  font-weight: 900;
}

.person-sidebar {
  display: grid;
  gap: 18px;
  align-content: start;
}

.side-panel {
  padding: 18px;
}

.top-list {
  display: grid;
  gap: 10px;
}

.top-item {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  padding: 10px;
  border-radius: 8px;
  background: #f6f8fb;
  color: inherit;
  text-decoration: none;
}

.top-item:hover {
  background: #eef5ff;
}

.top-rank {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: #e8f2ff;
  color: #1677ff;
  font-weight: 900;
}

.top-info {
  min-width: 0;
}

.top-info strong,
.top-info em {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.top-info strong {
  color: #111827;
  font-size: 14px;
}

.top-info em {
  margin-top: 3px;
  color: #778292;
  font-size: 12px;
  font-style: normal;
}

.empty-text {
  margin: 0;
  color: #778190;
}

@media (max-width: 1120px) {
  .person-content {
    grid-template-columns: 1fr;
  }

  .person-sidebar {
    display: block;
  }

  .work-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .person-detail-page {
    padding: 14px;
  }

  .person-hero {
    border-radius: 16px;
  }

  .person-hero-inner {
    grid-template-columns: 104px minmax(0, 1fr);
    grid-template-areas:
      "profile summary"
      "stats stats";
    gap: 15px;
    padding: 18px;
  }

  .profile-section {
    width: 104px;
  }

  .profile-image {
    border-radius: 9px;
  }

  .person-title-row {
    display: block;
  }

  .person-title-row h1 {
    display: -webkit-box;
    overflow: hidden;
    font-size: 25px;
    line-height: 1.16;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }

  .birth-year {
    display: inline-block;
    margin-top: 5px;
    font-size: 15px;
  }

  .original-name {
    margin-top: 5px;
    font-size: 13px;
  }

  .tag-row {
    gap: 7px;
    margin-top: 10px;
  }

  .tag {
    min-height: 28px;
    padding: 0 9px;
    font-size: 12px;
  }

  .person-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    margin-top: 2px;
  }

  .stat-card {
    min-height: 64px;
    padding: 10px;
  }

  .stat-card strong {
    font-size: 15px;
  }

  .person-content {
    display: block;
    margin-top: 14px;
  }

  .detail-panel {
    padding: 16px;
    border-radius: 14px;
  }

  .detail-panel + .detail-panel,
  .person-sidebar {
    margin-top: 14px;
  }

  .detail-panel h2 {
    font-size: 19px;
  }

  .biography {
    font-size: 13px;
    line-height: 1.75;
  }

  .biography-wrap.collapsible:not(.expanded) {
    max-height: 280px;
    overflow: hidden;
    padding-bottom: 34px;
  }

  .biography-wrap.collapsible:not(.expanded) .biography {
    max-height: none;
  }

  .biography-wrap.collapsible:not(.expanded)::after {
    content: "";
    position: absolute;
    left: 0;
    right: 0;
    bottom: 0;
    height: 88px;
    background: linear-gradient(180deg, rgba(255, 255, 255, 0), #fff 82%);
    pointer-events: none;
  }

  .biography-toggle {
    display: inline-flex;
    justify-content: center;
    align-items: center;
    min-width: 46px;
    height: 28px;
    margin-top: 12px;
  }

  .biography-wrap.collapsible:not(.expanded) .biography-toggle {
    position: absolute;
    right: 0;
    bottom: 0;
    z-index: 2;
    margin-top: 0;
    padding-left: 14px;
    background: linear-gradient(90deg, rgba(255, 255, 255, 0), #fff 28%);
  }

  .biography-wrap.collapsible.expanded .biography-toggle {
    display: flex;
    width: fit-content;
    margin-left: auto;
  }

  .panel-heading {
    align-items: flex-start;
    margin-bottom: 12px;
  }

  .panel-heading span {
    display: none;
  }

  .work-grid {
    display: flex;
    gap: 12px;
    overflow-x: auto;
    padding-bottom: 4px;
    scrollbar-width: none;
  }

  .work-grid::-webkit-scrollbar {
    display: none;
  }

  .work-card {
    flex: 0 0 122px;
  }

  .work-card:hover .work-poster {
    transform: none;
    box-shadow: 0 8px 24px rgba(16, 24, 40, 0.14);
  }

  .work-title {
    font-size: 14px;
  }

  .top-item {
    grid-template-columns: 30px minmax(0, 1fr) auto;
  }
}
</style>
