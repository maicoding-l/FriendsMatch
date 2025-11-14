import myAxios from "../request"


export const getCurrentUser = async() =>{
    return await myAxios.get('/user/current').then((response : any)=>{
        console.log("/user/current success",response);
        return response?.data?.data;
    }).catch((error :any)=>{
        console.log("/user/current error",error);
    });
}
