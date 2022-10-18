package com.moyu.rpc.exchange;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 异步请求的存放地
 */
public class FutureReception extends CompletableFuture<Object> {
    // 存放请求，响应到了就 complete future
    private static final Map<Long, FutureReception> FUTURE_MAP = new ConcurrentHashMap<>();
    // 单机唯一 id
    private final Long id;
    // 发出的请求
    private final Request request;

    private FutureReception(Request request) {
        super();
        this.id = AutoIncId.newId();
        this.request = request;
        this.request.setId(id);
    }

    public static FutureReception addFuture(Request request) {
        return addFuture(request, null);
    }

    public static FutureReception addFuture(Request request, Object resultIfPresent) {
        FutureReception futureReception = new FutureReception(request);

        FUTURE_MAP.put(futureReception.getId(), futureReception);

        if (resultIfPresent != null) {
            futureReception.complete(resultIfPresent);
        }
        // 开启定时检查超时任务

        return futureReception;
    }

    /**
     * 接收结果，根据 id 处理
     */
    public static void receive(Response response) {

        long id = response.getId();

        FutureReception futureReception = FUTURE_MAP.remove(id);
        if (futureReception != null) {
            // 暂时就那么简单
            futureReception.complete(response.getResult());

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
