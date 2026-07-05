// AI 搭配 API 客户端
//
// 复用 src/api/index.ts 的 Result 信封约定：
//   { code: number, message: string, data: T }
// 这里自带一个最小 request()，避免与主 api 模块耦合（AI 接口调用量小、独立演进）。

type AiResult<T> = { code: number; message?: string; data: T };

const BASE: string =
  (import.meta as unknown as { env: Record<string, string> }).env
    .VITE_API_BASE_URL || '/api';

export interface AiOutfit {
  id: number;
  items: number[];
  score?: number;
}

export interface AiWeather {
  temp: number;
  condition: string;
}

export interface AiGenerateRequest {
  seedClothingIds: number[];
  occasion: string;
  season: string;
  weather?: AiWeather;
}

export interface AiGenerateResponse {
  generationId: number;
  outfits: number[][];
}

export type AiFeedback = 'like' | 'dislike' | 'none';

async function request<T>(
  method: string,
  url: string,
  body?: unknown,
): Promise<T> {
  const resp = await fetch(BASE + url, {
    method,
    headers: body ? { 'Content-Type': 'application/json' } : undefined,
    body: body ? JSON.stringify(body) : undefined,
  });
  const json = (await resp.json().catch(() => ({}))) as AiResult<T>;
  if (!resp.ok || (json.code !== undefined && json.code !== 0)) {
    throw new Error(
      json.message || `${method} ${url} failed (${resp.status})`,
    );
  }
  return json.code !== undefined ? json.data : (json as unknown as T);
}

export const aiApi = {
  generate(req: AiGenerateRequest): Promise<AiGenerateResponse> {
    return request<AiGenerateResponse>('POST', '/v1/outfits/ai-generate', req);
  },
  feedback(generationId: number, feedback: AiFeedback): Promise<unknown> {
    return request<unknown>(
      'POST',
      `/v1/outfits/ai-generation/${generationId}/feedback`,
      { feedback },
    );
  },
};
