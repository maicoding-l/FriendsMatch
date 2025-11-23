import { UserType } from './user'

/**
 *
 * 队伍类别
 *
 */
export type TeamType = {
  id: number
  teamName: string
  description: string
  maxNum: number
  expireTime?: Date //？表示可有可无
  // todo做成枚举类型
  status: 0
  password: string
  createTime: Date
  updateTime: Date
  createUser?: UserType
  userId: number
  hasJoinNum?: number
  membersList?: number[]
}
