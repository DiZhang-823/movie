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

export async function getPersonById(id: number): Promise<PersonDetail> {
    const response = await request(`/persons/${id}`)

    if (!response.ok) {
        throw new Error('获取人物详情失败')
    }

    return response.json()
}
