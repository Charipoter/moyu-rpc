package com.moyu.rpc.exchange;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程安全的 server 抽象类
 */
public abstract class AbstractConcurrentServer extends AbstractServer {
    /**
     * 服务器状态
     */
    private AtomicInteger state = new AtomicInteger(NEW);

    public AbstractConcurrentServer(InetSocketAddress localAddress) {
        super(localAddress);
    }

    @Override
    public void open() {
        // 只允许开启一次
        if (state.get() == NEW && state.compareAndSet(NEW, OPEN)) {
            doOpen();
        }
    }

    @Override
    public void close() {
        if (state.get() == CLOSE) {
            return;
        }
        synchronized (this) {
            int s = state.get();
            if (s != CLOSE) {
                if (s == NEW && state.compareAndSet(NEW, CLOSE)) {
                    // 没有资源需要关闭
                } else if (s == OPEN && state.compareAndSet(OPEN, CLOSE)) {
                    // 关闭所有实际连接
                    clientConnectionMap.values().forEach(Connection::close);
                    // 进一步关闭其他资源
                    doClose();
                }
            }
        }
    }

    @Override
    public boolean isOpen() {
        return state.get() == OPEN;
    }

    @Override
    public boolean isClosed() {
        return state.get() == CLOSE;
    }
}
