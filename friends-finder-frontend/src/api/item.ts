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

export const getItemRecommendationsByRandomWalk = (userId: number, topN = 20) => {
  return myAxios.get<{ data: ItemType[] }>('/item/recommendByRandomWalk', {
    params: { userId, topN },
  })
}

/**
 * 切换收藏状态
 * @param itemId 物品ID
 * @returns 当前收藏状态（true-已收藏，false-已取消收藏）
 */
export const toggleFavorite = (itemId: number) => {
  return myAxios.post<{ data: boolean }>('/item/toggleFavorite', null, {
    params: { itemId },
  })
}

/**
 * 对物品打分
 * @param itemId 物品ID
 * @param score 分数 (1.0-5.0)
 * @returns 是否成功
 */
export const rateItem = (itemId: number, score: number) => {
  return myAxios.post<{ data: boolean }>('/item/rate', null, {
    params: { itemId, score },
  })
}

/**
 * 获取用户对物品的交互状态
 * @param itemId 物品ID
 * @returns UserItem对象（包含action和weight）
 */
export interface UserItemAction {
  id: number
  userId: number
  itemId: number
  action: number // 1-喜欢 2-收藏 3-看过/读过/听过
  weight: number
  createTime: string
  updateTime: string
}

export const getUserItemAction = (itemId: number) => {
  return myAxios.get<{ data: UserItemAction | null }>('/item/getUserAction', {
    params: { itemId },
  })
}
