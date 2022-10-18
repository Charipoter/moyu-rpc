package com.moyu.rpc.timer.support;

import com.moyu.rpc.timer.Timer;
import com.moyu.rpc.timer.TimerTask;
import com.moyu.rpc.timer.TimerTaskExecutor;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class TimeWheelTimer implements Timer {

    private final Worker worker;
    private final Thread workerThread;
    /**
     * 相对开启时间
     */
    private volatile long startTime;

    private final ThreadFactory threadFactory;

    /**
     * 当前已经处理过了哪个 tick
     */
    private int currentTick;
    /**
     * 每个 tick 占多少时间，代表了精度
     */
    private final long tickDuration;
    /**
     * 时间轮的每一个 tick
     */
    private final TimeWheelBucket[] buckets;
    /**
     * 为了更好地控制，任务并不是直接给到时间轮，放进队列里，由 worker 具体放入
     */
    private final BlockingQueue<TimerTask> outOfWheelTasks;
    /**
     * 异步执行器
     */
    private final TimerTaskExecutor executor;
    /**
     * 如果提交的是 Runnable，默认返回该结果
     */
    private final Object defaultResult = new Object();

    public TimeWheelTimer(long tickDuration, TimeUnit unit) {
        this.worker = new Worker();

        this.threadFactory = new DefaultThreadFactory("timer");

        this.workerThread = threadFactory.newThread(worker);

        this.outOfWheelTasks = new LinkedBlockingQueue<>();

        this.currentTick = 0;

        this.tickDuration = unit.toNanos(tickDuration);

        this.executor = new InJvmTimerTaskExecutor();

        this.buckets = createBuckets();

        this.startTime = -1;
    }

    private TimeWheelBucket[] createBuckets() {
        // TODO:暂时默认 100 个桶，后续自动计算
        TimeWheelBucket[] buckets = new TimeWheelBucket[100];
        for (int i = 0; i < 100; i++) {
            buckets[i] = new TimeWheelBucket();
        }
        return buckets;
    }

    private void start() {
        // worker 只允许一次性的运行停止
        if (worker.isReady() && worker.tryStart()) {
            workerThread.start();
        }
    }

    @Override
    public void schedule(Runnable task, long delay, TimeUnit unit) {
        if (worker.isShutdown()) {
            return;
        }
        preSchedule();

        TimerTask timerTask = new TimerTask(task, deadlineForDelay(delay, unit), this);
        outOfWheelTasks.add(timerTask);
    }

    @Override
    public CompletableFuture<Object> schedule(Callable<Object> task, long delay, TimeUnit unit) {
        if (worker.isShutdown()) {
            return CompletableFuture.completedFuture(defaultResult);
        }
        preSchedule();

        TimerTask timerTask = new TimerTask(task, deadlineForDelay(delay, unit), this);
        outOfWheelTasks.add(timerTask);
        return timerTask.getFuture();
    }
    /**
     * 检查 startTime 是否设置，否则 deadline 将计算有误
     */
    private void preSchedule() {
        if (startTime < 0) {
            start();
        }
    }

    private long deadlineForDelay(long delay, TimeUnit unit) {
        long deadline = System.nanoTime() - startTime + unit.toNanos(delay);

        if (deadline < 0 && delay > 0) {
            deadline = Long.MAX_VALUE;
        }

        return deadline;
    }

    /**
     * 关闭相关线程并返回未处理完的任务
     */
    @Override
    public CompletableFuture<Set<TimerTask>> close() {
        // shutdown worker
        worker.shutdown();
        return worker.shutdownFuture;
    }

    /**
     * 负责执行任务
     */
    private class Worker implements Runnable {
        private final AtomicInteger state;
        public static final int READY = 0;
        public static final int RUNNING = 1;
        public static final int SHUTDOWN = 2;
        public CompletableFuture<Set<TimerTask>> shutdownFuture;

        Worker() {
            this.state = new AtomicInteger(READY);
            this.shutdownFuture = new CompletableFuture<>();
        }

        public boolean tryStart() {
            return state.compareAndSet(READY, RUNNING);
        }

        @Override
        public void run() {

            if (isShutdown()) {
                return;
            }
            // 初始化开启时间
            startTime = System.nanoTime();

            do {
                // 等到下一个 tick 开始，在此期间不要空闲
                waitUntilNextTick();
                // 处理当前 tick，注意可能没法在 tick 持续时间内处理完，解决方案：
                // 1. 使用多线程处理
                // 2. 对于没处理完的，延后一个 tick
                if (!isShutdown()) {
                    processTasks();
                    currentTick++;
                }

            } while (!isShutdown());
            // worker 被 shutdown 了，但仍可能有任务在异步执行，关闭执行器拿到等待执行的任务
            afterShutdown();
        }

        private void afterShutdown() {
            // 未进入时间轮的任务
            Set<TimerTask> tasks = new HashSet<>(outOfWheelTasks);
            // 异步执行器未处理的
            tasks.addAll(executor.close());
            // 桶内未处理的
            for (TimeWheelBucket bucket : buckets) {
                tasks.addAll(bucket.close());
            }
            shutdownFuture.complete(tasks);
        }

        private void processTasks() {
            // 找到对应的桶，并发执行任务
            TimeWheelBucket bucket = buckets[currentTick % buckets.length];
            bucket.runTasks();
        }

        private void waitUntilNextTick() {
            long currentTime = System.nanoTime() - startTime;
            long nextTickStartTime = (currentTick + 1) * tickDuration;
            long timeToWait = nextTickStartTime - currentTime;

            // 我们错过了某一个 tick，赶紧返回去执行
            if (timeToWait <= 0) {
                return;
            }

            long deadline = startTime + nextTickStartTime;
            // 这段时间用来处理队列内的任务
            TimerTask task;
            while (deadline - System.nanoTime() >= 10000) {
                try {
                    task = outOfWheelTasks.poll(timeToWait, TimeUnit.NANOSECONDS);

                    if (task == null) {
                        // 超时了
                        return;
                    }
                    // 将其放入桶中
                    transferTaskToBucket(task);

                } catch (InterruptedException e) {
                    // worker 被 shutdown 了
                    return;
                }
            }
        }

        /**
         * 将某个任务放入桶中
         * 注意，某个任务可能由于任务队列太慢的原因，导致传输它的时候已经到了其的 deadline
         * 此时只能把他放进下一个 tick 了
         */
        private void transferTaskToBucket(TimerTask task) {
            // 需要跨越的 tick 数
            long passedTicks = task.deadline / tickDuration;
            // 需要跨越的轮转数
            task.setRemainingRound((passedTicks - currentTick) / (buckets.length));
            // 找到具体的桶，注意我们可能错过了这个任务的执行，我们不能把它放到之前的 tick 里
            long calculatedPassedTicks = Math.max(currentTick, passedTicks);
            int stopIndex = (int) calculatedPassedTicks % buckets.length;

            TimeWheelBucket bucket = buckets[stopIndex];
            bucket.addTask(task);
        }

        public boolean isShutdown() {
            return state.get() == SHUTDOWN;
        }

        public boolean isRunning() {
            return state.get() == RUNNING;
        }

        public boolean isReady() {
            return state.get() == READY;
        }

        public void shutdown() {
            state.set(SHUTDOWN);
            // 如若 worker 在等待下一个 tick，将其唤醒
            workerThread.interrupt();
        }
    }

    private class TimeWheelBucket {

        // 链表存储，需要自定义来提高灵活性
        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        private class Node {
            private TimerTask task;
            private Node prev;
            private Node next;
        }

        private Node head;
        private Node tail;
        private int size;

        TimeWheelBucket() {
        }

        public void addTask(TimerTask task) {
            Node t = tail, newNode = new Node(task, tail, null);
            tail = newNode;
            if (t == null) {
                head = newNode;
            } else {
                t.next = newNode;
            }
            size++;
        }

        private void removeNode(Node node) {
            Node prev = node.prev, next = node.next;
            if (prev == null) {
                head = next;
            } else {
                prev.next = next;
                node.prev = null;
            }
            if (next == null) {
                tail = prev;
            } else {
                next.prev = prev;
                node.next = null;
            }
            node.task = null;
            size--;
        }

        /**
         * 执行桶内任务，注意 worker 可能在执行时被 shutdown
         */
        public void runTasks() {
            Node node = head;
            while (node != null && !worker.isShutdown()) {
                // 注意保存后继节点，否则删除当前节点后链路就断了
                Node next = node.next;
                TimerTask task = node.task;

                if (task.isCancelled()) {
                    removeNode(node);
                } else if (task.remainingRound <= 0) {
                    // 可以立即执行
                    removeNode(node);
                    executor.execute(task);
                } else {
                    task.remainingRound--;
                }
                node = next;
            }
        }
        /**
         * 拿到桶内剩下的任务
         */
        public Set<TimerTask> close() {
            if (worker.isShutdown()) {
                Set<TimerTask> tasks = new HashSet<>();
                Node node = head;
                while (node != null) {
                    tasks.add(node.task);
                    node = node.next;
                }
                return tasks;
            }
            throw new UnsupportedOperationException("worker 还在运行，不能关闭桶");
        }

    }
}
