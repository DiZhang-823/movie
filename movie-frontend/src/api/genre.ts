import {request} from "../utils/request.ts";

export interface GenreItem {
    id: number
    tmdbId: number
    name: string
}

export async function getGenres(): Promise<GenreItem[]> {
    const response = await request(`/genres`)

    if (!response.ok) {
        throw new Error('Failed to fetch genres')
    }

    return response.json()
}