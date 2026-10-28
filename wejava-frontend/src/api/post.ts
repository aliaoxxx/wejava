import request from '@/utils/request'

export function listPost(params: any) {
  return request.get<any, any>('/system/post/list', { params })
}

export function getPost(postId: string | number) {
  return request.get<any, any>(`/system/post/${postId}`)
}

export function createPost(data: any) {
  return request.post<any, any>('/system/post', data)
}

export function updatePost(data: any) {
  return request.put<any, any>('/system/post', data)
}

export function removePost(ids: string) {
  return request.delete<any, any>(`/system/post/${ids}`)
}

export function listPostOptions() {
  return request.get<any, any>('/system/post/option-select')
}
