import myAxios from "../request"
import type { ChatMessage } from "../composables/useWebSocket"

/**
 * 获取聊天历史
 * @param targetUserId 目标用户ID
 * @param pageNum 页码
 * @param pageSize 每页大小
 * @returns 聊天历史列表
 */
export const getChatHistory = async (targetUserId: number, pageNum: number = 1, pageSize: number = 50) => {
    return await myAxios.get('/message/history', {
        params: { targetUserId, pageNum, pageSize }
    }).then((response: any) => {
        console.log('/message/history success', response);
        return response?.data?.data;
    }).catch((error: any) => {
        console.log('/message/history error', error);
        throw error;
    });
}

/**
 * 获取聊天列表（最近联系人）
 * @returns 聊天列表
 */
export const getChatList = async () => {
    return await myAxios.get('/message/list').then((response: any) => {
        console.log('/message/list success', response);
        return response?.data?.data;
    }).catch((error: any) => {
        console.log('/message/list error', error);
        throw error;
    });
}

/**
 * 获取未读消息数
 * @param targetUserId 目标用户ID
 * @returns 未读消息数
 */
export const getUnreadCount = async (targetUserId: number) => {
    return await myAxios.get('/message/unread', {
        params: { targetUserId }
    }).then((response: any) => {
        console.log('/message/unread success', response);
        return response?.data?.data;
    }).catch((error: any) => {
        console.log('/message/unread error', error);
        throw error;
    });
}

/**
 * 标记消息为已读
 * @param messageId 消息ID
 * @returns 是否成功
 */
export const markAsRead = async (messageId: number) => {
    return await myAxios.post(`/message/read/${messageId}`).then((response: any) => {
        console.log(`/message/read/${messageId} success`, response);
        return response?.data?.data;
    }).catch((error: any) => {
        console.log(`/message/read/${messageId} error`, error);
        throw error;
    });
}
