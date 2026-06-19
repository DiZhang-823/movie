<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import {
  createAwardBody,
  createAwardCategory,
  createAwardEdition,
  createAwardNomination,
  deleteAwardBody,
  deleteAwardEdition,
  getAwardBodies,
  getAwardCategories,
  getAwardEditions,
  getAwardNominations,
  updateAwardBody,
  updateAwardCategory,
  updateAwardEdition,
  type AwardBody,
  type AwardCategory,
  type AwardEdition,
  type AwardNomination
} from '../api/award'
import { getMovies, type MovieItem } from '../api/movie'
import { searchPersons, type PersonSearchItem } from '../api/person'
import { getTvSeasons, type TvSeasonItem } from '../api/tvSeason'

const bodies = ref<AwardBody[]>([])
const editions = ref<AwardEdition[]>([])
const categories = ref<AwardCategory[]>([])
const nominations = ref<AwardNomination[]>([])
const selectedBodyId = ref<number | null>(null)
const selectedEditionId = ref<number | null>(null)
const selectedCategoryId = ref<number | null>(null)
const editingBodyId = ref<number | null>(null)
const editingEditionId = ref<number | null>(null)
const editingCategoryId = ref<number | null>(null)
const activeDetailTab = ref<'overview' | 'categories' | 'nominations'>('categories')
const loading = ref(false)
const saving = ref(false)
const message = ref('')
const errorMessage = ref('')
const bodyListExpanded = ref(false)
const activeBodyMenuId = ref<number | null>(null)
const activeEditionMenuId = ref<number | null>(null)

const movieQuery = ref('')
const tvSeasonQuery = ref('')
const personQuery = ref('')
const movieResults = ref<MovieItem[]>([])
const tvSeasonResults = ref<TvSeasonItem[]>([])
const personResults = ref<PersonSearchItem[]>([])
const selectedMovie = ref<MovieItem | null>(null)
const selectedTvSeason = ref<TvSeasonItem | null>(null)
const selectedPerson = ref<PersonSearchItem | null>(null)
const searchingMovie = ref(false)
const searchingTvSeason = ref(false)
const searchingPerson = ref(false)

const bodyForm = reactive({
  name: '',
  originalName: '',
  sortOrder: null as number | null,
  description: ''
})

const editionForm = reactive({
  name: '',
  editionNumber: null as number | null,
  awardYear: null as number | null,
  ceremonyDate: '',
  description: ''
})

const categoryForm = reactive({
  name: '',
  sortOrder: null as number | null,
  description: ''
})

const nominationForm = reactive({
  awardCategoryId: null as number | null,
  movieId: null as number | null,
  tvSeasonId: null as number | null,
  personId: null as number | null,
  winner: false,
  note: '',
  sortOrder: null as number | null
})

const selectedBody = computed(() => bodies.value.find((body) => body.id === selectedBodyId.value) ?? null)
const selectedEdition = computed(() => editions.value.find((edition) => edition.id === selectedEditionId.value) ?? null)
const selectedCategory = computed(() => categories.value.find((category) => category.id === selectedCategoryId.value) ?? null)
const bodyListCollapsible = computed(() => bodies.value.length > 5)

watch(selectedBodyId, async (bodyId) => {
  selectedEditionId.value = null
  selectedCategoryId.value = null
  editions.value = []
  categories.value = []
  nominations.value = []
  cancelEditionEdit()
  cancelCategoryEdit()
  clearNominationForm()
  if (bodyId) {
    await loadEditions(bodyId)
  }
})

watch(selectedEditionId, async (editionId) => {
  selectedCategoryId.value = null
  categories.value = []
  nominations.value = []
  cancelCategoryEdit()
  clearNominationForm()
  if (editionId) {
    await loadCategories(editionId)
  }
})

watch(selectedCategoryId, async (categoryId) => {
  nominations.value = []
  nominationForm.awardCategoryId = categoryId
  if (categoryId) {
    await loadNominations(categoryId)
  }
})

async function loadAwards() {
  loading.value = true
  errorMessage.value = ''
  try {
    bodies.value = await getAwardBodies()
    if (!selectedBodyId.value && bodies.value.length > 0) {
      selectedBodyId.value = bodies.value[0].id
    }
  } catch (error) {
    console.error(error)
    errorMessage.value = '读取奖项数据失败'
  } finally {
    loading.value = false
  }
}

async function loadEditions(bodyId: number) {
  editions.value = await getAwardEditions(bodyId)
  if (editions.value.length > 0 && !selectedEditionId.value) {
    selectedEditionId.value = editions.value[0].id
  }
}

async function loadCategories(editionId: number) {
  categories.value = await getAwardCategories(editionId)
  if (categories.value.length > 0 && !selectedCategoryId.value) {
    selectedCategoryId.value = categories.value[0].id
  }
}

async function loadNominations(categoryId: number) {
  nominations.value = await getAwardNominations(categoryId)
}

async function saveBody() {
  if (!bodyForm.name.trim()) {
    errorMessage.value = '请填写奖项名称'
    return
  }

  saving.value = true
  message.value = ''
  errorMessage.value = ''
  try {
    const payload = {
      name: bodyForm.name.trim(),
      originalName: optionalText(bodyForm.originalName),
      sortOrder: bodyForm.sortOrder,
      description: optionalText(bodyForm.description)
    }
    const saved = editingBodyId.value
      ? await updateAwardBody(editingBodyId.value, payload)
      : await createAwardBody(payload)

    clearBodyForm()
    await loadAwards()
    selectedBodyId.value = saved.id
    message.value = editingBodyId.value ? '奖项品牌已更新' : '奖项品牌已创建'
    editingBodyId.value = null
  } catch (error) {
    console.error(error)
    errorMessage.value = editingBodyId.value ? '更新奖项品牌失败' : '创建奖项品牌失败'
  } finally {
    saving.value = false
  }
}

