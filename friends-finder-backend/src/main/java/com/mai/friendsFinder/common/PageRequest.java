package com.mai.friendsFinder.common;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
@Data
public class PageRequest implements Serializable {


    @Serial
    private static final long serialVersionUID = -7759193589194661939L;

    /**
     * 页面大小
     */
    protected int pageSize;


    /**
     * 页码
     */
    protected int pageNo;
}
