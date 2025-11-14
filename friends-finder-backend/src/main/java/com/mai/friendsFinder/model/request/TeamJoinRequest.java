package com.mai.friendsFinder.model.request;


import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class TeamJoinRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 5446951157009729035L;

    /**
     * 队伍ID
     */
    private Long teamId;

    /**
     * 密码
     */
    private String password;
}
