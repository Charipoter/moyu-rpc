package com.moyu.rpc.timer;

import com.moyu.rpc.timer.support.TimeWheelTimer;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * junit 测试即使存在运行线程也会结束程序，所以需要在此测试
 */
public class TimerTest {
    @Slf4j
    private static class Task implements Runnable {
        private final Timer timer;
        private final long delay;
        private final TimeUnit unit;
        private final int segId;

        Task(Timer timer, long delay, TimeUnit unit, int segId) {
            this.timer = timer;
            this.delay = delay;
            this.unit = unit;
            this.segId = segId;
        }

        @Override
        public void run() {
            log.debug(String.valueOf(segId));
            timer.schedule(this, delay, unit);
        }
    }

    public static void main(String[] args) {
        TimeUnit unit = TimeUnit.MILLISECONDS;
        Timer timer = new TimeWheelTimer(100, TimeUnit.MILLISECONDS);
        long delay = 1000;

        List<Task> ts = new ArrayList<>();
        for (int i = 0; i < 10; i++) {

            Task task = new Task(timer, delay, unit, i);
            ts.add(task);
        }
        for (Task task : ts) {
            timer.schedule(task, delay, unit);
        }

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Set<TimerTask> tasks = null;
        try {
            tasks = timer.close().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        System.out.println(tasks.size());

    }

}
