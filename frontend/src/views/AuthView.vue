<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { authApi } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import BrandMark from '@/components/BrandMark.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isRegister = computed(() => route.name === 'register')
const loading = ref(false)
const submitError = ref('')

const form = reactive({
  username: '',
  identifier: '',
  email: '',
  password: '',
})

const errors = reactive({
  username: '',
  identifier: '',
  password: '',
})

function validate(): boolean {
  errors.username = ''
  errors.identifier = ''
  errors.password = ''

  let ok = true
  if (isRegister.value) {
    const name = form.username.trim()
    if (!name) {
      errors.username = '请输入用户名'
      ok = false
    } else if (name.length < 3 || name.length > 32) {
      errors.username = '用户名需为 3-32 个字符'
      ok = false
    }
  } else if (!form.identifier.trim()) {
    errors.identifier = '请输入邮箱或用户名'
    ok = false
  }

  if (!form.password) {
    errors.password = '请输入密码'
    ok = false
  } else if (form.password.length < 6 || form.password.length > 64) {
    errors.password = '密码需为 6-64 个字符'
    ok = false
  }
  return ok
}

// 被守卫拦下的页面会把原路径挂在 redirect 上。只接受站内绝对路径，
// 挡掉 //evil.com 与 /\evil.com 两类协议相对写法（WHATWG URL 会把后者
// 归一成 //evil.com），免得登录页变成开放重定向跳板。
function resolveTarget(): string {
  const raw = route.query.redirect
  const target = Array.isArray(raw) ? raw[0] : raw
  if (
    typeof target === 'string' &&
    target.startsWith('/') &&
    !target.startsWith('//') &&
    !target.startsWith('/\\')
  ) {
    return target
  }
  return '/'
}

async function submit() {
  if (loading.value || !validate()) return
  loading.value = true
  submitError.value = ''
  try {
    const auth = isRegister.value
      ? await authApi.register({
          username: form.username.trim(),
          password: form.password,
          email: form.email.trim() || undefined,
        })
      : await authApi.login({ username: form.identifier.trim(), password: form.password })
    userStore.setAuth(auth)
    router.push(resolveTarget())
  } catch (e) {
    submitError.value = e instanceof Error ? e.message : '请求失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth">
    <div class="auth-brand">
      <div class="auth-brand-inner">
        <BrandMark :size="44" variant="light" />
        <h1 class="auth-brand-name">KECE 课测</h1>
        <p class="auth-brand-tagline">多维度编程与技能评测平台</p>
        <ul class="auth-brand-points">
          <li>算法 OJ · 数据库 OJ</li>
          <li>前端 AI 智能评测</li>
          <li>标准客观题即时判分</li>
        </ul>
      </div>
    </div>

    <div class="auth-panel">
      <div class="auth-card card">
        <h2 class="auth-title">{{ isRegister ? '创建账号' : '欢迎登录' }}</h2>

        <div v-if="isRegister" class="field">
          <span class="field-prefix">@</span>
          <input
            v-model="form.username"
            class="field-input"
            type="text"
            placeholder="请输入用户名"
            autocomplete="username"
          />
        </div>
        <p v-if="isRegister" class="field-error">{{ errors.username }}</p>

        <div v-if="!isRegister" class="field">
          <span class="field-prefix">@</span>
          <input
            v-model="form.identifier"
            class="field-input"
            type="text"
            placeholder="请输入邮箱/用户名"
            autocomplete="username"
          />
        </div>
        <p v-if="!isRegister" class="field-error">{{ errors.identifier }}</p>

        <div v-if="isRegister" class="field">
          <span class="field-prefix">@</span>
          <input
            v-model="form.email"
            class="field-input"
            type="email"
            placeholder="邮箱（选填）"
            autocomplete="email"
          />
        </div>

        <div class="field">
          <span class="field-prefix">#</span>
          <input
            v-model="form.password"
            class="field-input"
            type="password"
            placeholder="请输入密码"
            autocomplete="current-password"
          />
        </div>
        <p class="field-error">{{ errors.password }}</p>

        <p v-if="submitError" class="submit-error">{{ submitError }}</p>

        <button class="btn btn-primary btn-block" :disabled="loading" @click="submit">
          {{ loading ? '请稍候…' : isRegister ? '注册' : '登录' }}
        </button>

        <div class="auth-links">
          <template v-if="!isRegister">
            <span class="auth-link muted">忘记密码？</span>
            <RouterLink to="/register" class="auth-link">注册帐号</RouterLink>
          </template>
          <template v-else>
            <span class="muted">已有帐号？</span>
            <RouterLink to="/login" class="auth-link">去登录</RouterLink>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1fr 1fr;
}
.auth-brand {
  background: var(--button);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
}
.auth-brand-inner {
  max-width: 380px;
}
.auth-brand-name {
  font-size: 32px;
  margin: 20px 0 8px;
  color: #fff;
}
.auth-brand-tagline {
  color: rgba(255, 255, 255, 0.7);
  margin: 0 0 28px;
}
.auth-brand-points {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
  color: rgba(255, 255, 255, 0.85);
  font-size: 15px;
}
.auth-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
}
.auth-card {
  width: 100%;
  max-width: 380px;
  padding: 40px 36px;
}
.auth-title {
  margin: 0 0 24px;
  font-size: 24px;
}
.auth-card .field {
  margin-bottom: 4px;
}
.auth-card .field-error {
  margin-bottom: 10px;
}
.submit-error {
  margin: 0 0 16px;
}
.auth-links {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  font-size: 14px;
}
.auth-link {
  color: var(--ink-secondary);
  transition: color 0.15s ease;
}
.auth-link:hover {
  color: var(--ink);
}
@media (max-width: 860px) {
  .auth {
    grid-template-columns: 1fr;
  }
  .auth-brand {
    display: none;
  }
  .auth-panel {
    min-height: 100vh;
    align-items: flex-start;
    padding-top: 64px;
  }
}
</style>
