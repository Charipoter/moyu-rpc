package com.moyu.rpc.exchange;

import java.net.InetSocketAddress;

/**
 * 非线程安全的 client 抽象类
 */
public abstract class AbstractSimpleServer extends AbstractServer {
    private volatile int state = NEW;

    public AbstractSimpleServer(InetSocketAddress localAddress) {
        super(localAddress);
    }

    @Override
    public void open() {
        // 只允许开启一次
        if (state == NEW) {
            doOpen();
            state = OPEN;
        }
    }

    @Override
    public void close() {
        if (state == CLOSE) {
            return;
        }

        switch (state) {
            case OPEN -> {
                // 关闭所有实际连接
                clientConnectionMap.values().forEach(Connection::close);
                // 进一步关闭其他资源
                doClose();
            }
            case NEW -> {
                // 没有需要关闭的资源
            }
            default -> {}
        }

        state = CLOSE;
    }

    @Override
    public boolean isOpen() {
        return state == OPEN;
    }

    @Override
    public boolean isClosed() {
        return state == CLOSE;
    }
}