async function saveEdition() {
  if (!selectedBodyId.value || !editionForm.name.trim() || !editionForm.editionNumber) {
    errorMessage.value = '请先选择奖项品牌，并填写届数名称和届数'
    return
  }
  if (!isValidDateText(editionForm.ceremonyDate)) {
    errorMessage.value = '颁奖日期格式必须是 YYYY-MM-DD'
    return
  }

  saving.value = true
  message.value = ''
  errorMessage.value = ''
  try {
    const payload = {
      awardBodyId: selectedBodyId.value,
      name: editionForm.name.trim(),
      editionNumber: editionForm.editionNumber,
      awardYear: editionForm.awardYear,
      ceremonyDate: optionalText(editionForm.ceremonyDate),
      description: optionalText(editionForm.description)
    }
    const saved = editingEditionId.value
      ? await updateAwardEdition(editingEditionId.value, payload)
      : await createAwardEdition(payload)

    clearEditionForm()
    await loadEditions(selectedBodyId.value)
    selectedEditionId.value = saved.id
    message.value = editingEditionId.value ? '奖项届数已更新' : '奖项届数已创建'
    editingEditionId.value = null
  } catch (error) {
    console.error(error)
    errorMessage.value = editingEditionId.value ? '更新奖项届数失败' : '创建奖项届数失败'
  } finally {
    saving.value = false
  }
}

async function saveCategory() {
  if (!selectedEditionId.value || !categoryForm.name.trim()) {
    errorMessage.value = '请先选择届数，并填写具体奖项名称'
    return
  }

  saving.value = true
  message.value = ''
  errorMessage.value = ''
  try {
    const payload = {
      awardEditionId: selectedEditionId.value,
      name: categoryForm.name.trim(),
      sortOrder: categoryForm.sortOrder,
      description: optionalText(categoryForm.description)
    }
    const saved = editingCategoryId.value
      ? await updateAwardCategory(editingCategoryId.value, payload)
      : await createAwardCategory(payload)

    clearCategoryForm()
    await loadCategories(selectedEditionId.value)
    selectedCategoryId.value = saved.id
    activeDetailTab.value = 'categories'
    message.value = editingCategoryId.value ? '具体奖项已更新' : '具体奖项已创建'
    editingCategoryId.value = null
  } catch (error) {
    console.error(error)
    errorMessage.value = editingCategoryId.value ? '更新具体奖项失败' : '创建具体奖项失败'
  } finally {
    saving.value = false
  }
}

async function saveNomination() {
  if (!nominationForm.awardCategoryId) {
    errorMessage.value = '请先选择具体奖项'
    return
  }
  if (nominationForm.movieId && nominationForm.tvSeasonId) {
    errorMessage.value = '电影和剧集季不能同时选择'
    return
  }
  if (!nominationForm.movieId && !nominationForm.tvSeasonId && !nominationForm.personId) {
    errorMessage.value = '提名至少需要电影、剧集季或人物中的一个'
    return
  }

  saving.value = true
  message.value = ''
  errorMessage.value = ''
  try {
    await createAwardNomination({
      awardCategoryId: nominationForm.awardCategoryId,
      movieId: nominationForm.movieId,
      tvSeasonId: nominationForm.tvSeasonId,
      personId: nominationForm.personId,
      winner: nominationForm.winner,
      note: optionalText(nominationForm.note),
      sortOrder: nominationForm.sortOrder
    })
    clearNominationForm()
    if (selectedCategoryId.value) {
      await loadNominations(selectedCategoryId.value)
    }
    activeDetailTab.value = 'nominations'
    message.value = '提名已添加'
  } catch (error) {
    console.error(error)
    errorMessage.value = '添加提名失败'
  } finally {
    saving.value = false
  }
}

function editBody(body: AwardBody) {
  activeBodyMenuId.value = null
  editingBodyId.value = body.id
  bodyForm.name = body.name
  bodyForm.originalName = body.originalName ?? ''
  bodyForm.sortOrder = body.sortOrder
  bodyForm.description = body.description ?? ''
}

function toggleBodyMenu(bodyId: number) {
  activeBodyMenuId.value = activeBodyMenuId.value === bodyId ? null : bodyId
}

async function deleteBody(body: AwardBody) {
  activeBodyMenuId.value = null
  const confirmed = window.confirm(`确认删除“${body.name}”吗？该品牌下的届数、具体奖项和提名也会一并删除。`)
  if (!confirmed) {
    return
  }

  saving.value = true
  message.value = ''
  errorMessage.value = ''
  try {
    await deleteAwardBody(body.id)
    if (editingBodyId.value === body.id) {
      cancelBodyEdit()
    }
    if (selectedBodyId.value === body.id) {
      selectedBodyId.value = null
    }
    await loadAwards()
    message.value = '奖项品牌已删除，后续排序已前移'
  } catch (error) {
    console.error(error)
    errorMessage.value = '删除奖项品牌失败'
  } finally {
    saving.value = false
  }
}

