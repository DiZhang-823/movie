<template>
  <div class="app-container">
    <header class="top-nav">
      <div class="container">
        <div class="logo" @click="goHome">海盗电台</div>

        <nav class="nav-menu">
          <router-link to="/movies">电影</router-link>
          <router-link to="/tv-seasons">剧集</router-link>
          <router-link v-if="currentUser?.isAdmin" to="/admin/tmdb-import">后台管理</router-link>
        </nav>

        <div class="user-box">
          <button v-if="!currentUser" class="login-btn" @click="goToLogin">
            登录
          </button>

          <div v-else ref="userMenuRef" class="user-menu">
            <button class="user-trigger" :class="{ active: userMenuOpen }" @click="toggleUserMenu">
              <span class="user-name">{{ displayName }}</span>
              <span class="user-arrow" :class="{ open: userMenuOpen }">▾</span>
            </button>
            <div v-if="userMenuOpen" class="user-dropdown">
              <button class="dropdown-item" @click="logout">退出</button>
            </div>
          </div>
        </div>
      </div>
    </header>

    <main class="main-content">
      <div class="container">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { request } from '../utils/request'
import { isLoggedIn, removeToken } from '../utils/auth'

const router = useRouter()

const currentUser = ref<{ username: string; nickname?: string | null; isAdmin?: boolean } | null>(null)
const userMenuOpen = ref(false)
const userMenuRef = ref<HTMLElement | null>(null)

const displayName = computed(() => {
  if (!currentUser.value) {
    return ''
  }
  return currentUser.value.nickname || currentUser.value.username
})

async function loadCurrentUser() {
  if (!isLoggedIn()) {
    currentUser.value = null
    return
  }

  try {
    const response = await request('/users/me')

    if (!response.ok) {
      removeToken()
      currentUser.value = null
      return
    }

    const data = await response.json()
    currentUser.value = {
      username: data.username,
      nickname: data.nickname ?? null,
      isAdmin: Boolean(data.isAdmin)
    }
  } catch (error) {
    console.error('Failed to load current user:', error)
    currentUser.value = null
  }
}

function goHome() {
  router.push('/movies')
}

function goToLogin() {
  router.push('/login')
}

function toggleUserMenu() {
  userMenuOpen.value = !userMenuOpen.value
}

function closeUserMenu() {
  userMenuOpen.value = false
}

function handleDocumentClick(event: MouseEvent) {
  if (!userMenuOpen.value) {
    return
  }
  const target = event.target
  if (target instanceof Node && userMenuRef.value?.contains(target)) {
    return
  }
  closeUserMenu()
}

async function logout() {
  closeUserMenu()
  removeToken()
  currentUser.value = null
  await router.push('/movies')
  window.location.reload()
}

onMounted(() => {
  loadCurrentUser()
  document.addEventListener('click', handleDocumentClick)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleDocumentClick)
})
</script>

<style scoped>
.app-container {
  min-height: 100vh;
  background: #f5f5f5;
}

.top-nav {
  min-height: 64px;
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
}

.top-nav .container {
  min-height: 64px;
  display: flex;
  align-items: center;
  gap: 24px;
}

.logo {
  font-size: 25px;
  font-weight: 700;
  color: #1677ff;
  flex-shrink: 0;
  cursor: pointer;
  white-space: nowrap;
}

.nav-menu {
  display: flex;
  gap: 20px;
  flex-shrink: 0;
}

.nav-menu a {
  text-decoration: none;
  color: #333;
  font-size: 16px;
  font-weight: 500;
  white-space: nowrap;
}

.nav-menu a:hover {
  color: #1677ff;
}

.nav-menu a.router-link-active {
  color: #1677ff;
}

.user-box {
  margin-left: auto;
  display: flex;
  align-items: center;
  min-width: 0;
}

.login-btn {
  height: 36px;
  padding: 0 14px;
  border: none;
  border-radius: 6px;
  background: #1677ff;
  color: #fff;
  cursor: pointer;
  white-space: nowrap;
}

.user-menu {
  position: relative;
  display: flex;
  align-items: center;
}

.user-trigger {
  max-width: 180px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0 8px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #333;
  cursor: pointer;
}

.user-trigger:hover,
.user-trigger.active {
  background: #f3f4f6;
  color: #1677ff;
}

.user-name {
  font-size: 15px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-arrow {
  flex: 0 0 auto;
  color: #6b7280;
  font-size: 12px;
  line-height: 1;
  transition: transform 0.16s ease;
}

.user-arrow.open {
  transform: rotate(180deg);
}

.user-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  z-index: 20;
  width: 120px;
  padding: 6px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.12);
}

.dropdown-item {
  width: 100%;
  height: 34px;
  display: flex;
  align-items: center;
  padding: 0 10px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #333;
  cursor: pointer;
  font-size: 14px;
  text-align: left;
  white-space: nowrap;
}

.dropdown-item:hover {
  background: #f3f4f6;
  color: #1677ff;
}

.main-content {
  padding: 24px 0;
}

.container {
  width: min(1200px, calc(100% - 32px));
  max-width: 1200px;
  margin: 0 auto;
}

@media (max-width: 720px) {
  .top-nav .container {
    min-height: 0;
    flex-wrap: wrap;
    gap: 10px 12px;
    padding: 12px 0;
  }

  .logo {
    font-size: 20px;
  }

  .nav-menu {
    order: 3;
    width: 100%;
    gap: 8px;
    overflow-x: auto;
    padding-bottom: 2px;
    scrollbar-width: none;
  }

  .nav-menu::-webkit-scrollbar {
    display: none;
  }

  .nav-menu a {
    display: inline-flex;
    align-items: center;
    min-height: 34px;
    padding: 0 12px;
    border-radius: 999px;
    background: #f3f4f6;
    font-size: 14px;
  }

  .nav-menu a.router-link-active {
    background: #e8f2ff;
  }

  .user-trigger {
    max-width: 118px;
    height: 34px;
    padding: 0 6px;
  }

  .user-name {
    font-size: 14px;
  }

  .login-btn {
    height: 34px;
    padding: 0 12px;
  }

  .user-dropdown {
    top: calc(100% + 6px);
    width: 112px;
  }

  .main-content {
    padding: 14px 0 24px;
  }

  .container {
    width: min(100% - 24px, 1200px);
  }
}
</style>
