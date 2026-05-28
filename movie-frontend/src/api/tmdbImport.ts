import { request } from '../utils/request'

export type TmdbImportStatus = {
    nextTmdbId: number
    enabled: boolean
    intervalSeconds: number
    updatedAt?: string | null
}

export type TmdbImportRunItem = {
    tmdbId: number
    success: boolean
    title?: string | null
    originalTitle?: string | null
    errorMessage?: string | null
}

export type TmdbImportRunResult = {
    startTmdbId: number
    requestedCount: number
    successCount: number
    failureCount: number
    items: TmdbImportRunItem[]
}

export type ImdbRatingsSyncResult = {
    datasetUrl: string
    moviesWithImdbId: number
    totalRatingsRows: number
    matchedMovies: number
    updatedMovies: number
    missingMovies: number
    missingOnly: boolean
    startedAt: string
    finishedAt: string
}

export type ImdbRatingsSyncStatus = {
    lastSyncedAt: string | null
    missingRatingCount: number
    syncing: boolean
    currentStartedAt: string | null
    lastErrorMessage: string | null
    lastResult: ImdbRatingsSyncResult | null
}

export async function fetchTmdbImportStatus(): Promise<TmdbImportStatus> {
    const response = await request('/admin/tmdb-import/status')
    if (!response.ok) {
        throw new Error('Failed to load TMDB import status')
    }
    return await response.json()
}

export async function setTmdbImportEnabled(enabled: boolean): Promise<TmdbImportStatus> {
    const response = await request('/admin/tmdb-import/switch', {
        method: 'PUT',
        body: JSON.stringify({ enabled })
    })
    if (!response.ok) {
        throw new Error('Failed to update TMDB import switch')
    }
    return await response.json()
}

export async function setTmdbImportInterval(intervalSeconds: number): Promise<TmdbImportStatus> {
    const response = await request('/admin/tmdb-import/interval', {
        method: 'PUT',
        body: JSON.stringify({ intervalSeconds })
    })
    if (!response.ok) {
        throw new Error('Failed to update TMDB import interval')
    }
    return await response.json()
}

export async function runTmdbImportOnce(tmdbId: number) {
    const response = await request('/admin/tmdb-import/run-once', {
        method: 'POST',
        body: JSON.stringify({ tmdbId })
    })
    if (!response.ok) {
        const text = await response.text()
        throw new Error(text || 'Failed to run TMDB import')
    }
    return await response.json()
}

export async function runTmdbImportBatch(tmdbId: number, count = 1): Promise<TmdbImportRunResult> {
    const response = await request('/admin/tmdb-import/run', {
        method: 'POST',
        body: JSON.stringify({ tmdbId, count })
    })
    if (!response.ok) {
        const text = await response.text()
        throw new Error(text || 'Failed to run TMDB import')
    }
    return await response.json()
}

export async function fetchImdbRatingsSyncStatus(): Promise<ImdbRatingsSyncStatus> {
    const response = await request('/admin/imdb-ratings/status')
    if (!response.ok) {
        throw new Error('Failed to load IMDb rating sync status')
    }
    return await response.json()
}

export async function syncImdbRatings(): Promise<ImdbRatingsSyncStatus> {
    const response = await request('/admin/imdb-ratings/sync', {
        method: 'POST'
    })
    if (!response.ok) {
        const text = await response.text()
        throw new Error(text || 'Failed to sync IMDb ratings')
    }
    return await response.json()
}

export async function syncMissingImdbRatings(): Promise<ImdbRatingsSyncStatus> {
    const response = await request('/admin/imdb-ratings/sync-missing', {
        method: 'POST'
    })
    if (!response.ok) {
        const text = await response.text()
        throw new Error(text || 'Failed to sync missing IMDb ratings')
    }
    return await response.json()
}
