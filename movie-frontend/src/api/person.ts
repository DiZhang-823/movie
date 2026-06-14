import { request } from '../utils/request'

export type PersonMovieCredit = {
    movieId: number
    tmdbId: number
    title: string
    originalTitle: string | null
    releaseDate: string | null
    voteAverage: number | null
    voteCount: number | null
    imdbVoteAverage: number | null
    imdbVoteCount: number | null
    imdbRatingUpdatedAt: string | null
    posterPath: string | null
    jobs: string[]
    character: string | null
}

export type PersonDetail = {
    id: number
    tmdbId: number
    name: string
    originalName: string | null
    gender: number | null
    birthday: string | null
    deathday: string | null
    placeOfBirth: string | null
    biography: string | null
    knownForDepartment: string | null
    profilePath: string | null
    popularity: number | null
    movieCreditCount: number
    credits: PersonMovieCredit[]
}

export type PersonSearchItem = {
    personId: number
    name: string
    originalName: string | null
    profilePath: string | null
    character: string | null
    job: string | null
}

export async function searchPersons(name: string, size = 8): Promise<PersonSearchItem[]> {
    const searchParams = new URLSearchParams()
    if (name.trim()) {
        searchParams.set('name', name.trim())
    }
    searchParams.set('size', String(size))

    const response = await request(`/persons?${searchParams.toString()}`)

    if (!response.ok) {
        throw new Error('搜索人物失败')
    }

    return response.json()
}

export async function getPersonById(id: number): Promise<PersonDetail> {
    const response = await request(`/persons/${id}`)

    if (!response.ok) {
        throw new Error('获取人物详情失败')
    }

    return response.json()
}
