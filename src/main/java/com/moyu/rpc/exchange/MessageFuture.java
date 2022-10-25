package com.moyu.rpc.exchange;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 消息接收中心
 */
@Setter
@Getter
public class MessageFuture extends CompletableFuture<Object> {

    private static final Map<Long, MessageFuture> FUTURE_MAP = new ConcurrentHashMap<>();

    private static final AtomicLong messageId = new AtomicLong();
    /**
     * 收到的
     */
    private Message received;
    /**
     * 发送的
     */
    private Message sent;

    public MessageFuture(Message sent) {
        this.sent = sent;
    }
    /**
     * 创建一个 future
     */
    public static MessageFuture addFuture(Object message) {
        // 生成一个单机唯一 id
        long id = generateFutureId();
        MessageFuture future = new MessageFuture(Message.of(id, message));
        // 放入 map
        FUTURE_MAP.put(id, future);
        return future;
    }

    /**
     * 收到结果
     */
    public static void receive(Message received) {

        long id = received.getId();

        MessageFuture future = FUTURE_MAP.remove(id);

        if (future != null) {
            future.setReceived(received);
            doReceive(received);
            future.complete(received.getRawMessage());
        }

    }

    protected static void doReceive(Message received){

    }


    private static long generateFutureId() {
        return messageId.incrementAndGet();
    }

}
