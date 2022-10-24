package com.moyu.rpc.exchange;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 网络中传输的消息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Message {
    /**
     * 消息 id，注意这是单机唯一
     */
    private long id;
    /**
     * 原生的数据
     */
    private Object rawMessage;

    public static Message of(long id, Object message) {
        return new Message(id, message);
    }

}
