package com.moyu.rpc.mytimer;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 延迟任务调度器
 */
public interface Timer {

    CompletableFuture<Object> schedule(Runnable task, long delay, TimeUnit unit);
    CompletableFuture<Object> schedule(Callable<Object> task, long delay, TimeUnit unit);
    /**
     * 返回未执行完的任务
     */
    Set<TimerTask> close();

}
