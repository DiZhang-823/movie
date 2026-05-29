import { request } from '../utils/request'
import type { UserMovieStatus } from './userMovie'

export interface UserTvSeasonResponse {
  tvSeasonId: number
  status: UserMovieStatus
  rating: number | null
  watchedDate: string | null
  updatedAt: string | null
}

export interface UserTvSeasonRequest {
  status: UserMovieStatus
  rating?: number | null
  watchedDate?: string | null
}

export async function getUserTvSeason(tvSeasonId: number, token: string): Promise<UserTvSeasonResponse | null> {
  const response = await request(`/user-tv-seasons/${tvSeasonId}`, {
    headers: {
      Authorization: `Bearer ${token}`
    }
  })

  if (response.status === 204) {
    return null
  }

  if (!response.ok) {
    throw new Error('Failed to fetch user TV season status')
  }

  return response.json()
}

export async function saveUserTvSeason(tvSeasonId: number, data: UserTvSeasonRequest, token: string) {
  const response = await request(`/user-tv-seasons/${tvSeasonId}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(data)
  })

  if (!response.ok) {
    throw new Error('Failed to save user TV season status')
  }
}
