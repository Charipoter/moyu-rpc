package com.moyu.rpc.timer;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class TestTimer {
    @Slf4j
    private static class Task implements Runnable {

        private Timer timer;
        private long delay;
        private TimeUnit unit;

        private int i;

        Task(Timer timer, long delay, TimeUnit unit, int i) {
            this.timer = timer;
            this.delay = delay;
            this.unit = unit;
            this.i = i;
        }

        @Override
        public void run() {
            log.debug(String.valueOf(i));
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
