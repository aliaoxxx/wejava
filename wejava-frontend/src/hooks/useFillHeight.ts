import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'

/**
 * 量出容器可用高度，供 el-table 固定表头并在内部滚动。
 * <p>
 * el-table 的 height 传百分比在 flex 容器里算不准（会按内容高度撑开），
 * 所以这里用 ResizeObserver 量出真实像素高度再传进去。
 */
export function useFillHeight() {
  const wrapRef = ref<HTMLElement>()
  const height = ref<number>()
  let observer: ResizeObserver | undefined

  onMounted(() => {
    nextTick(() => {
      if (!wrapRef.value) return
      observer = new ResizeObserver((entries) => {
        const value = entries[0]?.contentRect.height
        if (value) height.value = Math.floor(value)
      })
      observer.observe(wrapRef.value)
    })
  })

  onBeforeUnmount(() => observer?.disconnect())

  return { wrapRef, height }
}