function editEdition(edition: AwardEdition) {
  activeEditionMenuId.value = null
  editingEditionId.value = edition.id
  editionForm.name = edition.name
  editionForm.editionNumber = edition.editionNumber
  editionForm.awardYear = edition.awardYear
  editionForm.ceremonyDate = edition.ceremonyDate ?? ''
  editionForm.description = edition.description ?? ''
  activeDetailTab.value = 'overview'
}

function toggleEditionMenu(editionId: number) {
  activeEditionMenuId.value = activeEditionMenuId.value === editionId ? null : editionId
}

async function deleteEdition(edition: AwardEdition) {
  activeEditionMenuId.value = null
  const confirmed = window.confirm(`确认删除“${edition.name}”吗？该届下的具体奖项和提名也会一并删除。`)
  if (!confirmed) {
    return
  }

  saving.value = true
  message.value = ''
  errorMessage.value = ''
  try {
    await deleteAwardEdition(edition.id)
    if (editingEditionId.value === edition.id) {
      cancelEditionEdit()
    }
    if (selectedEditionId.value === edition.id) {
      selectedEditionId.value = null
    }
    if (selectedBodyId.value) {
      await loadEditions(selectedBodyId.value)
    }
    message.value = '奖项届数已删除'
  } catch (error) {
    console.error(error)
    errorMessage.value = '删除奖项届数失败'
  } finally {
    saving.value = false
  }
}

function editCategory(category: AwardCategory) {
  editingCategoryId.value = category.id
  categoryForm.name = category.name
  categoryForm.sortOrder = category.sortOrder
  categoryForm.description = category.description ?? ''
  activeDetailTab.value = 'categories'
}

function cancelBodyEdit() {
  editingBodyId.value = null
  clearBodyForm()
}

function cancelEditionEdit() {
  editingEditionId.value = null
  clearEditionForm()
}

function cancelCategoryEdit() {
  editingCategoryId.value = null
  clearCategoryForm()
}

async function moveBody(index: number, direction: -1 | 1) {
  const targetIndex = index + direction
  if (targetIndex < 0 || targetIndex >= bodies.value.length) {
    return
  }

  const current = bodies.value[index]
  const target = bodies.value[targetIndex]
  await swapBodySort(current, target, normalizedSort(target, targetIndex), normalizedSort(current, index))
}

async function swapBodySort(current: AwardBody, target: AwardBody, currentSort: number, targetSort: number) {
  saving.value = true
  errorMessage.value = ''
  try {
    await updateAwardBody(current.id, toBodyPayload(current, currentSort))
    await updateAwardBody(target.id, toBodyPayload(target, targetSort))
    const preservedId = selectedBodyId.value
    await loadAwards()
    selectedBodyId.value = preservedId
    message.value = '奖项品牌顺序已更新'
  } catch (error) {
    console.error(error)
    errorMessage.value = '调整奖项品牌顺序失败'
  } finally {
    saving.value = false
  }
}

async function moveCategory(index: number, direction: -1 | 1) {
  const targetIndex = index + direction
  if (targetIndex < 0 || targetIndex >= categories.value.length) {
    return
  }

  const current = categories.value[index]
  const target = categories.value[targetIndex]
  saving.value = true
  errorMessage.value = ''
  try {
    await updateAwardCategory(current.id, toCategoryPayload(current, normalizedSort(target, targetIndex)))
    await updateAwardCategory(target.id, toCategoryPayload(target, normalizedSort(current, index)))
    if (selectedEditionId.value) {
      const preservedId = selectedCategoryId.value
      await loadCategories(selectedEditionId.value)
      selectedCategoryId.value = preservedId
    }
    message.value = '具体奖项顺序已更新'
  } catch (error) {
    console.error(error)
    errorMessage.value = '调整具体奖项顺序失败'
  } finally {
    saving.value = false
  }
}

async function searchMovies() {
  if (!movieQuery.value.trim()) {
    movieResults.value = []
    return
  }

  searchingMovie.value = true
  try {
    movieResults.value = (await getMovies({
      title: movieQuery.value.trim(),
      page: 0,
      size: 8,
      sortBy: 'ID',
      direction: 'asc'
    })).content
  } catch (error) {
    console.error(error)
    errorMessage.value = '搜索电影失败'
  } finally {
    searchingMovie.value = false
  }
}

async function searchTvSeasons() {
  if (!tvSeasonQuery.value.trim()) {
    tvSeasonResults.value = []
    return
  }

  searchingTvSeason.value = true
  try {
    tvSeasonResults.value = (await getTvSeasons({
      title: tvSeasonQuery.value.trim(),
      page: 0,
      size: 8,
      sortBy: 'ID',
      direction: 'asc'
    })).content
  } catch (error) {
    console.error(error)
    errorMessage.value = '搜索剧集失败'
  } finally {
    searchingTvSeason.value = false
  }
}

async function searchPeople() {
  if (!personQuery.value.trim()) {
    personResults.value = []
    return
  }

  searchingPerson.value = true
  try {
    personResults.value = await searchPersons(personQuery.value.trim(), 8)
  } catch (error) {
    console.error(error)
    errorMessage.value = '搜索人物失败'
  } finally {
    searchingPerson.value = false
  }
}

function selectMovie(movie: MovieItem) {
  selectedMovie.value = movie
  selectedTvSeason.value = null
  nominationForm.movieId = movie.id
  nominationForm.tvSeasonId = null
  movieResults.value = []
  movieQuery.value = movie.title
}

