// Frontend API wrapper.
//
// Auto-generated `paths` types live in ./schema.d.ts (produced by
// openapi-typescript from http://localhost:8080/v3/api-docs). The
// backend convention is the standard Result envelope:
//
//   { code: number, message: string, data: T }
//
// so `request<T>` unwraps `data` and rejects anything whose code is
// not 0. The frontend dev server (see vite.config.ts) proxies `/api`
// to http://localhost:8080, so `BASE` defaults to a relative path.

import type { paths } from './schema';

type ApiResult<T> = { code: number; message?: string; data: T };

const BASE: string =
  (import.meta as unknown as { env: Record<string, string> }).env
    .VITE_API_BASE_URL || '/api';

class ApiError extends Error {
  code: number;
  constructor(code: number, message: string) {
    super(message);
    this.code = code;
  }
}

async function request<T>(
  method: string,
  url: string,
  body?: unknown,
  query?: Record<string, string | number | boolean | undefined>,
): Promise<T> {
  let full = BASE + url;
  if (query) {
    const params = new URLSearchParams();
    for (const [k, v] of Object.entries(query)) {
      if (v === undefined || v === null) continue;
      params.set(k, String(v));
    }
    const qs = params.toString();
    if (qs) full += (full.includes('?') ? '&' : '?') + qs;
  }
  const resp = await fetch(full, {
    method,
    headers: body ? { 'Content-Type': 'application/json' } : undefined,
    body: body ? JSON.stringify(body) : undefined,
  });
  const json = (await resp.json().catch(() => ({}))) as ApiResult<T>;
  if (!resp.ok || (json.code !== undefined && json.code !== 0)) {
    throw new ApiError(
      json.code ?? resp.status,
      json.message || `${method} ${url} failed (${resp.status})`,
    );
  }
  return (json.code !== undefined ? json.data : (json as unknown as T));
}

function toQuery(filter: Record<string, unknown> | undefined): Record<string, string> {
  const out: Record<string, string> = {};
  if (!filter) return out;
  for (const [k, v] of Object.entries(filter)) {
    if (v === undefined || v === null || v === '') continue;
    out[k] = String(v);
  }
  return out;
}

export type ApiSchema = paths;
export { ApiError };

export const api = {
  clothing: {
    list: (filter?: Record<string, unknown>) =>
      request('GET', '/v1/clothing', undefined, toQuery(filter)),
    get: (id: number) => request('GET', `/v1/clothing/${id}`),
    create: (data: unknown) => request('POST', '/v1/clothing', data),
    update: (id: number, data: unknown) =>
      request('PUT', `/v1/clothing/${id}`, data),
    delete: (id: number) => request('DELETE', `/v1/clothing/${id}`),
    uploadImage: (id: number, file: File) => {
      const form = new FormData();
      form.append('file', file);
      return fetch(BASE + `/v1/clothing/${id}/images`, {
        method: 'POST',
        body: form,
      }).then((r) => r.json());
    },
    deleteImage: (id: number, imageId: number) =>
      request('DELETE', `/v1/clothing/${id}/images/${imageId}`),
  },

  categories: {
    list: () => request('GET', '/v1/categories'),
    create: (data: unknown) => request('POST', '/v1/categories', data),
    update: (id: number, data: unknown) =>
      request('PUT', `/v1/categories/${id}`, data),
    delete: (id: number) => request('DELETE', `/v1/categories/${id}`),
  },

  tags: {
    list: () => request('GET', '/v1/tags'),
    create: (data: unknown) => request('POST', '/v1/tags', data),
    update: (id: number, data: unknown) =>
      request('PUT', `/v1/tags/${id}`, data),
    delete: (id: number) => request('DELETE', `/v1/tags/${id}`),
  },

  outfits: {
    list: (filter?: Record<string, unknown>) =>
      request('GET', '/v1/outfits', undefined, toQuery(filter)),
    get: (id: number) => request('GET', `/v1/outfits/${id}`),
    create: (data: unknown) => request('POST', '/v1/outfits', data),
    update: (id: number, data: unknown) =>
      request('PUT', `/v1/outfits/${id}`, data),
    delete: (id: number) => request('DELETE', `/v1/outfits/${id}`),
    addItem: (outfitId: number, clothingId: number, sortOrder = 0) =>
      request('POST', `/v1/outfits/${outfitId}/items`, undefined, {
        clothingId,
        sortOrder,
      }),
    removeItem: (outfitId: number, clothingId: number) =>
      request('DELETE', `/v1/outfits/${outfitId}/items/${clothingId}`),
    reorderItems: (outfitId: number, orders: unknown) =>
      request('PUT', `/v1/outfits/${outfitId}/items/reorder`, orders),
  },

  calendar: {
    range: (from: string, to: string) =>
      request('GET', '/v1/calendar', undefined, { from, to }),
    get: (id: number) => request('GET', `/v1/calendar/${id}`),
    create: (data: unknown) => request('POST', '/v1/calendar', data),
    update: (id: number, data: unknown) =>
      request('PUT', `/v1/calendar/${id}`, data),
    delete: (id: number) => request('DELETE', `/v1/calendar/${id}`),
  },

  stats: {
    overview: () => request('GET', '/v1/stats/overview'),
    forClothing: (id: number) => request('GET', `/v1/stats/clothing/${id}`),
    mostWorn: (limit = 10) =>
      request('GET', '/v1/stats/most-worn', undefined, { limit }),
    leastWorn: (days = 90) =>
      request('GET', '/v1/stats/least-worn', undefined, { days }),
  },

  wearLogs: {
    create: (data: { clothingId: number; wornAt: string }) =>
      request('POST', '/v1/wear-logs', data),
    delete: (id: number) => request('DELETE', `/v1/wear-logs/${id}`),
  },

  images: {
    proxyUrl: (objectKey: string) => `${BASE}/v1/images/${objectKey}`,
  },
};
