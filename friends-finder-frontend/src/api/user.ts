import myAxios from "../request"


export const getCurrentUser = async() =>{
    return await myAxios.get('/user/current').then((response : any)=>{
        console.log("/user/current success",response);
        return response?.data?.data;
    }).catch((error :any)=>{
        console.log("/user/current error",error);
    });
}

/**
 * 匹配用户（包含共同物品信息）
 * @param num 匹配数量
 * @returns 用户匹配结果列表
 */
export const matchUsersWithCommonItems = (num: number = 10) => {
    return myAxios.get('/user/match/withCommonItems', {
        params: { num }
    })
}

/**
 * 根据用户ID获取用户信息
 * @param id 用户ID
 * @returns 用户信息
 */
export const getUserById = async (id: number) => {
    return await myAxios.get(`/user/${id}`).then((response: any) => {
        console.log(`/user/${id} success`, response);
        return response?.data?.data;
    }).catch((error: any) => {
        console.log(`/user/${id} error`, error);
        throw error;
    });
}