function selectTvSeason(tvSeason: TvSeasonItem) {
  selectedTvSeason.value = tvSeason
  selectedMovie.value = null
  nominationForm.tvSeasonId = tvSeason.id
  nominationForm.movieId = null
  tvSeasonResults.value = []
  tvSeasonQuery.value = tvSeason.title
}

function selectPerson(person: PersonSearchItem) {
  selectedPerson.value = person
  nominationForm.personId = person.personId
  personResults.value = []
  personQuery.value = person.name
}

function clearSelectedMovie() {
  selectedMovie.value = null
  nominationForm.movieId = null
  movieQuery.value = ''
}

function clearSelectedTvSeason() {
  selectedTvSeason.value = null
  nominationForm.tvSeasonId = null
  tvSeasonQuery.value = ''
}

function clearSelectedPerson() {
  selectedPerson.value = null
  nominationForm.personId = null
  personQuery.value = ''
}

function selectCategory(categoryId: number) {
  selectedCategoryId.value = categoryId
  activeDetailTab.value = 'nominations'
}

function optionalText(value: string) {
  const normalized = value.trim()
  return normalized ? normalized : null
}

function clearBodyForm() {
  bodyForm.name = ''
  bodyForm.originalName = ''
  bodyForm.sortOrder = null
  bodyForm.description = ''
}

function clearEditionForm() {
  editionForm.name = ''
  editionForm.editionNumber = null
  editionForm.awardYear = null
  editionForm.ceremonyDate = ''
  editionForm.description = ''
}

function clearCategoryForm() {
  categoryForm.name = ''
  categoryForm.sortOrder = null
  categoryForm.description = ''
}

function clearNominationForm() {
  nominationForm.movieId = null
  nominationForm.tvSeasonId = null
  nominationForm.personId = null
  nominationForm.winner = false
  nominationForm.note = ''
  nominationForm.sortOrder = null
  nominationForm.awardCategoryId = selectedCategoryId.value
  clearSelectedMovie()
  clearSelectedTvSeason()
  clearSelectedPerson()
}

function isValidDateText(value: string) {
  if (!value.trim()) {
    return true
  }
  return /^\d{4}-\d{2}-\d{2}$/.test(value.trim())
}

function formatDate(value?: string | null) {
  return value || '未设置'
}

function formatSort(value?: number | null) {
  return value ?? '-'
}

function normalizedSort(item: { sortOrder: number | null }, index: number) {
  return item.sortOrder ?? index + 1
}

function toBodyPayload(body: AwardBody, sortOrder: number) {
  return {
    name: body.name,
    originalName: body.originalName,
    sortOrder,
    description: body.description
  }
}

function toCategoryPayload(category: AwardCategory, sortOrder: number) {
  return {
    awardEditionId: category.awardEditionId,
    name: category.name,
    sortOrder,
    description: category.description
  }
}

function nominationTarget(nomination: AwardNomination) {
  const work = nomination.movieTitle || nomination.tvSeasonTitle
  if (nomination.personName && work) {
    return `${nomination.personName} · ${work}`
  }
  return nomination.personName || work || '未命名提名'
}

function tvSeasonLabel(tvSeason: TvSeasonItem) {
  return `${tvSeason.showName} 第${tvSeason.seasonNumber}季 · ${tvSeason.title}`
}

function closeBodyMenu() {
  activeBodyMenuId.value = null
  activeEditionMenuId.value = null
}

onMounted(() => {
  document.addEventListener('click', closeBodyMenu)
  loadAwards()
})

onBeforeUnmount(() => {
  document.removeEventListener('click', closeBodyMenu)
})
</script>

