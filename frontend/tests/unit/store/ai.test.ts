import { describe, it, expect, beforeEach, vi } from 'vitest';
import { setActivePinia, createPinia } from 'pinia';
import { useAiStore } from '@/store/ai';
import { aiApi } from '@/api/ai';

const fakeResponse = {
  generationId: 42,
  outfits: [
    [1, 2, 3],
    [4, 5, 6],
    [7, 8, 9],
    [10, 11, 12],
    [13, 14, 15],
  ],
};

describe('ai store / feedback 乐观更新', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.restoreAllMocks();
  });

  it('feedback 成功后 lastResult 引用保持不变且 lastFeedbackAt 更新', async () => {
    vi.spyOn(aiApi, 'generate').mockResolvedValue(fakeResponse);
    vi.spyOn(aiApi, 'feedback').mockResolvedValue({});

    const ai = useAiStore();
    await ai.generate({
      seedClothingIds: [1],
      occasion: 'casual',
      season: 'all',
    });
    expect(ai.lastResult).not.toBeNull();
    expect(ai.lastFeedbackAt).toBeNull();

    const beforeRef = ai.lastResult;
    await ai.feedback(ai.lastResult!.generationId, 'like');
    const afterRef = ai.lastResult;

    expect(afterRef).toBe(beforeRef); // 引用未变，UI 可继续用
    expect(afterRef?.generationId).toBe(42);
    expect(aiApi.feedback).toHaveBeenCalledWith(42, 'like');
    expect(typeof ai.lastFeedbackAt).toBe('number');
    expect(ai.lastFeedbackAt).toBeGreaterThan(0);
  });

  it('feedback 对历史 generationId 调用不会清空 lastResult 也不记录时间戳', async () => {
    vi.spyOn(aiApi, 'generate')
      .mockResolvedValueOnce(fakeResponse)
      .mockResolvedValueOnce({
        ...fakeResponse,
        generationId: 99,
        outfits: [[1, 2]],
      });
    vi.spyOn(aiApi, 'feedback').mockResolvedValue({});

    const ai = useAiStore();
    // 第一次 generate：generationId=42
    await ai.generate({
      seedClothingIds: [1],
      occasion: 'casual',
      season: 'all',
    });
    expect(ai.lastResult?.generationId).toBe(42);
    // 触发一次同代 feedback,记录时间戳
    await ai.feedback(42, 'like');
    const firstFeedbackAt = ai.lastFeedbackAt;
    expect(firstFeedbackAt).not.toBeNull();

    // 第二次 generate：generationId=99（lastResult 切换,时间戳重置）
    await ai.generate({
      seedClothingIds: [2],
      occasion: 'work',
      season: 'spring',
    });
    expect(ai.lastResult?.generationId).toBe(99);
    expect(ai.lastFeedbackAt).toBeNull();

    // 对历史 generationId (42) 调用 feedback,不影响新的 lastResult,也不更新时间戳
    await expect(ai.feedback(42, 'dislike')).resolves.toBeUndefined();
    expect(ai.lastResult?.generationId).toBe(99);
    expect(ai.lastFeedbackAt).toBeNull();
  });
});