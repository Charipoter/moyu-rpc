package com.moyu.rpc.mytimer;

import java.util.Collection;

/**
 * 用于并发处理一堆任务
 */
public interface TimerTaskExecutor {
    /**
     * 只执行，不关心结果，执行失败由任务自己承担
     */
    void execute(Collection<? extends TimerTask> tasks);

    void execute(TimerTask task);


}
