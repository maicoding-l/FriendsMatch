package com.mai.friendsFinder.model.request;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class DeleteRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 3489347184912436299L;
    private long id;
}
