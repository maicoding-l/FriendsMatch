package com.mai.friendsFinder.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 *   用户注册请求
 *
 * @author ljm
 */
@Data
public class UserRegisterRequest implements Serializable {


    private static final long serialVersionUID = -7355320073425265823L;
    private String userAccount;
    private String userPassword;
    private String checkPassword;
    private String planetCode;
}
