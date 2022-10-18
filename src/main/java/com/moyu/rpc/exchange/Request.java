package com.moyu.rpc.exchange;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class Request {

    private long id;

    private Object content;

    public Request(Object content) {
        this.content = content;
    }

}
