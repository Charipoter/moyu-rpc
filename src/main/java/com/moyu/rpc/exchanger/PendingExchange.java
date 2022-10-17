package com.moyu.rpc.exchanger;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 异步请求的存放地
 */
public class PendingExchange extends CompletableFuture<Object> {
    // 存放请求，响应到了就 complete future
    private static final Map<Long, PendingExchange> PENDING_MAP = new ConcurrentHashMap<>();
    // 单机唯一 id
    private Long id;
    // 发出的请求
    private Request request;

    private PendingExchange(Request request) {
        super();
        this.id = AutoIncId.newId();
        this.request = request;
        this.request.setId(id);
    }

    public static PendingExchange pending(Request request) {
        return pending(request, null);
    }

    /**
     * 无需异步，自带结果
     */
    public static PendingExchange pending(Request request, Object resultIfPresent) {
        return pending(request, resultIfPresent, -1, null);
    }

    public static PendingExchange pending(Request request, Object resultIfPresent, long timeout, TimeUnit unit) {
        PendingExchange pendingExchange = new PendingExchange(request);

        PENDING_MAP.put(pendingExchange.getId(), pendingExchange);

        if (resultIfPresent != null) {
            pendingExchange.complete(resultIfPresent);
        } else if (timeout > 0) {
            // 开启定时检查超时任务

        }

        return pendingExchange;
    }

    /**
     * 接收结果，根据 id 处理
     */
    public static void receive(Response response) {

        Long id = response.getId();

        if (id == null) {
            // ???
            return;
        }

        PendingExchange pendingExchange = PENDING_MAP.remove(id);
        if (pendingExchange != null) {
            // 暂时就那么简单
            pendingExchange.complete(response.getResult());

        }

    }

    public Long getId() {
        return this.id;
    }


    private static class AutoIncId {

        private static final AtomicLong id = new AtomicLong();

        public static long newId() {
            return id.incrementAndGet();
        }

    }

}
