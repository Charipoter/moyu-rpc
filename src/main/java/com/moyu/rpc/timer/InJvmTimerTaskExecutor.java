package com.moyu.rpc.timer;

import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
public class InJvmTimerTaskExecutor implements TimerTaskExecutor {

    private ExecutorService executor;

    private BlockingQueue<Runnable> taskQueue;

    public InJvmTimerTaskExecutor() {
        this.taskQueue = new LinkedBlockingQueue<>();
        this.executor = new ThreadPoolExecutor(10, 20, 10, TimeUnit.MINUTES,
                taskQueue, new DefaultThreadFactory("timerExec"),
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

    @Override
    public Set<TimerTask> close() {
        List<Runnable> tasks = executor.shutdownNow();
        // 如果一个任务已被获取，就一定会被执行，因此只需要拿到队列中的任务
        return tasks.stream().map(r -> (TimerTask) r).collect(Collectors.toSet());
    }
}
