<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { request } from '../utils/request'

const router = useRouter()

const username = ref('')
const password = ref('')
const email = ref('')
const confirmPassword = ref('')
const loading = ref(false)
const errorMessage = ref('')
const emailCode = ref('')
const sendingCode = ref(false)
const codeMessage = ref('')
const countdown = ref(0)
let countdownTimer: number | undefined

function isValidEmail(email: string) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
}

async function handleRegister() {
  errorMessage.value = ''

  if (!username.value.trim()) {
    errorMessage.value = '请输入用户名'
    return
  }

  if (!email.value.trim()) {
    errorMessage.value = '请输入邮箱'
    return
  }

  if (!isValidEmail(email.value.trim())) {
    errorMessage.value = '请输入正确的邮箱格式'
    return
  }

  if (!emailCode.value.trim()) {
    errorMessage.value = '请输入邮箱验证码'
    return
  }

  if (!password.value) {
    errorMessage.value = '请输入密码'
    return
  }

  if (password.value !== confirmPassword.value) {
    errorMessage.value = '两次输入的密码不一致'
    return
  }

  loading.value = true

  try {
    const response = await request('/auth/register', {
      method: 'POST',
      body: JSON.stringify({
        username: username.value.trim(),
        email: email.value.trim(),
        password: password.value,
        emailCode: emailCode.value.trim()
      })
    })

    if (!response.ok) {
        let message = '注册失败，请稍后重试'

        try {
            const errorData = await response.json()
            if (typeof errorData === 'string') {
                message = errorData
            } else if (errorData.errors) {
                message = errorData.errors
            } else if (errorData.message) {
                message = errorData.message
            }

        } catch {
            // 后端没有返回 JSON 时，使用默认提示
        }

        if (response.status === 409) {
            message = '用户名或邮箱已存在'
        }

        errorMessage.value = message
        return
    }

    await router.push({
        path: '/login',
        query: {
            registered: 'true'
        }
    })
  } catch (error) {
    console.error(error)
    errorMessage.value = '注册失败'
  } finally {
    loading.value = false
  }
}

async function handleSendEmailCode() {
    errorMessage.value = ''

    if (!email.value.trim()) {
        errorMessage.value = '请输入邮箱'
        return
    }

    if (!isValidEmail(email.value.trim())) {
        errorMessage.value = '请输入正确的邮箱格式'
        return
    }

    sendingCode.value = true

    try {
        const response = await request('/auth/email-code', {
        method: 'POST',
        body: JSON.stringify({
            email: email.value.trim()
        })
        })

        if (!response.ok) {
            let message = '验证码发送失败，请稍后重试'

            try {
                const errorData = await response.json()
                if (typeof errorData === 'string') {
                message = errorData
                } else if (errorData.message) {
                message = errorData.message
                } else if (errorData.error) {
                message = errorData.error
                }
            } catch {
                // 后端没返回 JSON 就用默认提示
        }

errorMessage.value = message
return
}

        codeMessage.value = '验证码已发送，请查看邮箱'
        countdown.value = 60

        countdownTimer = window.setInterval(() => {
            countdown.value--
            if (countdown.value <= 0 && countdownTimer) {
                clearInterval(countdownTimer)
                countdownTimer = undefined
            }
        }, 1000)
    } catch (error) {
        console.error(error)
        errorMessage.value = '验证码发送失败'
    } finally {
        sendingCode.value = false
    }
}

function goToLogin() {
  router.push('/login')
}

</script>

<template>
  <div class="register-page">
    <div class="register-card">
      <h2>注册新用户</h2>

      <form @submit.prevent="handleRegister">
        <div class="form-item">
          <label>用户名</label>
          <input v-model="username" type="text" placeholder="请输入用户名" />
        </div>
        <div class="form-item">
            <label>邮箱</label>
            <input v-model="email" type="email" placeholder="请输入邮箱" />
        </div>
        <div class="form-item">
        <label>邮箱验证码</label>
        <div class="code-row">
            <input
                v-model="emailCode"
                type="text"
                placeholder="请输入邮箱验证码"
            />
            <button
                type="button"
                class="send-code-btn"
                :disabled="sendingCode || countdown > 0"
                @click="handleSendEmailCode"
            >
            {{ sendingCode ? '发送中...' : countdown > 0 ? `${countdown}s后重试` : '发送验证码' }}
            </button>
        </div>
        <div v-if="codeMessage" class="code-message">
            {{ codeMessage }}
        </div>
        </div>
        <div class="form-item">
          <label>密码</label>
          <input v-model="password" type="password" placeholder="请输入密码" />
        </div>

        <div class="form-item">
          <label>确认密码</label>
          <input v-model="confirmPassword" type="password" placeholder="请再次输入密码" />
        </div>

        <div v-if="errorMessage" class="error-message">
          {{ errorMessage }}
        </div>

        <button type="submit" :disabled="loading">
          {{ loading ? '注册中...' : '注册' }}
        </button>

        <p class="login-tip">
          已有账号？
          <span @click="goToLogin">去登录</span>
        </p>
      </form>
    </div>
  </div>
</template>

<style scoped>
.register-page {
  min-height: calc(100vh - 120px);
  display: flex;
  align-items: center;
  justify-content: center;
}

.register-card {
  width: min(360px, 100%);
  background: #fff;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08);
}

.register-card h2 {
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

.login-tip {
  margin-top: 16px;
  text-align: center;
  font-size: 14px;
  color: #666;
}

.login-tip span {
  color: #1677ff;
  cursor: pointer;
  margin-left: 4px;
}

.login-tip span:hover {
  text-decoration: underline;
}

.code-row {
  display: flex;
  gap: 8px;
}

.code-row input {
  flex: 1;
  min-width: 0;
}

.send-code-btn {
  width: 110px;
  height: 40px;
  border: none;
  border-radius: 6px;
  background: #52c41a;
  color: #fff;
  cursor: pointer;
  font-size: 14px;
}

.send-code-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.code-message {
  margin-top: -8px;
  margin-bottom: 12px;
  font-size: 13px;
  color: #389e0d;
}

@media (max-width: 480px) {
  .register-page {
    min-height: auto;
    align-items: flex-start;
    padding: 18px 0;
  }

  .register-card {
    padding: 20px;
    border-radius: 10px;
  }

  .code-row {
    gap: 6px;
  }

  .send-code-btn {
    width: 96px;
    padding: 0 8px;
    font-size: 13px;
    white-space: nowrap;
  }
}
</style>
