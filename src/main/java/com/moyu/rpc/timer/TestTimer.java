package com.moyu.rpc.timer;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadLocalRandom;
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
        TimeUnit unit = TimeUnit.SECONDS;
        Timer timer = new TimeWheelTimer(10, TimeUnit.MILLISECONDS);
        for (int i = 0; i < 100; i++) {
            int delay = ThreadLocalRandom.current().nextInt(4) + 1;
            Task task = new Task(timer, delay, unit, i);
            timer.schedule(task, delay, unit);
        }

    }

}
