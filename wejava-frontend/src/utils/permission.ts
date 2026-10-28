import { useUserStore } from '@/store/modules/user'

export function hasPerm(perm: string | string[]): boolean {
  const perms = useUserStore().perms
  if (perms.includes('*:*:*')) return true
  const list = Array.isArray(perm) ? perm : [perm]
  return list.some((p) => perms.includes(p))
}

export function hasRole(role: string | string[]): boolean {
  const roles = useUserStore().roles
  if (roles.includes('admin')) return true
  const list = Array.isArray(role) ? role : [role]
  return list.some((r) => roles.includes(r))
}
