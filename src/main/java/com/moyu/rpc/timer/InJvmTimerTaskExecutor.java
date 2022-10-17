package com.moyu.rpc.mytimer;

import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class InJvmTimerTaskExecutor implements TimerTaskExecutor {

    private ExecutorService executor;

    public InJvmTimerTaskExecutor() {
        this.executor = new ThreadPoolExecutor(10, 20, 10, TimeUnit.MINUTES,
                new LinkedBlockingQueue<>(), new DefaultThreadFactory("timerExec"),
                // 任务被拒绝了由调用方自己承担
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @Override
    public void execute(Collection<? extends TimerTask> tasks) {
        tasks.forEach(this::execute);
    }

    @Override
    public void execute(TimerTask task) {
        executor.execute(task);
    }
}
