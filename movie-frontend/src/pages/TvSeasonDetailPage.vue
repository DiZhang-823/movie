<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getTvSeasonById, type TvSeasonDetail } from '../api/tvSeason'
import { getUserTvSeason, saveUserTvSeason, type UserTvSeasonResponse } from '../api/userTvSeason'
import type { PersonItem } from '../api/movie'
import { getToken } from '../utils/auth'

type UserStatus = 'WANT_TO_WATCH' | 'WATCHED' | 'NOT_INTERESTED'

const route = useRoute()
const season = ref<TvSeasonDetail | null>(null)
const userSeason = ref<UserTvSeasonResponse | null>(null)
const loading = ref(false)
const errorMessage = ref('')
const saving = ref(false)
const visibleCastCount = ref(12)

const displayCast = computed(() => season.value?.cast.slice(0, visibleCastCount.value) ?? [])
const hasMoreCast = computed(() => (season.value?.cast.length ?? 0) > visibleCastCount.value)
const releaseYear = computed(() => season.value?.year || season.value?.airDate?.slice(0, 4) || '年份未知')
const heroStyle = computed(() => {
  if (!season.value?.posterPath) return {}
  return { '--detail-backdrop': `url("${getImageUrl(season.value.posterPath, 'w500')}")` }
})

async function loadPage() {
  loading.value = true
  errorMessage.value = ''
  userSeason.value = null
  visibleCastCount.value = 12
  try {
    const id = Number(route.params.id)
    season.value = await getTvSeasonById(id)
    document.title = `${season.value.title} - 剧集`

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

function formatList(values: string[] | null | undefined) {
  return values && values.length ? values.join(' / ') : '暂无'
}

function getDisplayVoteAverage(item: TvSeasonDetail) {
  return item.imdbVoteAverage ?? item.voteAverage
}

function getDisplayVoteCount(item: TvSeasonDetail) {
  return item.imdbVoteCount ?? item.voteCount
}

function getPersonRole(person: PersonItem) {
  return person.character ? `饰 ${person.character}` : person.job || person.originalName || ''
}

function getStatusText(status: UserStatus) {
  if (status === 'WANT_TO_WATCH') return '想看'
  if (status === 'WATCHED') return '看过'
  return '不想看'
}

async function markSeason(status: UserStatus) {
  if (!season.value) return
  const token = getToken()
  if (!token) {
    window.location.href = `/login?redirect=${encodeURIComponent(`/tv-seasons/${season.value.id}`)}`
    return
  }

  saving.value = true
  try {
    await saveUserTvSeason(season.value.id, {
      status,
      watchedDate: status === 'WATCHED' ? new Date().toISOString().slice(0, 10) : undefined
    }, token)
    userSeason.value = await getUserTvSeason(season.value.id, token)
  } finally {
    saving.value = false
  }
}

function expandCast() {
  visibleCastCount.value += 12
}

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
        <div class="hero-inner">
          <div class="poster-box">
            <img v-if="season.posterPath" :src="getImageUrl(season.posterPath, 'w500')" :alt="season.title" />
            <div v-else class="poster-placeholder">{{ season.title.slice(0, 1) }}</div>
          </div>

          <div class="hero-main">
            <div class="title-row">
              <h1>{{ season.title }}</h1>
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

            <div class="score-grid">
              <div>
                <span>评分</span>
                <strong>{{ formatScore(getDisplayVoteAverage(season)) }}</strong>
              </div>
              <div>
                <span>评分人数</span>
                <strong>{{ formatCount(getDisplayVoteCount(season)) }}</strong>
              </div>
              <div>
                <span>首播</span>
                <strong>{{ formatDate(season.airDate) }}</strong>
              </div>
            </div>

            <div class="actions">
              <button :class="{ active: userSeason?.status === 'WANT_TO_WATCH' }" :disabled="saving" @click="markSeason('WANT_TO_WATCH')">想看</button>
              <button :class="{ active: userSeason?.status === 'WATCHED' }" :disabled="saving" @click="markSeason('WATCHED')">看过</button>
              <button :class="{ active: userSeason?.status === 'NOT_INTERESTED' }" :disabled="saving" @click="markSeason('NOT_INTERESTED')">不想看</button>
            </div>
            <div v-if="userSeason" class="record-pill">
              {{ getStatusText(userSeason.status) }}
              <span v-if="userSeason.watchedDate || userSeason.updatedAt">{{ formatDate(userSeason.watchedDate || userSeason.updatedAt) }}</span>
            </div>
          </div>
        </div>
      </section>

      <main class="detail-layout">
        <div class="main-column">
          <section class="detail-section">
            <h2>剧情简介</h2>
            <p class="overview">{{ season.overview || '暂无简介' }}</p>
          </section>

          <section class="detail-section">
            <h2>基础信息</h2>
            <div class="facts-grid">
              <div><span>剧名</span><strong>{{ season.showName }}</strong></div>
              <div><span>季数</span><strong>第 {{ season.seasonNumber }} 季</strong></div>
              <div><span>原名</span><strong>{{ season.originalTitle || '未知' }}</strong></div>
              <div><span>类型</span><strong>{{ formatList(season.genres) }}</strong></div>
              <div><span>国家/地区</span><strong>{{ formatList(season.countries) }}</strong></div>
              <div><span>集数</span><strong>{{ season.episodeCount ?? '未知' }} 集</strong></div>
            </div>
          </section>

          <section class="detail-section">
            <h2>主要演员</h2>
            <div v-if="displayCast.length" class="people-grid">
              <RouterLink v-for="person in displayCast" :key="person.personId" class="person-card" :to="`/persons/${person.personId}`">
                <img v-if="person.profilePath" :src="getImageUrl(person.profilePath, 'w185')" :alt="person.name" />
                <div v-else class="avatar-placeholder">{{ person.name.slice(0, 1) }}</div>
                <div class="person-info">
                  <strong>{{ person.name }}</strong>
                  <span>{{ getPersonRole(person) }}</span>
                </div>
              </RouterLink>
            </div>
            <p v-else class="empty">暂无演员信息</p>
            <button v-if="hasMoreCast" class="expand-btn" @click="expandCast">展开更多</button>
          </section>
        </div>

        <aside class="side-column">
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
</template>

<style scoped>
.tv-detail-page {
  display: grid;
  gap: 20px;
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
  background: #111827;
  color: #fff;
}

.detail-hero::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image: linear-gradient(90deg, rgba(17, 24, 39, 0.96), rgba(17, 24, 39, 0.78)), var(--detail-backdrop);
  background-position: center;
  background-size: cover;
  filter: blur(12px);
  transform: scale(1.06);
}

.hero-inner {
  position: relative;
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  gap: 28px;
  padding: 34px;
}

.poster-box img,
.poster-placeholder {
  width: 100%;
  aspect-ratio: 2 / 3;
  border-radius: 8px;
  object-fit: cover;
  box-shadow: 0 22px 48px rgba(0, 0, 0, 0.34);
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
  gap: 12px;
  flex-wrap: wrap;
}

.title-row h1 {
  margin: 0;
  font-size: 40px;
  line-height: 1.15;
}

.title-row span {
  color: rgba(255, 255, 255, 0.68);
  font-size: 24px;
  font-weight: 800;
}

.original-title {
  color: rgba(255, 255, 255, 0.72);
}

.meta-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 20px;
}

