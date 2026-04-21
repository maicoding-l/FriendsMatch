import myAxios from "../request"

/**
 * 发送好友申请
 * @param toUserId 接收申请的用户id
 * @param message 申请消息
 */
export const sendFriendRequest = async (toUserId: number, message?: string) => {
    return await myAxios.post('/friend/request/send', {
        toUserId,
        message
    }).then((response: any) => {
        console.log("/friend/request/send success", response);
        return response?.data;
    }).catch((error: any) => {
        console.log("/friend/request/send error", error);
        throw error;
    });
}

/**
 * 处理好友申请
 * @param requestId 申请id
 * @param action 操作类型：1-同意，2-拒绝
 */
export const handleFriendRequest = async (requestId: number, action: number) => {
    return await myAxios.post('/friend/request/handle', {
        requestId,
        action
    }).then((response: any) => {
        console.log("/friend/request/handle success", response);
        return response?.data;
    }).catch((error: any) => {
        console.log("/friend/request/handle error", error);
        throw error;
    });
}

/**
 * 获取收到的好友申请
 */
export const getReceivedRequests = async () => {
    return await myAxios.get('/friend/request/received').then((response: any) => {
        console.log("/friend/request/received success", response);
        return response?.data?.data;
    }).catch((error: any) => {
        console.log("/friend/request/received error", error);
        throw error;
    });
}

/**
 * 获取发送的好友申请
 */
export const getSentRequests = async () => {
    return await myAxios.get('/friend/request/sent').then((response: any) => {
        console.log("/friend/request/sent success", response);
        return response?.data?.data;
    }).catch((error: any) => {
        console.log("/friend/request/sent error", error);
        throw error;
    });
}

/**
 * 获取好友列表
 */
export const getFriendList = async () => {
    return await myAxios.get('/friend/list').then((response: any) => {
        console.log("/friend/list success", response);
        return response?.data?.data;
    }).catch((error: any) => {
        console.log("/friend/list error", error);
        throw error;
    });
}

/**
 * 删除好友
 * @param friendId 好友id
 */
export const deleteFriend = async (friendId: number) => {
    return await myAxios.post('/friend/delete', {
        id: friendId
    }).then((response: any) => {
        console.log("/friend/delete success", response);
        return response?.data;
    }).catch((error: any) => {
        console.log("/friend/delete error", error);
        throw error;
    });
}

/**
 * 检查是否是好友
 * @param userId 用户id
 */
export const checkFriend = async (userId: number) => {
    return await myAxios.get('/friend/check', {
        params: { userId }
    }).then((response: any) => {
        console.log("/friend/check success", response);
        return response?.data?.data;
    }).catch((error: any) => {
        console.log("/friend/check error", error);
        throw error;
    });
}
