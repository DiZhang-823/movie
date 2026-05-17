  <script setup lang="ts">
  import { ref, onMounted } from 'vue'
  import { useRouter, useRoute } from 'vue-router'
  import { request } from '../utils/request'
  import { setToken } from '../utils/auth'

  const router = useRouter()
  const route = useRoute()

  const username = ref('')
  const password = ref('')
  const loading = ref(false)
  const errorMessage = ref('')
  const successMessage = ref('')

  async function handleLogin() {
    loading.value = true
    errorMessage.value = ''

    try {
      const response = await request('/auth/login', {
        method: 'POST',
        body: JSON.stringify({
          username: username.value,
          password: password.value
        })
      })

      if (!response.ok) {
        if (response.status === 401) {
          errorMessage.value = '用户名或密码错误'
        } else {
          errorMessage.value = '登录失败，请稍后重试'
        }
        return
      }


      const data = await response.json()
      setToken(data.token)

      const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
      await router.push(redirect)
      window.location.reload()
    } catch (error) {
      console.error(error)
      errorMessage.value = '登录失败'
    } finally {
      loading.value = false
    }
  }

  function goToRegister() {
    router.push('/register')
  }
  onMounted(() => {
  if (route.query.registered === 'true') {
    successMessage.value = '注册成功，请登录'
  }
})
  </script>

  <template>
    <div class="login-page">
      <div class="login-card">
        <h2>登录</h2>
        <form @submit.prevent="handleLogin">
          <div class="form-item">
            <label>用户名</label>
            <input v-model="username" type="text" placeholder="请输入用户名" />
          </div>

          <div class="form-item">
            <label>密码</label>
            <input v-model="password" type="password" placeholder="请输入密码" />
          </div>
          <div v-if="successMessage" class="success-message">
            {{ successMessage }}
          </div>
          <div v-if="errorMessage" class="error-message">
            {{ errorMessage }}
          </div>

          <button type="submit" :disabled="loading">
            {{ loading ? '登录中...' : '登录' }}
          </button>
          <p class="register-tip">
            还没有账号？
            <span @click="goToRegister">注册新用户</span>
          </p>
        </form>
      </div>
    </div>
  </template>

  <style scoped>
  .login-page {
    min-height: calc(100vh - 120px);
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .login-card {
    width: min(360px, 100%);
    background: #fff;
    padding: 24px;
    border-radius: 12px;
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08);
  }

  .login-card h2 {
    margin: 0 0 20px;
  }

  .form-item {
    display: flex;
    flex-direction: column;
    gap: 8px;
    margin-bottom: 16px;
  }

  .form-item input {
    height: 40px;
    padding: 0 12px;
    border: 1px solid #dcdfe6;
    border-radius: 6px;
    outline: none;
  }

  .form-item input:focus {
    border-color: #1677ff;
  }

  button {
    width: 100%;
    height: 40px;
    border: none;
    border-radius: 6px;
    background: #1677ff;
    color: #fff;
    cursor: pointer;
  }

  button:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }

  .error-message {
    margin-bottom: 12px;
    padding: 10px 12px;
    border-radius: 6px;
    background: #fff2f0;
    color: #d93025;
    font-size: 14px;
    border: 1px solid #ffccc7;
  }

  .register-tip {
  margin-top: 16px;
  text-align: center;
  font-size: 14px;
  color: #666;
}

.register-tip span {
  color: #1677ff;
  cursor: pointer;
  margin-left: 4px;
}

.register-tip span:hover {
  text-decoration: underline;
}

@media (max-width: 480px) {
  .login-page {
    min-height: auto;
    align-items: flex-start;
    padding: 24px 0;
  }

  .login-card {
    padding: 20px;
    border-radius: 10px;
  }
}
  </style>