.meta-tags span,
.meta-tags a {
  min-height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.12);
  color: rgba(255, 255, 255, 0.88);
  text-decoration: none;
  font-size: 13px;
  font-weight: 700;
}

.score-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  max-width: 620px;
  margin-top: 24px;
}

.score-grid div {
  min-height: 74px;
  padding: 12px 14px;
  border: 1px solid rgba(255, 255, 255, 0.12);
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
  font-size: 22px;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 24px;
}

.actions button {
  height: 40px;
  padding: 0 18px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
  cursor: pointer;
  font-weight: 800;
}

.actions button.active {
  background: #86dc7f;
  color: #143819;
}

.record-pill {
  width: fit-content;
  max-width: 100%;
  min-height: 36px;
  padding: 0 12px;
  margin-top: 12px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.12);
  color: rgba(255, 255, 255, 0.88);
  font-size: 13px;
  font-weight: 800;
}

.detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 20px;
  align-items: start;
}

.detail-section,
.side-panel {
  border: 1px solid #e7ebf0;
  border-radius: 8px;
  background: #fff;
}

.detail-section {
  padding: 22px;
}

.detail-section + .detail-section,
.side-panel + .side-panel {
  margin-top: 16px;
}

.detail-section h2,
.side-panel h2 {
  margin: 0 0 14px;
  color: #172033;
  font-size: 21px;
}

.overview {
  margin: 0;
  color: #3f4a5c;
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
}

.person-info span {
  margin-top: 4px;
  color: #778190;
  font-size: 12px;
}

.expand-btn {
  display: block;
  min-width: 130px;
  height: 38px;
  margin: 16px auto 0;
  border: 1px solid #dcdfe6;
  border-radius: 999px;
  background: #fff;
  color: #333;
  cursor: pointer;
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

@media (max-width: 1000px) {
  .hero-inner {
    grid-template-columns: 200px minmax(0, 1fr);
    padding: 28px;
  }

  .detail-layout {
    grid-template-columns: 1fr;
  }

  .side-column {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 14px;
  }

  .side-panel + .side-panel {
    margin-top: 0;
  }

  .people-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .hero-inner {
    grid-template-columns: 112px minmax(0, 1fr);
    gap: 14px;
    padding: 18px 14px;
  }

  .title-row h1 {
    font-size: 25px;
  }

  .title-row span {
    font-size: 18px;
  }

  .original-title {
    margin: 6px 0 0;
    font-size: 13px;
  }

  .meta-tags {
    margin-top: 12px;
    gap: 7px;
  }

  .meta-tags span,
  .meta-tags a {
    min-height: 28px;
    padding: 0 9px;
    font-size: 12px;
  }

  .score-grid,
  .actions,
  .record-pill {
    grid-column: 1 / -1;
  }

  .score-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 8px;
  }

  .score-grid div {
    min-height: 58px;
    padding: 9px;
  }

  .score-grid strong {
    font-size: 17px;
  }

  .actions {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .actions button {
    padding: 0 8px;
  }

  .detail-section {
    padding: 18px;
  }

  .facts-grid,
  .side-column {
    grid-template-columns: 1fr;
  }

  .people-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }
}
</style>
