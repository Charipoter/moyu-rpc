package com.moyu.rpc.timer;

import java.util.Collection;
import java.util.Set;

/**
 * 用于并发处理一堆任务
 */
public interface TimerTaskExecutor {
    /**
     * 只执行，不关心结果，执行失败由任务自己承担
     */
    void execute(Collection<? extends TimerTask> tasks);

    void execute(TimerTask task);
    /**
     * 关闭执行器并返回还未被执行的任务
     */
    Set<TimerTask> close();


}