<template>
  <section class="awards-panel">
    <div v-if="message" class="award-message success">{{ message }}</div>
    <div v-if="errorMessage" class="award-message error">{{ errorMessage }}</div>

    <aside class="award-sidebar">
      <div class="sidebar-section">
        <div class="panel-heading">
          <div>
            <span>奖项库</span>
            <strong>奖项品牌</strong>
          </div>
          <button class="mini-btn" :disabled="loading" @click="loadAwards">刷新</button>
        </div>

        <form class="stack-form" @submit.prevent="saveBody">
          <input v-model="bodyForm.name" placeholder="奖项名称，如 金球奖" />
          <input v-model="bodyForm.originalName" placeholder="原名，如 Golden Globe Awards" />
          <input v-model.number="bodyForm.sortOrder" type="number" placeholder="重要程度排序" />
          <div class="form-actions">
            <button class="primary-btn" :disabled="saving">{{ editingBodyId ? '保存奖项' : '新建奖项' }}</button>
            <button v-if="editingBodyId" class="secondary-btn" type="button" @click="cancelBodyEdit">取消</button>
          </div>
        </form>

        <div class="select-list body-select-list" :class="{ expanded: bodyListExpanded || !bodyListCollapsible }">
          <div
            v-for="(body, index) in bodies"
            :key="body.id"
            class="select-row"
            :class="{ active: body.id === selectedBodyId }"
            role="button"
            tabindex="0"
            @click="selectedBodyId = body.id"
            @keydown.enter="selectedBodyId = body.id"
          >
            <span>{{ body.name }}</span>
            <em>#{{ formatSort(body.sortOrder) }}</em>
            <span class="row-actions">
              <button type="button" :disabled="index === 0 || saving" @click.stop="moveBody(index, -1)">↑</button>
              <button type="button" :disabled="index === bodies.length - 1 || saving" @click.stop="moveBody(index, 1)">↓</button>
              <span class="body-menu-wrap" @click.stop>
                <button
                  type="button"
                  class="body-menu-trigger"
                  :class="{ active: activeBodyMenuId === body.id }"
                  :disabled="saving"
                  aria-label="奖项品牌操作"
                  @click.stop="toggleBodyMenu(body.id)"
                >
                  <span></span><span></span><span></span>
                </button>
                <span v-if="activeBodyMenuId === body.id" class="body-action-menu">
                  <button type="button" :disabled="saving" @click.stop="editBody(body)">编辑</button>
                  <button type="button" class="danger-action" :disabled="saving" @click.stop="deleteBody(body)">删除</button>
                </span>
              </span>
            </span>
          </div>
          <div v-if="!bodies.length" class="empty-line">暂无奖项品牌</div>
        </div>

        <button
          v-if="bodyListCollapsible"
          class="collapse-btn"
          type="button"
          @click="bodyListExpanded = !bodyListExpanded"
        >
          {{ bodyListExpanded ? '收起奖项品牌' : `展开全部 ${bodies.length} 个奖项品牌` }}
        </button>
      </div>

      <div class="sidebar-section">
        <div class="panel-heading">
          <div>
            <span>{{ selectedBody?.name || '未选择奖项' }}</span>
            <strong>届数</strong>
          </div>
        </div>

        <form class="stack-form" @submit.prevent="saveEdition">
          <input v-model="editionForm.name" placeholder="第80届金球奖" />
          <div class="split-fields">
            <input v-model.number="editionForm.editionNumber" type="number" placeholder="届数" />
            <input v-model.number="editionForm.awardYear" type="number" placeholder="年份" />
          </div>
          <input v-model="editionForm.ceremonyDate" placeholder="YYYY-MM-DD" inputmode="numeric" />
          <div class="form-actions">
            <button class="primary-btn" :disabled="saving || !selectedBodyId">
              {{ editingEditionId ? '保存届数' : '新建届数' }}
            </button>
            <button v-if="editingEditionId" class="secondary-btn" type="button" @click="cancelEditionEdit">取消</button>
          </div>
        </form>

        <div class="select-list">
          <div
            v-for="edition in editions"
            :key="edition.id"
            class="select-row"
            :class="{ active: edition.id === selectedEditionId }"
            role="button"
            tabindex="0"
            @click="selectedEditionId = edition.id"
            @keydown.enter="selectedEditionId = edition.id"
          >
            <span>{{ edition.name }}</span>
            <em>{{ edition.awardYear || '-' }}</em>
            <span class="row-actions">
              <span class="body-menu-wrap" @click.stop>
                <button
                  type="button"
                  class="body-menu-trigger"
                  :class="{ active: activeEditionMenuId === edition.id }"
                  :disabled="saving"
                  aria-label="奖项届数操作"
                  @click.stop="toggleEditionMenu(edition.id)"
                >
                  <span></span><span></span><span></span>
                </button>
                <span v-if="activeEditionMenuId === edition.id" class="body-action-menu">
                  <button type="button" :disabled="saving" @click.stop="editEdition(edition)">编辑</button>
                  <button type="button" class="danger-action" :disabled="saving" @click.stop="deleteEdition(edition)">删除</button>
                </span>
              </span>
            </span>
          </div>
          <div v-if="selectedBodyId && !editions.length" class="empty-line">暂无届数</div>
        </div>
      </div>
    </aside>

    <main class="award-workspace">
      <header class="edition-header">
        <div>
          <span>{{ selectedBody?.name || '奖项管理' }}</span>
          <h2>{{ selectedEdition?.name || '请选择或新建届数' }}</h2>
          <p>{{ selectedEdition?.awardYear || '年份未设置' }} · 颁奖日期 {{ formatDate(selectedEdition?.ceremonyDate) }}</p>
        </div>
      </header>

      <div class="detail-tabs">
        <button :class="{ active: activeDetailTab === 'overview' }" @click="activeDetailTab = 'overview'">基本信息</button>
        <button :class="{ active: activeDetailTab === 'categories' }" @click="activeDetailTab = 'categories'">具体奖项</button>
        <button :class="{ active: activeDetailTab === 'nominations' }" @click="activeDetailTab = 'nominations'">提名名单</button>
      </div>

      <section v-if="activeDetailTab === 'overview'" class="workspace-grid single">
        <article class="work-card">
          <h3>当前届数</h3>
          <dl class="meta-list">
            <div><dt>奖项品牌</dt><dd>{{ selectedBody?.name || '-' }}</dd></div>
            <div><dt>届数名称</dt><dd>{{ selectedEdition?.name || '-' }}</dd></div>
            <div><dt>届数</dt><dd>{{ selectedEdition?.editionNumber || '-' }}</dd></div>
            <div><dt>颁奖日期</dt><dd>{{ formatDate(selectedEdition?.ceremonyDate) }}</dd></div>
          </dl>
        </article>
      </section>

      <section v-if="activeDetailTab === 'categories'" class="workspace-grid">
        <article class="work-card">
          <div class="card-heading">
            <h3>具体奖项</h3>
            <span>{{ categories.length }} 项</span>
          </div>

          <form class="inline-form" @submit.prevent="saveCategory">
            <input v-model="categoryForm.name" placeholder="最佳剧情片" />
            <input v-model.number="categoryForm.sortOrder" type="number" placeholder="排序" />
            <div class="form-actions">
              <button class="primary-btn" :disabled="saving || !selectedEditionId">
                {{ editingCategoryId ? '保存奖项' : '新建具体奖项' }}
              </button>
              <button v-if="editingCategoryId" class="secondary-btn" type="button" @click="cancelCategoryEdit">取消</button>
            </div>
          </form>

          <div class="category-table">
            <button
              v-for="(category, index) in categories"
              :key="category.id"
              class="category-row"
              :class="{ active: category.id === selectedCategoryId }"
              @click="selectCategory(category.id)"
            >
              <span class="sort-pill">{{ formatSort(category.sortOrder) }}</span>
              <strong>{{ category.name }}</strong>
              <span class="row-actions">
                <button type="button" :disabled="index === 0 || saving" @click.stop="moveCategory(index, -1)">↑</button>
                <button type="button" :disabled="index === categories.length - 1 || saving" @click.stop="moveCategory(index, 1)">↓</button>
                <button type="button" @click.stop="editCategory(category)">编辑</button>
              </span>
            </button>
            <div v-if="selectedEditionId && !categories.length" class="empty-state">这一届还没有具体奖项</div>
          </div>
        </article>

        <article class="work-card preview-card">
          <div class="card-heading">
            <h3>提名预览</h3>
            <span>{{ selectedCategory?.name || '未选择奖项' }}</span>
          </div>
          <div class="preview-list">
            <div v-for="nomination in nominations.slice(0, 5)" :key="nomination.id" class="preview-row">
              <span class="winner-badge" :class="{ muted: !nomination.winner }">
                {{ nomination.winner ? '获奖' : '提名' }}
              </span>
              <div>
                <strong>{{ nominationTarget(nomination) }}</strong>
                <small>{{ nomination.note || nomination.awardCategoryName }}</small>
              </div>
            </div>
            <div v-if="!nominations.length" class="empty-state">选择奖项后可查看提名预览</div>
          </div>
        </article>
      </section>

      <section v-if="activeDetailTab === 'nominations'" class="workspace-grid">
        <article class="work-card">
          <div class="card-heading">
            <h3>添加提名</h3>
            <span>{{ selectedCategory?.name || '请选择具体奖项' }}</span>
          </div>

          <form class="nomination-form" @submit.prevent="saveNomination">
            <label>
              <span>具体奖项</span>
              <select v-model.number="nominationForm.awardCategoryId">
                <option :value="null">请选择具体奖项</option>
                <option v-for="category in categories" :key="category.id" :value="category.id">{{ category.name }}</option>
              </select>
            </label>
            <label class="search-field">
              <span>电影</span>
              <div class="search-line">
                <input v-model="movieQuery" :disabled="!!selectedMovie" placeholder="搜索电影标题" @keyup.enter.prevent="searchMovies" />
                <button class="secondary-btn" type="button" :disabled="searchingMovie || !!selectedMovie" @click="searchMovies">
                  {{ searchingMovie ? '搜索中' : '搜索' }}
                </button>
              </div>
              <div v-if="selectedMovie" class="selected-chip">
                {{ selectedMovie.title }}
                <button type="button" @click="clearSelectedMovie">移除</button>
              </div>
              <div v-if="movieResults.length" class="result-list">
                <button v-for="movie in movieResults" :key="movie.id" type="button" @click="selectMovie(movie)">
                  <strong>{{ movie.title }}</strong>
                  <small>{{ movie.releaseDate || '日期未知' }}</small>
                </button>
              </div>
            </label>
            <label class="search-field">
              <span>剧集季</span>
              <div class="search-line">
                <input v-model="tvSeasonQuery" :disabled="!!selectedTvSeason" placeholder="搜索剧集标题" @keyup.enter.prevent="searchTvSeasons" />
                <button class="secondary-btn" type="button" :disabled="searchingTvSeason || !!selectedTvSeason" @click="searchTvSeasons">
                  {{ searchingTvSeason ? '搜索中' : '搜索' }}
                </button>
              </div>
              <div v-if="selectedTvSeason" class="selected-chip">
                {{ tvSeasonLabel(selectedTvSeason) }}
                <button type="button" @click="clearSelectedTvSeason">移除</button>
              </div>
              <div v-if="tvSeasonResults.length" class="result-list">
                <button v-for="tvSeason in tvSeasonResults" :key="tvSeason.id" type="button" @click="selectTvSeason(tvSeason)">
                  <strong>{{ tvSeasonLabel(tvSeason) }}</strong>
                  <small>{{ tvSeason.airDate || '日期未知' }}</small>
                </button>
              </div>
            </label>
            <label class="search-field">
              <span>人物</span>
              <div class="search-line">
                <input v-model="personQuery" :disabled="!!selectedPerson" placeholder="搜索人物姓名" @keyup.enter.prevent="searchPeople" />
                <button class="secondary-btn" type="button" :disabled="searchingPerson || !!selectedPerson" @click="searchPeople">
                  {{ searchingPerson ? '搜索中' : '搜索' }}
                </button>
              </div>
              <div v-if="selectedPerson" class="selected-chip">
                {{ selectedPerson.name }}
                <button type="button" @click="clearSelectedPerson">移除</button>
              </div>
              <div v-if="personResults.length" class="result-list">
                <button v-for="person in personResults" :key="person.personId" type="button" @click="selectPerson(person)">
                  <strong>{{ person.name }}</strong>
                  <small>{{ person.originalName || '暂无原名' }}</small>
                </button>
              </div>
            </label>
            <label>
              <span>备注</span>
              <input v-model="nominationForm.note" placeholder="角色名或说明" />
            </label>
            <label>
              <span>排序</span>
              <input v-model.number="nominationForm.sortOrder" type="number" placeholder="排序" />
            </label>
            <label class="check-field">
              <input v-model="nominationForm.winner" type="checkbox" />
              <span>标记为获奖</span>
            </label>
            <button class="primary-btn" :disabled="saving || !nominationForm.awardCategoryId">添加提名</button>
          </form>
        </article>

        <article class="work-card">
          <div class="card-heading">
            <h3>提名名单</h3>
            <span>{{ nominations.length }} 条</span>
          </div>

          <div class="nomination-list">
            <div v-for="nomination in nominations" :key="nomination.id" class="nomination-row">
              <span class="winner-badge" :class="{ muted: !nomination.winner }">
                {{ nomination.winner ? '获奖' : '提名' }}
              </span>
              <div>
                <strong>{{ nominationTarget(nomination) }}</strong>
                <small>{{ nomination.note || nomination.awardCategoryName }}</small>
              </div>
              <em>#{{ formatSort(nomination.sortOrder) }}</em>
            </div>
            <div v-if="selectedCategoryId && !nominations.length" class="empty-state">这个奖项还没有提名</div>
          </div>
        </article>
      </section>
    </main>
  </section>
