import myAxios from "../request"


export const getCurrentUser = async() =>{
    return await myAxios.get('/user/current');
}
