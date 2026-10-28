import type { Directive, DirectiveBinding } from 'vue'
import { hasPerm } from '@/utils/permission'

/**
 * 无权限时隐藏元素。
 * 用 display:none 而不是 removeChild —— 直接移除 DOM 节点会让 Vue 的 vdom
 * 与实际 DOM 不一致，在 el-table 这类频繁复用的场景里会抛 "parentNode of null"。
 */
export const hasPermDirective: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    if (!hasPerm(binding.value)) {
      el.style.display = 'none'
    }
  }
}
