import { request } from '../utils/request'
import type { PersonItem } from './movie'

export interface TvSeasonItem {
  id: number
  tmdbTvId: number
  tmdbSeasonId: number | null
  seasonNumber: number
  seasonCount: number | null
  title: string
  originalTitle: string | null
  showName: string
  originalShowName: string | null
  airDate: string | null
  year: number | null
  episodeCount: number | null
  voteAverage: number | null
  voteCount: number | null
  imdbVoteAverage: number | null
  imdbVoteCount: number | null
  imdbRatingUpdatedAt: string | null
  posterPath: string | null
  createdAt: string
  userTvSeasonStatus: 'WANT_TO_WATCH' | 'WATCHED' | 'NOT_INTERESTED' | null
  userRating: number | null
  userWatchedDate: string | null
  userTvSeasonUpdatedAt: string | null
}

export interface TvSeasonPageResponse {
  content: TvSeasonItem[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}

export interface GetTvSeasonsParams {
  title?: string
  genreId?: number
  year?: number
  regionCode?: string
  userTvSeasonStatus?: 'DEFAULT' | 'WANT_TO_WATCH' | 'WATCHED' | 'NOT_INTERESTED' | 'UNMARKED'
  userRatingMin?: number
  userRatingMax?: number
  tvSeasonRatingMin?: number
  tvSeasonRatingMax?: number
  page?: number
  size?: number
  sortBy?: string
  direction?: string
}

export interface TvSeasonCollectionItem {
  id: number
  seasonNumber: number
  seasonName: string
  airDate: string | null
  voteAverage: number | null
  imdbVoteAverage: number | null
  posterPath: string | null
}

export interface TvSeasonDetail {
  id: number
  tmdbTvId: number
  tmdbSeasonId: number | null
  seasonNumber: number
  seasonCount: number | null
  title: string
  originalTitle: string | null
  showName: string
  originalShowName: string | null
  overview: string | null
  posterPath: string | null
  airDate: string | null
  year: number | null
  episodeCount: number | null
  voteAverage: number | null
  voteCount: number | null
  imdbVoteAverage: number | null
  imdbVoteCount: number | null
  imdbRatingUpdatedAt: string | null
  imdbParentId: string | null
  genres: string[]
  countries: string[]
  producers: PersonItem[]
  directors: PersonItem[]
  writers: PersonItem[]
  cast: PersonItem[]
  collectionSeasons: TvSeasonCollectionItem[]
}

export async function getTvSeasons(params: GetTvSeasonsParams): Promise<TvSeasonPageResponse> {
  const searchParams = new URLSearchParams()

  if (params.title) searchParams.set('title', params.title)
  if (params.genreId !== undefined) searchParams.set('genreId', String(params.genreId))
  if (params.year !== undefined) searchParams.set('year', String(params.year))
  if (params.regionCode !== undefined) searchParams.set('regionCode', params.regionCode)
  if (params.userTvSeasonStatus !== undefined) searchParams.set('userTvSeasonStatus', params.userTvSeasonStatus)
  if (params.userRatingMin !== undefined) searchParams.set('userRatingMin', String(params.userRatingMin))
  if (params.userRatingMax !== undefined) searchParams.set('userRatingMax', String(params.userRatingMax))
  if (params.tvSeasonRatingMin !== undefined) searchParams.set('tvSeasonRatingMin', String(params.tvSeasonRatingMin))
  if (params.tvSeasonRatingMax !== undefined) searchParams.set('tvSeasonRatingMax', String(params.tvSeasonRatingMax))

  searchParams.set('page', String(params.page ?? 0))
  searchParams.set('size', String(params.size ?? 20))
  searchParams.set('sortBy', params.sortBy ?? 'ID')
  searchParams.set('direction', params.direction ?? 'asc')

  const response = await request(`/tv-seasons?${searchParams.toString()}`)

  if (!response.ok) {
    throw new Error('Failed to fetch TV seasons')
  }

  return response.json()
}

export async function getTvSeasonById(id: number): Promise<TvSeasonDetail> {
  const response = await request(`/tv-seasons/${id}`)

  if (!response.ok) {
    throw new Error('Failed to fetch TV season detail')
  }

  return response.json()
}

export async function deleteTvSeason(id: number): Promise<void> {
  const response = await request(`/admin/tv-seasons/${id}`, {
    method: 'DELETE'
  })

  if (!response.ok) {
    throw new Error('Failed to delete TV season')
  }
}
