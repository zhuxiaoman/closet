import { defineStore } from 'pinia';
import { ref } from 'vue';
import { api } from '../api';

export const useOutfitStore = defineStore('outfit', () => {
  const list = ref<unknown[]>([]);
  const total = ref(0);
  const loading = ref(false);

  async function fetchList(filter: Record<string, unknown> = {}) {
    loading.value = true;
    try {
      const data = await api.outfits.list(filter);
      list.value = (data as { records?: unknown[] }).records ?? [];
      total.value = (data as { total?: number }).total ?? 0;
    } finally {
      loading.value = false;
    }
  }

  async function get(id: number) {
    return await api.outfits.get(id);
  }

  async function create(data: unknown) {
    return await api.outfits.create(data);
  }

  async function update(id: number, data: unknown) {
    return await api.outfits.update(id, data);
  }

  async function remove(id: number) {
    return await api.outfits.delete(id);
  }

  async function addItem(
    outfitId: number,
    clothingId: number,
    sortOrder = 0,
  ) {
    return await api.outfits.addItem(outfitId, clothingId, sortOrder);
  }

  async function removeItem(outfitId: number, clothingId: number) {
    return await api.outfits.removeItem(outfitId, clothingId);
  }

  async function reorderItems(outfitId: number, orders: unknown) {
    return await api.outfits.reorderItems(outfitId, orders);
  }

  return {
    list,
    total,
    loading,
    fetchList,
    get,
    create,
    update,
    remove,
    addItem,
    removeItem,
    reorderItems,
  };
});