</template>

<style scoped>
.awards-panel {
  display: grid;
  gap: 16px;
  grid-template-columns: 360px minmax(0, 1fr);
}

.award-message {
  border-radius: 8px;
  font-size: 14px;
  grid-column: 1 / -1;
  padding: 12px 14px;
}

.award-message.success {
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  color: #166534;
}

.award-message.error {
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #991b1b;
}

.award-sidebar,
.award-workspace,
.work-card {
  min-width: 0;
}

.award-sidebar {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.sidebar-section,
.award-workspace,
.work-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.sidebar-section {
  padding: 16px;
}

.panel-heading,
.card-heading,
.edition-header {
  align-items: flex-start;
  display: flex;
  gap: 12px;
  justify-content: space-between;
}

.panel-heading span,
.edition-header span,
.card-heading span {
  color: #6b7280;
  display: block;
  font-size: 13px;
}

.panel-heading strong {
  color: #111827;
  display: block;
  font-size: 17px;
  margin-top: 4px;
}

.stack-form,
.select-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 14px;
}

.body-select-list {
  max-height: 280px;
  overflow: hidden;
}

.body-select-list.expanded {
  max-height: none;
  overflow: visible;
}

.split-fields,
.inline-form {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

input,
select {
  background: #fff;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  color: #111827;
  height: 38px;
  min-width: 0;
  outline: none;
  padding: 0 11px;
}

input:focus,
select:focus {
  border-color: #1677ff;
  box-shadow: 0 0 0 3px rgba(22, 119, 255, 0.12);
}

button {
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 700;
  min-height: 38px;
  padding: 0 14px;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.primary-btn {
  background: #1677ff;
  color: #fff;
}

.secondary-btn,
.mini-btn {
  background: #eef5ff;
  border: 1px solid #cfe0ff;
  color: #1677ff;
}

.mini-btn {
  min-height: 32px;
  padding: 0 10px;
}

.collapse-btn {
  background: #f8fbff;
  border: 1px solid #dbeafe;
  color: #1677ff;
  margin-top: 10px;
  width: 100%;
}

.form-actions {
  display: flex;
  gap: 8px;
}

.select-row,
.category-row {
  align-items: center;
  background: #fafafa;
  border: 1px solid #edf0f3;
  color: #111827;
  display: grid;
  gap: 10px;
  grid-template-columns: minmax(0, 1fr) auto auto;
  min-width: 0;
  text-align: left;
}

.select-row[role="button"] {
  cursor: pointer;
}

.select-row[role="button"]:focus-visible {
  outline: 2px solid #1677ff;
  outline-offset: 2px;
}

.category-row {
  grid-template-columns: auto minmax(0, 1fr) auto;
}

.select-row.active,
.category-row.active {
  background: #f0f6ff;
  border-color: #bcd8ff;
  color: #1677ff;
}

.select-row span:first-child,
.category-row strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.select-row em,
.nomination-row em {
  color: #6b7280;
  flex: 0 0 auto;
  font-style: normal;
  font-size: 13px;
}

.row-actions {
  display: inline-flex;
  flex: 0 0 auto;
  gap: 4px;
}

.row-actions button {
  background: #fff;
  border: 1px solid #dbe1ea;
  color: #4b5563;
  font-size: 13px;
  min-height: 28px;
  padding: 0 8px;
}

.body-menu-wrap {
  position: relative;
  display: inline-flex;
}

.body-menu-trigger {
  width: 28px;
  min-height: 28px;
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  padding: 0;
  border: 1px solid #dbe2ea;
  border-radius: 6px;
  background: #fff;
}

.body-menu-trigger:hover,
.body-menu-trigger.active {
  border-color: #9fc5ff;
  background: #eef5ff;
}

.body-menu-trigger span {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: #526171;
}

.body-action-menu {
  position: absolute;
  top: calc(100% + 5px);
  right: 0;
  z-index: 20;
  display: grid;
  width: 92px;
  padding: 5px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.14);
}

.body-action-menu button {
  min-height: 32px;
  padding: 0 9px;
  border: none;
  border-radius: 5px;
  background: transparent;
  color: #374151;
  font-size: 13px;
  text-align: left;
}

.body-action-menu button:hover:not(:disabled) {
  background: #f3f4f6;
}

.body-action-menu .danger-action {
  color: #dc2626;
}

.body-action-menu .danger-action:hover:not(:disabled) {
  background: #fef2f2;
}

.award-workspace {
  padding: 18px;
}

.edition-header {
  border-bottom: 1px solid #eef0f3;
  padding-bottom: 16px;
}

.edition-header h2,
.work-card h3 {
  margin: 0;
}

.edition-header h2 {
  color: #111827;
  font-size: 24px;
  margin-top: 4px;
}

.edition-header p {
  color: #6b7280;
  font-size: 14px;
  margin: 8px 0 0;
}

.detail-tabs {
  background: #f3f4f6;
  border-radius: 8px;
  display: inline-flex;
  gap: 4px;
  margin: 16px 0;
  padding: 4px;
}

.detail-tabs button {
  background: transparent;
  color: #4b5563;
  min-height: 34px;
}

.detail-tabs button.active {
  background: #fff;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.08);
  color: #1677ff;
}

.workspace-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: minmax(0, 1.4fr) minmax(300px, 0.8fr);
}

