package com.mai.friendsFinder.exception;

import com.mai.friendsFinder.common.ErrorCode;

/**
 * 自定义异常类
 *
 * @author ljm
 */
public class BusinessException extends RuntimeException {
    private final int code;
    private final String description;

    public BusinessException(String message, String description, int code) {
        super(message);
        this.description = description;
        this.code = code;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMsg());
        this.code = errorCode.getCode();
        this.description = errorCode.getDescription();
    }

    public BusinessException(ErrorCode errorCode, String description) {
        super(errorCode.getMsg());
        this.code = errorCode.getCode();
        this.description = description;
    }


    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
