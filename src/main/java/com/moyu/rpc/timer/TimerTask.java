package com.moyu.rpc.timer;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

/**
 * 定时任务本身
 */
@Getter
@Setter
@ToString
public class TimerTask implements Runnable {

    private Timer timer;
    /**
     * 任务
     */
    private Callable<Object> task;
    /**
     * 还剩下几轮才被执行，0 代表当轮执行，-1 代表未计算
     */
    public long remainingRound = -1;
    /**
     * 被执行的相对时间
     */
    public long deadline;
    /**
     * 异步结果，给调用方使用
     */
    private final CompletableFuture<Object> future;
    /**
     * 默认结果，如果提供了 Runnable 就使用这个
     */
    private static final Object defaultResult = null;

    public TimerTask(Runnable task, long deadline, Timer timer) {
        this(() -> {
            task.run();
            return defaultResult;
        }, deadline, timer);
    }

    public TimerTask(Callable<Object> task, long deadline, Timer timer) {
        this.task = task;
        this.deadline = deadline;
        this.future = new CompletableFuture<>();
        this.timer = timer;
    }

    public void run() {

        if (isDone() || isCancelled()) {
            return;
        }

        Object r = null;
        try {
            r = task.call();
        } catch (Exception e) {
            // 忽略
        }

        future.complete(r);
    }

    public boolean isDone() {
        return future.isDone();
    }

    public boolean isCancelled() {
        return future.isCancelled();
    }

    public void cancel() {
        // 如果任务正在被运行那就没办法
        future.cancel(true);
    }

}
