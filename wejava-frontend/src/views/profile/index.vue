<script setup lang="ts">
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useRouter } from 'vue-router'
import { changePassword, getProfile, updateProfile } from '@/api/profile'
import { clearToken } from '@/utils/auth'

const router = useRouter()

const profile = reactive({
  userId: '',
  username: '',
  nickname: '',
  phone: '',
  email: '',
  deptName: ''
})
const loading = ref(false)
const savingInfo = ref(false)
const savingPwd = ref(false)

const infoRef = ref<FormInstance>()
const pwdRef = ref<FormInstance>()

const infoForm = reactive({ nickname: '', phone: '', email: '' })
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const infoRules: FormRules = {
  nickname: [{ required: true, message: '昵称不能为空', trigger: 'blur' }]
}

const pwdRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '长度需在 6~32 之间', trigger: 'blur' },
    {
      pattern: /^(?=.*[A-Za-z])(?=.*\d).+$/,
      message: '必须同时包含字母和数字',
      trigger: 'blur'
    }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: string, callback: any) => {
        if (value !== pwdForm.newPassword) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

async function fetchProfile() {
  loading.value = true
  try {
    const res = await getProfile()
    Object.assign(profile, res.data ?? {})
    infoForm.nickname = profile.nickname
    infoForm.phone = profile.phone
    infoForm.email = profile.email
  } finally {
    loading.value = false
  }
}

async function saveInfo() {
  const valid = await infoRef.value?.validate().catch(() => false)
  if (!valid) return
  savingInfo.value = true
  try {
    await updateProfile(infoForm)
    ElMessage.success('资料已更新')
    await fetchProfile()
  } finally {
    savingInfo.value = false
  }
}

async function savePassword() {
  const valid = await pwdRef.value?.validate().catch(() => false)
  if (!valid) return
  savingPwd.value = true
  try {
    await changePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    ElMessage.success('密码已修改，请重新登录')
    clearToken()
    await router.push('/login')
  } finally {
    savingPwd.value = false
  }
}

onMounted(fetchProfile)
</script>

<template>
  <el-row v-loading="loading" :gutter="16">
    <el-col :span="12">
      <el-card shadow="never">
        <template #header>基本资料</template>
        <el-form ref="infoRef" :model="infoForm" :rules="infoRules" label-width="90px">
          <el-form-item label="用户名">
            <el-input :model-value="profile.username" disabled />
          </el-form-item>
          <el-form-item label="所属部门">
            <el-input :model-value="profile.deptName" disabled />
          </el-form-item>
          <el-form-item label="昵称" prop="nickname">
            <el-input v-model="infoForm.nickname" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="infoForm.phone" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="infoForm.email" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="savingInfo" @click="saveInfo">保存</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </el-col>

    <el-col :span="12">
      <el-card shadow="never">
        <template #header>修改密码</template>
        <el-form ref="pwdRef" :model="pwdForm" :rules="pwdRules" label-width="90px">
          <el-form-item label="原密码" prop="oldPassword">
            <el-input v-model="pwdForm.oldPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="pwdForm.newPassword" type="password" show-password
              placeholder="6~32 位，含字母和数字" />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="savingPwd" @click="savePassword">修改密码</el-button>
            <span class="tip">修改成功后所有会话将失效，需要重新登录</span>
          </el-form-item>
        </el-form>
      </el-card>
    </el-col>
  </el-row>
</template>

<style scoped>
.tip {
  margin-left: 12px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
