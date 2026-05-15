<template>
  <div class="app-container">
    <header class="top-nav">
      <div class="container">
        <div class="logo" @click="goHome">海盗电台</div>

        <nav class="nav-menu">
          <router-link to="/movies">电影</router-link>
          <router-link to="/persons">人物</router-link>
          <router-link v-if="currentUser?.isAdmin" to="/admin/tmdb-import">TMDB导入</router-link>
        </nav>

        <div class="user-box">
          <button v-if="!currentUser" class="login-btn" @click="goToLogin">
            登录
          </button>

          <div v-else class="user-info">
            <span class="user-name">{{ displayName }}</span>
            <button class="logout-btn" @click="logout">退出</button>
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
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { request } from '../utils/request'
import { isLoggedIn, removeToken } from '../utils/auth'

const router = useRouter()

const currentUser = ref<{ username: string; nickname?: string | null; isAdmin?: boolean } | null>(null)

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

async function logout() {
  removeToken()
  currentUser.value = null
  await router.push('/movies')
  window.location.reload()
}

onMounted(() => {
  loadCurrentUser()
})
</script>

<style scoped>
.app-container {
  min-height: 100vh;
  background: #f5f5f5;
}

.top-nav {
  height: 64px;
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
}

.top-nav .container {
  height: 100%;
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
}

.login-btn,
.logout-btn {
  height: 36px;
  padding: 0 14px;
  border: none;
  border-radius: 6px;
  background: #1677ff;
  color: #fff;
  cursor: pointer;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-name {
  font-size: 15px;
  color: #333;
  font-weight: 500;
}

.main-content {
  padding: 24px 0;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
}
</style>
