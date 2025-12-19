import type { ItemType } from '../models/item'
import myAxios from '../request'

export const getItemRecommendations = (limit = 30) => {
  return myAxios.get<{ data: ItemType[] }>('/item/recommend', {
    params: { limit },
  })
}

export const getItemById = (id: string | number) => {
  return myAxios.get<{ data: ItemType }>('/item/getById', {
    params: { id },
  })
}
