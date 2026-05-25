import {getToken} from './auth'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'https://movie-backend-cgj6.onrender.com/api'
const REQUEST_TIMEOUT_MS = 15000

export async function request(path: string, options: RequestInit = {}) {
    const token = getToken()
    const controller = new AbortController()
    const timeoutId = window.setTimeout(() => {
        controller.abort()
    }, REQUEST_TIMEOUT_MS)

    const headers: Record<string, string> = {
        ...(options.headers as Record<string, string> || {})
    }

    if (!(options.body instanceof FormData)) {
        headers['Content-Type'] = 'application/json'
    }

    if (token) {
        headers['Authorization'] = `Bearer ${token}`
    }

    try {
        return await fetch(`${API_BASE_URL}${path}`, {
            ...options,
            headers,
            signal: options.signal ?? controller.signal
        })
    } finally {
        window.clearTimeout(timeoutId)
    }
}
