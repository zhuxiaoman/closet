import { defineStore } from 'pinia';
import { ref } from 'vue';
import { api } from '../api';

export const useCalendarStore = defineStore('calendar', () => {
  const list = ref<unknown[]>([]);
  const loading = ref(false);

  async function range(from: string, to: string) {
    loading.value = true;
    try {
      const data = await api.calendar.range(from, to);
      list.value = (data as { records?: unknown[] }).records ?? data ?? [];
    } finally {
      loading.value = false;
    }
  }

  async function get(id: number) {
    return await api.calendar.get(id);
  }

  async function create(data: unknown) {
    return await api.calendar.create(data);
  }

  async function update(id: number, data: unknown) {
    return await api.calendar.update(id, data);
  }

  async function remove(id: number) {
    return await api.calendar.delete(id);
  }

  return { list, loading, range, get, create, update, remove };
});
