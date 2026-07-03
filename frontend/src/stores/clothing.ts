import { defineStore } from 'pinia';
import { ref } from 'vue';
import { api } from '../api';

export const useClothingStore = defineStore('clothing', () => {
  const list = ref<unknown[]>([]);
  const total = ref(0);
  const loading = ref(false);

  async function fetchList(filter: Record<string, unknown> = {}) {
    loading.value = true;
    try {
      const data = await api.clothing.list(filter);
      list.value = (data as { records?: unknown[] }).records ?? [];
      total.value = (data as { total?: number }).total ?? 0;
    } finally {
      loading.value = false;
    }
  }

  async function get(id: number) {
    return await api.clothing.get(id);
  }

  async function create(data: unknown) {
    return await api.clothing.create(data);
  }

  async function update(id: number, data: unknown) {
    return await api.clothing.update(id, data);
  }

  async function remove(id: number) {
    return await api.clothing.delete(id);
  }

  return { list, total, loading, fetchList, get, create, update, remove };
});
