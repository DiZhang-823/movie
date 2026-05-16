import {getToken} from './auth'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'https://movie-backend-cgj6.onrender.com/api'

export async function request(path: string, options: RequestInit = {}) {
    const token = getToken()

    const headers: Record<string, string> = {
        ...(options.headers as Record<string, string> || {})
    }

    if (!(options.body instanceof FormData)) {
        headers['Content-Type'] = 'application/json'
    }

    if (token) {
        headers['Authorization'] = `Bearer ${token}`
    }

    return await fetch(`${API_BASE_URL}${path}`, {
        ...options,
        headers
    })
}
