import { http, type PageData } from '@/utils/http'

export function listPost(params: Record<string, unknown>) {
  return http.get<PageData>('/system/post/list', { params })
}

export function getPost(postId: string | number) {
  return http.get<Record<string, unknown>>(`/system/post/${postId}`)
}

export function createPost(data: Record<string, unknown>) {
  return http.post('/system/post', data)
}

export function updatePost(data: Record<string, unknown>) {
  return http.put('/system/post', data)
}

export function removePost(ids: string) {
  return http.delete(`/system/post/${ids}`)
}

export function listPostOptions() {
  return http.get<Record<string, unknown>[]>('/system/post/option-select')
}
