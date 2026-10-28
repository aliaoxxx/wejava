<script setup lang="ts">
import { ElMessage } from 'element-plus'
import FieldItem from './FieldItem.vue'
import type { CrudSchema, FormField } from '@/types/crud'

const props = defineProps<{
  schema: CrudSchema
  dialog: { visible: boolean; mode: 'create' | 'edit'; title: string; model: Record<string, any> }
}>()

const innerFormRef = ref()

async function validate(): Promise<boolean> {
  return (await innerFormRef.value?.validate().catch(() => false)) ?? false
}

defineExpose({ validate })

const emit = defineEmits<{ submit: [] }>()

function visibleFields(): FormField[] {
  return (props.schema.form ?? []).filter((field) => {
    if (field.only && field.only !== props.dialog.mode) return false
    if (field.visible && !field.visible(props.dialog.model)) return false
    return true
  })
}

function rulesOf(field: FormField) {
  const rules = [...(field.rules ?? [])]
  if (field.required) {
    rules.unshift({ required: true, message: `${field.label}不能为空`, trigger: 'blur' })
  }
  return rules
}
</script>

<template>
  <el-dialog
    v-model="dialog.visible"
    :title="`${dialog.title}${schema.key}`"
    :width="schema.dialog?.width || '640px'"
    destroy-on-close
    append-to-body
  >
    <el-form
      ref="innerFormRef"
      :model="dialog.model"
      :label-width="schema.dialog?.labelWidth || '96px'"
      @submit.prevent
    >
      <el-row :gutter="16">
        <el-col v-for="field in visibleFields()" :key="field.prop" :span="field.span || 24">
          <el-form-item :label="field.label" :prop="field.prop" :rules="rulesOf(field)">
            <FieldItem :field="field" :model="dialog.model" :mode="dialog.mode" />
            <div v-if="field.tip" class="field-tip">{{ field.tip }}</div>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <template #footer>
      <el-button @click="dialog.visible = false">取消</el-button>
      <el-button type="primary" @click="emit('submit')">确定</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.field-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}
</style>
