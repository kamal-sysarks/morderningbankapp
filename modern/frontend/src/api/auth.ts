import api from './client'

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  username: string
  employeeName: string
}

export const login = (data: LoginRequest) =>
  api.post<LoginResponse>('/api/auth/login', data).then((r) => r.data)
