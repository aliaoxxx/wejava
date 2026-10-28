<script setup lang="ts">
import type { FormField, Option } from '@/types/crud'

const props = defineProps<{
  field: FormField
  model: Record<string, any>
  mode: 'create' | 'edit'
}>()

const disabled = computed(() => props.field.disabled?.(props.model, props.mode) ?? false)
const options = ref<Option[]>(props.field.options ?? [])

onMounted(async () => {
  if (props.field.loadOptions) {
    options.value = await props.field.loadOptions()
  }
})
</script>

<template>
  <el-select
    v-if="field.type === 'select'"
    v-model="model[field.prop]"
    :placeholder="field.placeholder || '请选择'"
    :disabled="disabled"
    :multiple="field.multiple"
    clearable
    style="width: 100%"
  >
    <el-option v-for="opt in options" :key="opt.value" :label="opt.label" :value="opt.value" />
  </el-select>

  <el-tree-select
    v-else-if="field.type === 'tree-select'"
    v-model="model[field.prop]"
    :data="options"
    node-key="value"
    :props="{ label: 'label', children: 'children' }"
    :disabled="disabled"
    check-strictly
    default-expand-all
    :placeholder="field.placeholder || '请选择'"
    style="width: 100%"
  />

  <el-radio-group v-else-if="field.type === 'radio'" v-model="model[field.prop]" :disabled="disabled">
    <el-radio v-for="opt in options" :key="opt.value" :value="opt.value">{{ opt.label }}</el-radio>
  </el-radio-group>

  <el-switch
    v-else-if="field.type === 'switch'"
    v-model="model[field.prop]"
    :disabled="disabled"
    :active-value="0"
    :inactive-value="1"
  />

  <el-input-number
    v-else-if="field.type === 'number'"
    v-model="model[field.prop]"
    :disabled="disabled"
    :min="0"
    style="width: 100%"
  />

  <el-date-picker
    v-else-if="field.type === 'date'"
    v-model="model[field.prop]"
    type="date"
    value-format="YYYY-MM-DD"
    :disabled="disabled"
    style="width: 100%"
  />

  <el-date-picker
    v-else-if="field.type === 'datetime'"
    v-model="model[field.prop]"
    type="datetime"
    value-format="YYYY-MM-DD HH:mm:ss"
    :disabled="disabled"
    style="width: 100%"
  />

  <el-input
    v-else
    v-model="model[field.prop]"
    :type="field.type === 'password' ? 'password' : field.type === 'textarea' ? 'textarea' : 'text'"
    :rows="field.type === 'textarea' ? 4 : undefined"
    :placeholder="field.placeholder || '请输入'"
    :disabled="disabled"
    show-password
  />
</template>
