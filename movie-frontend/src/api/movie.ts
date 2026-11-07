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
    imdbVoteAverage: number | null
    imdbVoteCount: number | null
    imdbRatingUpdatedAt: string | null
    posterPath: string | null
    createdAt: string
    userMovieStatus: 'WANT_TO_WATCH' | 'WATCHED' | 'NOT_INTERESTED' | null
    userRating: number | null
    userWatchedDate: string | null
    userMovieUpdatedAt: string | null
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
    userMovieStatus?: 'DEFAULT' | 'WANT_TO_WATCH' | 'WATCHED' | 'NOT_INTERESTED' | 'UNMARKED'
    userRatingMin?: number
    userRatingMax?: number
    movieRatingMin?: number
    movieRatingMax?: number
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

export type MovieCollectionItem = {
    id: number
    tmdbId: number
    title: string
    releaseDate: string | null
    voteAverage: number | null
    voteCount: number | null
    imdbVoteAverage: number | null
    imdbVoteCount: number | null
    imdbRatingUpdatedAt: string | null
    posterPath: string | null
    sequenceNumber: number
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
    imdbVoteAverage: number | null
    imdbVoteCount: number | null
    imdbRatingUpdatedAt: string | null
    posterPath: string | null
    imdbId: string | null
    genres: string[]
    companies: string[]
    countries: string[]
    collectionName: string | null
    collectionMovies: MovieCollectionItem[]
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

    if (params.userRatingMin !== undefined) {
        searchParams.set('userRatingMin', String(params.userRatingMin))
    }

    if (params.userRatingMax !== undefined) {
        searchParams.set('userRatingMax', String(params.userRatingMax))
    }

    if (params.movieRatingMin !== undefined) {
        searchParams.set('movieRatingMin', String(params.movieRatingMin))
    }

    if (params.movieRatingMax !== undefined) {
        searchParams.set('movieRatingMax', String(params.movieRatingMax))
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

export async function updateMovieTitle(id: number, title: string): Promise<MovieDetail> {
    const response = await request(`/admin/movies/${id}/title`, {
        method: 'PUT',
        body: JSON.stringify({ title })
    })

    if (!response.ok) {
        throw new Error('更新电影标题失败')
    }

    return response.json()
}

export async function deleteMovie(id: number): Promise<void> {
    const response = await request(`/admin/movies/${id}`, {
        method: 'DELETE'
    })

    if (!response.ok) {
        throw new Error('删除电影失败')
    }
}
