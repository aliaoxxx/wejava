import { reactive } from 'vue'
import { getDictByType } from '@/api/dict'
import type { Column, Option } from '@/types/crud'

/**
 * 字典缓存。schema 里 `dict` 传字符串时，自动按 dictType 从后端加载。
 * 一次会话内只请求一次，字典改动后刷新页面生效。
 */
const cache = reactive<Record<string, Option[]>>({})
const pending = new Set<string>()

export async function loadDict(type: string): Promise<void> {
  if (cache[type] || pending.has(type)) return
  pending.add(type)
  try {
    const res = await getDictByType(type)
    cache[type] = (res.data ?? []).map((item: any) => ({
      label: item.dictLabel,
      value: item.dictValue
    }))
  } catch {
    cache[type] = []
  } finally {
    pending.delete(type)
  }
}

/** 解析 dict：数组直接用，字符串查缓存 */
export function dictOptions(dict?: Option[] | string): Option[] | undefined {
  if (!dict) return undefined
  if (Array.isArray(dict)) return dict
  return cache[dict]
}

/** 预先加载 schema 里用到的所有字典类型 */
export async function loadSchemaDicts(columns: Column[]): Promise<void> {
  const types = columns
    .map((column) => column.dict)
    .filter((dict): dict is string => typeof dict === 'string')
  await Promise.all([...new Set(types)].map(loadDict))
}
