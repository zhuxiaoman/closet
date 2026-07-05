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
  const history = ref<AiGenerateResponse[]>([]);

  async function generate(req: AiGenerateRequest): Promise<AiGenerateResponse> {
    generating.value = true;
    try {
      const r = await aiApi.generate(req);
      lastResult.value = r;
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
  }

  return { generating, lastResult, history, generate, feedback };
});
