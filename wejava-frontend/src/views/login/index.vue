<script setup lang="ts">
import logoMark from '@/assets/logo-mark.svg'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { getCaptcha, type CaptchaVO } from '@/api/auth'
import { useUserStore } from '@/store/modules/user'
import { useRoute, useRouter } from 'vue-router'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const captcha = ref<CaptchaVO>({ captchaKey: '', captchaImage: '' })
const form = reactive({ username: 'admin', password: 'admin123', captchaCode: '' })

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

async function refreshCaptcha() {
  try {
    const { data } = await getCaptcha()
    captcha.value = data
  } catch {
    // 忽略：登录失败后刷新验证码属于兜底行为
  }
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.login({
      username: form.username,
      password: form.password,
      captchaKey: captcha.value.captchaKey,
      captchaCode: form.captchaCode
    })
    ElMessage.success('登录成功')
    const redirect = (route.query.redirect as string) || '/'
    await router.push(redirect)
  } catch {
    form.captchaCode = ''
    await refreshCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(refreshCaptcha)
</script>

<template>
  <div class="login-page">
    <el-card class="login-card">
      <template #header>
        <div class="login-brand">
          <img class="login-logo" :src="logoMark" alt="WeJavaAdmin" />
          <span class="login-title">WeJava 管理后台</span>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="0" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" size="large" clearable />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            size="large"
            show-password
          />
        </el-form-item>
        <el-form-item prop="captchaCode">
          <div class="captcha-row">
            <el-input v-model="form.captchaCode" placeholder="验证码" size="large" />
            <img
              v-if="captcha.captchaImage"
              :src="captcha.captchaImage"
              class="captcha-img"
              title="点击刷新"
              @click="refreshCaptcha"
            />
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" class="login-btn" @click="handleLogin">
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-tip">默认账号 admin / admin123</div>
    </el-card>
  </div>
</template>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f2d3d 0%, #304156 100%);
}
.login-card {
  width: 380px;
}
.login-brand {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}
.login-logo {
  width: 28px;
  height: 28px;
  border-radius: 8px;
}
.login-title {
  font-size: 18px;
  font-weight: 600;
}
.captcha-row {
  display: flex;
  gap: 10px;
  width: 100%;
}
.captcha-img {
  height: 40px;
  width: 120px;
  cursor: pointer;
  border-radius: 4px;
  border: 1px solid #dcdfe6;
}
.login-btn {
  width: 100%;
}
.login-tip {
  text-align: center;
  color: #909399;
  font-size: 12px;
  margin-top: 8px;
}
</style>
