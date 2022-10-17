package com.moyu.rpc.timer;

import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class TimeWheelTimer implements Timer {

    private Worker worker;
    private Thread workerThread;
    /**
     * 相对开启时间
     */
    private volatile long startTime;

    private ThreadFactory threadFactory;

    /**
     * 当前已经处理过了哪个 tick
     */
    private int currentTick;
    /**
     * 每个 tick 占多少时间，代表了精度
     */
    private long tickDuration;
    /**
     * 时间轮的每一个 tick
     */
    private TimeWheelBucket buckets[];
    /**
     * 为了更好地控制，任务并不是直接给到时间轮，放进队列里，由 worker 具体放入
     */
    private BlockingQueue<TimerTask> outOfWheelTasks;
    /**
     * 异步执行器
     */
    private TimerTaskExecutor executor;
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
        // 也许该方法可以换个位置？
        start();
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
    public CompletableFuture<Object> schedule(Runnable task, long delay, TimeUnit unit) {
        TimerTask timerTask = new TimerTask(task, deadlineForDelay(delay, unit), this);
        return submitTask(timerTask);
    }

    @Override
    public CompletableFuture<Object> schedule(Callable<Object> task, long delay, TimeUnit unit) {
        TimerTask timerTask = new TimerTask(task, deadlineForDelay(delay, unit), this);
        return submitTask(timerTask);
    }

    private CompletableFuture<Object> submitTask(TimerTask task) {
        outOfWheelTasks.add(task);
        return task.getFuture();
    }

    private long deadlineForDelay(long delay, TimeUnit unit) {
        long deadline = System.nanoTime() - startTime + unit.toNanos(delay);

        if (deadline < 0 && delay > 0) {
            deadline = Long.MAX_VALUE;
        }

        return deadline;
    }

    @Override
    public Set<TimerTask> close() {
        return null;
    }

    /**
     * 负责执行任务
     */
    private class Worker implements Runnable {

        private final AtomicInteger state;

        public static final int READY = 0;
        public static final int RUNNING = 1;
        public static final int SHUTDOWN = 2;

        Worker() {
            this.state = new AtomicInteger(READY);
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
                processTasks();
                currentTick++;
            } while (!isShutdown());

            // 处理未被执行的任务

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

        private void transferTaskToBucket(TimerTask task) {
            // 注意，某个任务可能由于任务队列太慢的原因，导致传输它的时候已经到了其的 deadline
            // 此时只能把他放进下一个 tick 了

            // 需要跨越的 tick 数，我们会把其放到第 tick 个桶内
            long passedTicks = task.deadline / tickDuration;
            // 需要跨越的轮转数
            long round = (passedTicks - currentTick) / (buckets.length);

            task.setRemainingRound(round);
            // 找到具体的桶，注意我们可能错过了这个任务的执行，我们不能把它放到之前的 tick 里
            long optimizedPassedTicks = Math.max(currentTick, passedTicks);
            int stopIndex = (int) optimizedPassedTicks % buckets.length;

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

        public void runTasks() {
            Node node = head;

            while (node != null) {
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
                node = node.next;
            }
        }

    }
}
