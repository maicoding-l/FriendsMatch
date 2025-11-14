package com.mai.friendsFinder.model.request;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
@Data
public class UserLoginRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1048693345229389347L;
    private String userAccount;
    private String userPassword;
}
