package com.mai.friendsFinder.model.request;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class TeamQuitRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 4547843701060563417L;

    /**
     * 队伍id
     */
    private Long teamId;
}