.workspace-grid.single {
  grid-template-columns: 1fr;
}

.work-card {
  padding: 16px;
}

.inline-form {
  grid-template-columns: minmax(180px, 1fr) 72px max-content;
  margin: 14px 0;
}

.inline-form .form-actions {
  flex-wrap: nowrap;
  white-space: nowrap;
}

.category-table,
.preview-list,
.nomination-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.category-row {
  min-height: 46px;
}

.sort-pill {
  background: #eef5ff;
  border-radius: 999px;
  color: #1677ff;
  flex: 0 0 auto;
  font-size: 13px;
  font-weight: 700;
  padding: 5px 9px;
}

.preview-row,
.nomination-row {
  align-items: center;
  background: #fafafa;
  border: 1px solid #edf0f3;
  border-radius: 8px;
  display: flex;
  gap: 12px;
  min-width: 0;
  padding: 12px;
}

.preview-row div,
.nomination-row div {
  min-width: 0;
}

.preview-row strong,
.nomination-row strong {
  color: #111827;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-row small,
.nomination-row small {
  color: #6b7280;
  display: block;
  margin-top: 4px;
}

.winner-badge {
  background: #eaf8ef;
  border: 1px solid #bfe8cc;
  border-radius: 999px;
  color: #16803c;
  flex: 0 0 auto;
  font-size: 13px;
  font-weight: 700;
  padding: 5px 9px;
}

.winner-badge.muted {
  background: #f3f4f6;
  border-color: #e5e7eb;
  color: #6b7280;
}

.nomination-form {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.nomination-form label {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.nomination-form label span {
  color: #6b7280;
  font-size: 13px;
}

.search-field {
  grid-column: 1 / -1;
}

.search-line {
  display: grid;
  gap: 8px;
  grid-template-columns: minmax(0, 1fr) auto;
}

.selected-chip {
  align-items: center;
  background: #f0f6ff;
  border: 1px solid #bcd8ff;
  border-radius: 8px;
  color: #1677ff;
  display: flex;
  font-size: 14px;
  gap: 8px;
  justify-content: space-between;
  padding: 8px 10px;
}

.selected-chip button {
  background: transparent;
  color: #1677ff;
  min-height: 24px;
  padding: 0 4px;
}

.result-list {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  max-height: 220px;
  overflow: auto;
}

.result-list button {
  background: #fff;
  border-bottom: 1px solid #edf0f3;
  border-radius: 0;
  color: #111827;
  display: block;
  min-height: 48px;
  padding: 8px 10px;
  text-align: left;
}

.result-list button:last-child {
  border-bottom: none;
}

.result-list button:hover {
  background: #f8fbff;
}

.result-list strong,
.result-list small {
  display: block;
}

.result-list small {
  color: #6b7280;
  font-size: 12px;
  margin-top: 3px;
}

.check-field {
  align-items: center;
  flex-direction: row !important;
  gap: 8px !important;
}

.check-field input {
  height: 16px;
  width: 16px;
}

.meta-list {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin: 14px 0 0;
}

.meta-list div {
  background: #fafafa;
  border: 1px solid #edf0f3;
  border-radius: 8px;
  padding: 12px;
}

.meta-list dt {
  color: #6b7280;
  font-size: 13px;
}

.meta-list dd {
  color: #111827;
  font-weight: 700;
  margin: 7px 0 0;
}

.empty-line,
.empty-state {
  color: #6b7280;
  font-size: 14px;
  padding: 12px;
  text-align: center;
}

@media (max-width: 1100px) {
  .awards-panel,
  .workspace-grid {
    grid-template-columns: 1fr;
  }

  .award-sidebar {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .award-sidebar,
  .split-fields,
  .inline-form,
  .nomination-form,
  .meta-list,
  .search-line {
    grid-template-columns: 1fr;
  }

  .edition-header,
  .card-heading,
  .form-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .detail-tabs {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    width: 100%;
  }
}
</style>
