import {request} from "../utils/request.ts";

export type UserMovieStatus = 'WANT_TO_WATCH' | 'WATCHED' | 'NOT_INTERESTED'

export interface UserMovieResponse {
    movieId: number
    status: UserMovieStatus
    rating: number | null
    watchedDate: string | null
    updatedAt: string | null
}

export interface UserMovieRequest {
  status: 'WANT_TO_WATCH' | 'WATCHED' | 'NOT_INTERESTED'
  rating?: number | null
  watchedDate?: string | null
}

export async function getUserMovie(movieId: number, token: string) {
    const response = await request(`/user-movies/${movieId}`, {
        headers: {
            Authorization: `Bearer ${token}`
        }
    })

    if (response.status === 204) {
        return null
    }

    if (!response.ok) {
        throw new Error('Failed to fetch user movie status')
    }

    return await response.json()
}


export async function saveUserMovie(movieId: number, data: UserMovieRequest, token: string) {
  const response = await request(`/user-movies/${movieId}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(data)
  })

  if (!response.ok) {
    throw new Error('保存用户电影状态失败')
  }
}