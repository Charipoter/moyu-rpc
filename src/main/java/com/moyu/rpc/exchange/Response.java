package com.moyu.rpc.exchange;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class Response {

    private long id;

    private Object result;

}
