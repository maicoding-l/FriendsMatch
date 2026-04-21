import myAxios from '../request'

export interface ItemReviewAddRequest {
  itemId: number
  content: string
  rating?: number
}

export interface ItemReviewVO {
  id: number
  user: {
    id: number
    username: string
    avatarUrl: string
  }
  itemId: number
  content: string
  rating: number | null
  likeCount: number
  isLiked: boolean
  createTime: string
}

/**
 * 添加短评
 */
export const addReview = async (reviewData: ItemReviewAddRequest) => {
  return await myAxios.post('/review/add', reviewData)
}

/**
 * 获取物品的短评列表
 */
export const getReviewList = async (itemId: number) => {
  return await myAxios.get(`/review/list?itemId=${itemId}`)
}

/**
 * 删除短评
 */
export const deleteReview = async (id: number) => {
  return await myAxios.delete(`/review/delete?id=${id}`)
}

/**
 * 点赞/取消点赞短评
 */
export const toggleLikeReview = async (reviewId: number) => {
  return await myAxios.post(`/review/like?reviewId=${reviewId}`)
}
