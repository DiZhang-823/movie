import { request } from '../utils/request'

export type AwardBody = {
  id: number
  name: string
  originalName: string | null
  sortOrder: number | null
  description: string | null
  createdAt: string | null
  updatedAt: string | null
}

export type AwardBodyPayload = {
  name: string
  originalName?: string | null
  sortOrder?: number | null
  description?: string | null
}

export type AwardEdition = {
  id: number
  awardBodyId: number
  awardBodyName: string
  name: string
  editionNumber: number
  awardYear: number | null
  ceremonyDate: string | null
  description: string | null
  createdAt: string | null
  updatedAt: string | null
}

export type AwardEditionPayload = {
  awardBodyId: number
  name: string
  editionNumber: number
  awardYear?: number | null
  ceremonyDate?: string | null
  description?: string | null
}

export type AwardCategory = {
  id: number
  awardEditionId: number
  awardEditionName: string
  awardBodyId: number
  awardBodyName: string
  name: string
  sortOrder: number | null
  description: string | null
  createdAt: string | null
  updatedAt: string | null
}

export type AwardCategoryPayload = {
  awardEditionId: number
  name: string
  sortOrder?: number | null
  description?: string | null
}

export type AwardNomination = {
  id: number
  awardCategoryId: number
  awardCategoryName: string
  awardEditionId: number
  awardEditionName: string
  awardBodyId: number
  awardBodyName: string
  movieId: number | null
  movieTitle: string | null
  moviePosterPath: string | null
  tvSeasonId: number | null
  tvSeasonTitle: string | null
  tvSeasonShowName: string | null
  tvSeasonNumber: number | null
  tvSeasonPosterPath: string | null
  personId: number | null
  personName: string | null
  personProfilePath: string | null
  winner: boolean
  note: string | null
  sortOrder: number | null
  createdAt: string | null
  updatedAt: string | null
}

export type AwardNominationPayload = {
  awardCategoryId: number
  movieId?: number | null
  tvSeasonId?: number | null
  personId?: number | null
  winner?: boolean
  note?: string | null
  sortOrder?: number | null
}

async function parseResponse<T>(response: Response, fallbackMessage: string): Promise<T> {
  if (!response.ok) {
    const text = await response.text()
    throw new Error(text || fallbackMessage)
  }
  return response.json()
}

export async function getAwardBodies(): Promise<AwardBody[]> {
  return parseResponse(await request('/admin/awards/bodies'), 'Failed to load award bodies')
}

export async function createAwardBody(payload: AwardBodyPayload): Promise<AwardBody> {
  return parseResponse(await request('/admin/awards/bodies', {
    method: 'POST',
    body: JSON.stringify(payload)
  }), 'Failed to create award body')
}

export async function updateAwardBody(id: number, payload: AwardBodyPayload): Promise<AwardBody> {
  return parseResponse(await request(`/admin/awards/bodies/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  }), 'Failed to update award body')
}

export async function deleteAwardBody(id: number): Promise<void> {
  const response = await request(`/admin/awards/bodies/${id}`, {
    method: 'DELETE'
  })
  if (!response.ok) {
    const text = await response.text()
    throw new Error(text || 'Failed to delete award body')
  }
}

export async function getAwardEditions(awardBodyId?: number): Promise<AwardEdition[]> {
  const suffix = awardBodyId ? `?awardBodyId=${awardBodyId}` : ''
  return parseResponse(await request(`/admin/awards/editions${suffix}`), 'Failed to load award editions')
}

export async function createAwardEdition(payload: AwardEditionPayload): Promise<AwardEdition> {
  return parseResponse(await request('/admin/awards/editions', {
    method: 'POST',
    body: JSON.stringify(payload)
  }), 'Failed to create award edition')
}

export async function updateAwardEdition(id: number, payload: AwardEditionPayload): Promise<AwardEdition> {
  return parseResponse(await request(`/admin/awards/editions/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  }), 'Failed to update award edition')
}

export async function getAwardCategories(awardEditionId: number): Promise<AwardCategory[]> {
  return parseResponse(
    await request(`/admin/awards/categories?awardEditionId=${awardEditionId}`),
    'Failed to load award categories'
  )
}

export async function createAwardCategory(payload: AwardCategoryPayload): Promise<AwardCategory> {
  return parseResponse(await request('/admin/awards/categories', {
    method: 'POST',
    body: JSON.stringify(payload)
  }), 'Failed to create award category')
}

export async function updateAwardCategory(id: number, payload: AwardCategoryPayload): Promise<AwardCategory> {
  return parseResponse(await request(`/admin/awards/categories/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  }), 'Failed to update award category')
}

export async function getAwardNominations(awardCategoryId: number): Promise<AwardNomination[]> {
  return parseResponse(
    await request(`/admin/awards/nominations?awardCategoryId=${awardCategoryId}`),
    'Failed to load award nominations'
  )
}

export async function createAwardNomination(payload: AwardNominationPayload): Promise<AwardNomination> {
  return parseResponse(await request('/admin/awards/nominations', {
    method: 'POST',
    body: JSON.stringify(payload)
  }), 'Failed to create award nomination')
}
