package com.mai.friendsFinder.common;

/**
 * 返回工具类
 *
 * @author ljm
 */
public class ResultUtils {
    /**
     * 成功返回
     * @param data
     * @return
     * @param <T>
     */
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(0,data,"ok");
    }

    /**
     * 失败返回
     * @param errorCode
     * @return
     */
    public static BaseResponse error(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode.getCode(),null, errorCode.getMsg(),errorCode.getDescription());
    }

    /**
     * 失败
     *
     * @author ljm
     */
    public static BaseResponse error(ErrorCode errorCode,String msg, String description) {
        return new BaseResponse(errorCode.getCode(),null,msg,description);
    }

    /**
     * 失败
     *
     * @author ljm
     */
    public static BaseResponse error(int code,String msg,String description) {
        return new BaseResponse(code,null,msg,description);
    }


}
