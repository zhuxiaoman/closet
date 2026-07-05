import { defineStore } from 'pinia';
import { ref } from 'vue';
import {
  aiApi,
  type AiFeedback,
  type AiGenerateRequest,
  type AiGenerateResponse,
} from '@/api/ai';

const HISTORY_MAX = 20;

export const useAiStore = defineStore('ai', () => {
  const generating = ref(false);
  const lastResult = ref<AiGenerateResponse | null>(null);
  const lastFeedbackAt = ref<number | null>(null);
  const history = ref<AiGenerateResponse[]>([]);

  async function generate(req: AiGenerateRequest): Promise<AiGenerateResponse> {
    generating.value = true;
    try {
      const r = await aiApi.generate(req);
      lastResult.value = r;
      // 重新生成后清掉反馈时间戳，避免误导 UI
      lastFeedbackAt.value = null;
      history.value.unshift(r);
      if (history.value.length > HISTORY_MAX) {
        history.value.length = HISTORY_MAX;
      }
      return r;
    } finally {
      generating.value = false;
    }
  }

  async function feedback(
    generationId: number,
    feedback: AiFeedback,
  ): Promise<void> {
    await aiApi.feedback(generationId, feedback);
    // 乐观更新: 引用检查只在反馈当前展示的搭配时生效,避免对历史
    // generation 调用反馈后污染 lastResult(给后续 UI 渲染造成混乱)。
    // v2.1 会在这里加 likeCount / dislikeCount 等累计计数。
    if (lastResult.value?.generationId === generationId) {
      lastFeedbackAt.value = Date.now();
    }
  }

  function clearLastFeedback(): void {
    lastFeedbackAt.value = null;
  }

  return {
    generating,
    lastResult,
    lastFeedbackAt,
    history,
    generate,
    feedback,
    clearLastFeedback,
  };
});