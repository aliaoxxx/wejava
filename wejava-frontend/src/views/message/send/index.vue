<script setup lang="ts">
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { sendMessage } from '@/api/message'
import { listRoleOptions } from '@/api/role'
import { listUser } from '@/api/user'
import { deptTreeSelect } from '@/api/dept'

const formRef = ref<FormInstance>()
const sending = ref(false)

const form = reactive({
  msgType: 2,
  level: 0,
  title: '',
  content: '',
  link: '',
  targetType: 1,
  targetIds: [] as any[]
})

const roleOptions = ref<any[]>([])
const deptTree = ref<any[]>([])
const userOptions = ref<any[]>([])
const userLoading = ref(false)

const MSG_TYPES = [
  { label: '系统消息', value: 2 },
  { label: '公告', value: 1 },
  { label: '个人消息', value: 3 }
]
const LEVELS = [
  { label: '普通', value: 0 },
  { label: '重要', value: 1 },
  { label: '紧急', value: 2 }
]
const TARGETS = [
  { label: '全员', value: 1 },
  { label: '按角色', value: 2 },
  { label: '按部门', value: 3 },
  { label: '指定用户', value: 4 }
]

const rules: FormRules = {
  title: [{ required: true, message: '标题不能为空', trigger: 'blur' }]
}

async function loadOptions() {
  const [roles, depts] = await Promise.all([listRoleOptions(), deptTreeSelect()])
  roleOptions.value = roles.data ?? []
  deptTree.value = depts.data ?? []
}

async function searchUsers(keyword: string) {
  if (!keyword) {
    userOptions.value = []
    return
  }
  userLoading.value = true
  try {
    const res = await listUser({ pageNum: 1, pageSize: 20, username: keyword })
    userOptions.value = (res.data?.list ?? []).map((u: any) => ({
      label: `${u.nickname}（${u.username}）`,
      value: u.userId
    }))
  } finally {
    userLoading.value = false
  }
}

function onTargetChange() {
  form.targetIds = []
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (form.targetType !== 1 && !form.targetIds.length) {
    ElMessage.warning('请选择接收对象')
    return
  }
  sending.value = true
  try {
    await sendMessage({
      ...form,
      targetIds: form.targetType === 1 ? [] : form.targetIds
    })
    ElMessage.success('消息已发送')
    form.title = ''
    form.content = ''
    form.link = ''
    form.targetIds = []
  } finally {
    sending.value = false
  }
}

onMounted(loadOptions)
</script>

<template>
  <el-card shadow="never" class="send-card">
    <template #header>发送消息</template>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="消息类型">
        <el-radio-group v-model="form.msgType">
          <el-radio-button v-for="t in MSG_TYPES" :key="t.value" :value="t.value">{{ t.label }}</el-radio-button>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="级别">
        <el-radio-group v-model="form.level">
          <el-radio v-for="l in LEVELS" :key="l.value" :value="l.value">{{ l.label }}</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="接收范围">
        <el-radio-group v-model="form.targetType" @change="onTargetChange">
          <el-radio v-for="t in TARGETS" :key="t.value" :value="t.value">{{ t.label }}</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item v-if="form.targetType === 2" label="选择角色">
        <el-select v-model="form.targetIds" multiple filterable style="width: 100%">
          <el-option v-for="r in roleOptions" :key="r.roleId" :label="r.roleName" :value="r.roleId" />
        </el-select>
      </el-form-item>

      <el-form-item v-if="form.targetType === 3" label="选择部门">
        <el-tree-select
          v-model="form.targetIds"
          :data="deptTree"
          multiple
          check-strictly
          node-key="deptId"
          :props="{ label: 'deptName', children: 'children' }"
          show-checkbox
          style="width: 100%"
        />
        <div class="tip">选择父部门会自动包含其下属部门的成员</div>
      </el-form-item>

      <el-form-item v-if="form.targetType === 4" label="选择用户">
        <el-select
          v-model="form.targetIds"
          multiple
          filterable
          remote
          :remote-method="searchUsers"
          :loading="userLoading"
          placeholder="输入用户名搜索"
          style="width: 100%"
        >
          <el-option v-for="u in userOptions" :key="u.value" :label="u.label" :value="u.value" />
        </el-select>
      </el-form-item>

      <el-form-item label="标题" prop="title">
        <el-input v-model="form.title" maxlength="100" show-word-limit />
      </el-form-item>

      <el-form-item label="内容">
        <el-input v-model="form.content" type="textarea" :rows="5" maxlength="5000" show-word-limit />
      </el-form-item>

      <el-form-item label="跳转地址">
        <el-input v-model="form.link" placeholder="可选，如 /system/notice" />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="sending" @click="submit">发送</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<style scoped>
.send-card {
  max-width: 820px;
}
.tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
