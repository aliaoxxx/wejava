import { ElMessage, ElMessageBox } from 'element-plus'
import type { CrudMode, CrudSchema } from '@/types/crud'

export function useCrud(schema: CrudSchema) {
  const state = reactive({
    loading: false,
    list: [] as any[],
    total: 0,
    query: {
      pageNum: 1,
      pageSize: schema.pagination?.pageSize ?? 10
    },
    searchModel: {} as Record<string, any>,
    selected: [] as any[],
    dialog: {
      visible: false,
      mode: 'create' as CrudMode,
      title: '',
      model: {} as Record<string, any>
    }
  })

  for (const field of schema.search ?? []) {
    state.searchModel[field.prop] = undefined
  }

  function cleanParams(source: Record<string, any>) {
    const params: Record<string, any> = {}
    Object.keys(source).forEach((key) => {
      const value = source[key]
      if (value === '' || value === null || value === undefined) return
      if (Array.isArray(value) && value.length === 0) return
      params[key] = value
    })
    return params
  }

  async function fetchList() {
    state.loading = true
    try {
      const res = await schema.api.list({ ...state.query, ...cleanParams(state.searchModel) })
      state.list = res.data?.list ?? []
      state.total = res.data?.total ?? 0
    } finally {
      state.loading = false
    }
  }

  function handleSearch() {
    state.query.pageNum = 1
    fetchList()
  }

  function handleReset() {
    for (const field of schema.search ?? []) {
      state.searchModel[field.prop] = undefined
    }
    state.query.pageNum = 1
    fetchList()
  }

  function handleSelectionChange(rows: any[]) {
    state.selected = rows
  }

  function defaultModel() {
    const model: Record<string, any> = {}
    for (const field of schema.form ?? []) {
      model[field.prop] = field.defaultValue ?? undefined
    }
    return model
  }

  function openCreate() {
    state.dialog.mode = 'create'
    state.dialog.title = '新增'
    state.dialog.model = defaultModel()
    state.dialog.visible = true
  }

  async function openEdit(row: any) {
    state.dialog.mode = 'edit'
    state.dialog.title = '修改'
    let data = { ...row }
    if (schema.api.detail) {
      const res = await schema.api.detail(row[schema.rowKey])
      data = res.data
    }
    state.dialog.model = { ...defaultModel(), ...data }
    state.dialog.visible = true
  }

  async function submit() {
    let payload: any = { ...state.dialog.model }
    if (schema.beforeSubmit) {
      const result = await schema.beforeSubmit(payload, state.dialog.mode)
      if (result === false) return
      payload = result
    }

    if (state.dialog.mode === 'create') {
      await schema.api.create?.(payload)
      ElMessage.success('新增成功')
    } else {
      await schema.api.update?.(payload)
      ElMessage.success('修改成功')
    }
    state.dialog.visible = false
    await fetchList()
  }

  async function handleRemove(rows: any[]) {
    if (!rows.length) {
      ElMessage.warning('请先选择数据')
      return
    }
    const ids = rows.map((row) => row[schema.rowKey]).join(',')
    await ElMessageBox.confirm(`确认删除选中的 ${rows.length} 条数据？`, '提示', { type: 'warning' })
    await schema.api.remove?.(ids)
    ElMessage.success('删除成功')
    if (state.list.length === rows.length && state.query.pageNum > 1) {
      state.query.pageNum -= 1
    }
    await fetchList()
  }

  onMounted(fetchList)

  // 关键：返回 reactive 对象，属性直接是值而不是 ref，
  // 这样通过 props 传给子组件时不需要再 .value
  return Object.assign(state, {
    schema,
    fetchList,
    handleSearch,
    handleReset,
    handleSelectionChange,
    openCreate,
    openEdit,
    submit,
    handleRemove
  })
}

export type CrudInstance = ReturnType<typeof useCrud>
