package com.mai.friendsFinder.exception;

import com.mai.friendsFinder.common.BaseResponse;
import com.mai.friendsFinder.common.ErrorCode;
import com.mai.friendsFinder.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 *
 * 全局异常处理器
 *
 * @author ljm
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public BaseResponse businessExceptionHandler(BusinessException e) {
        log.error("businessException",e);
        return ResultUtils.error(e.getCode(),e.getMessage(),e.getDescription());
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse runtimeExceptionHandler(RuntimeException e) {
        log.error("runtimeException",e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR,e.getMessage(),"");
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public BaseResponse<?> handleJsonParseError(HttpMessageNotReadableException e) {
        log.error("JSON 解析失败", e);
        return ResultUtils.error(400, "请求体格式不正确：" + e.getMessage(), null);
    }

}
