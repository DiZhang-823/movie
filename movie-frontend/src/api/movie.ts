import { request } from '../utils/request'

export interface MovieItem {
    id: number
    tmdbId: number
    title: string
    originalTitle: string | null
    popularity: number | null
    releaseDate: string | null
    voteAverage: number | null
    voteCount: number | null
    posterPath: string | null
    createdAt: string
}

export interface MoviePageResponse {
    content: MovieItem[]
    page: number
    size: number
    totalElements: number
    totalPages: number
    last: boolean
}

export interface GetMoviesParams {
    title?: string
    genreId?: number
    year?: number
    regionCode?: string
    userMovieStatus?: 'WANT_TO_WATCH' | 'WATCHED' | 'NOT_INTERESTED' | 'UNMARKED'
    page?: number
    size?: number
    sortBy?: string
    direction?: string
}

export type PersonItem = {
    personId: number
    name: string
    originalName: string
    profilePath: string | null
    character: string | null
    job: string | null
}

export type MovieDetail = {
    id: number
    tmdbId: number
    title: string
    originalTitle: string | null
    overview: string | null
    popularity: number | null
    releaseDate: string | null
    revenue: number | null
    runtime: number | null
    voteAverage: number | null
    voteCount: number | null
    posterPath: string | null
    imdbId: string | null
    genres: string[]
    companies: string[]
    countries: string[]
    directors: PersonItem[]
    cast: PersonItem[]
}

export async function getMovies(params: GetMoviesParams): Promise<MoviePageResponse> {
    const searchParams = new URLSearchParams()

    if (params.title) {
        searchParams.set('title', params.title)
    }

    if (params.genreId !== undefined) {
        searchParams.set('genreId', String(params.genreId))
    }

    if (params.year !== undefined) {
        searchParams.set('year', String(params.year))
    }

    if (params.regionCode !== undefined) {
        searchParams.set('regionCode', String(params.regionCode))
    }

    if (params.userMovieStatus !== undefined) {
        searchParams.set('userMovieStatus', params.userMovieStatus)
    }

    searchParams.set('page', String(params.page ?? 0))
    searchParams.set('size', String(params.size ?? 20))
    searchParams.set('sortBy', params.sortBy ?? 'ID')
    searchParams.set('direction', params.direction ?? 'asc')

    const response = await request(`/movies?${searchParams.toString()}`)

    if (!response.ok) {
        throw new Error('Failed to fetch movies')
    }

    return response.json()
}

export async function getMovieById(id: number): Promise<MovieDetail> {
    const response = await request(`/movies/${id}`)

    if (!response.ok) {
        throw new Error('获取电影详情失败')
    }

    return response.json()
}
