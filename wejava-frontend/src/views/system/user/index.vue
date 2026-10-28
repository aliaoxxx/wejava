<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import CrudPage from '@/components/Crud/CrudPage.vue'
import { useCrud } from '@/hooks/useCrud'
import { resetUserPassword } from '@/api/user'
import { userSchema } from './schema'

const crud = useCrud(userSchema)

async function handleResetPwd(row: any) {
  const { value } = await ElMessageBox.prompt('请输入新密码（6~20 位）', '重置密码', {
    inputPattern: /^.{6,20}$/,
    inputErrorMessage: '密码长度需为 6~20 位'
  })
  await resetUserPassword({ userId: row.userId, password: value })
  ElMessage.success('密码已重置，该用户已被强制下线')
}
</script>

<template>
  <CrudPage :crud="crud">
    <template #row-actions="{ row }">
      <el-button
        v-hasPerm="'system:user:resetPwd'"
        link
        type="warning"
        @click="handleResetPwd(row)"
      >
        重置密码
      </el-button>
    </template>
  </CrudPage>
</template>
