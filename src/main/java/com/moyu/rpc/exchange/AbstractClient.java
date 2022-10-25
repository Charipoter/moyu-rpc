package com.moyu.rpc.exchange;

import lombok.Getter;
import lombok.Setter;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@Setter
public abstract class AbstractClient implements Client {

    private static final int NEW = 0;
    private static final int OPEN = 1;
    private static final int CLOSE = -1;

    private AtomicInteger state = new AtomicInteger(NEW);
    private Connection connection;
    private InetSocketAddress localAddress;
    private InetSocketAddress remoteAddress;

    public AbstractClient(InetSocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    @Override
    public CompletableFuture<Object> send(Object message) {
        return connection.send(message);
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    @Override
    public void open() {

        int s = state.get();
        if (s == OPEN) {

        } else if (s == CLOSE && state.compareAndSet(CLOSE, OPEN)) {
            // 关闭了又开启，对于客户端属于重连的情形
            reConnect();
        } else if (s == NEW && state.compareAndSet(NEW, OPEN)) {
            connect();
        } else {
            throw new RuntimeException("不存在的客户端状态");
        }
    }

    @Override
    public void connect() {
        // 交给子类完成
        connection = doConnect();
    }

    protected abstract Connection doConnect();

    @Override
    public void reConnect() {
        // 交给子类完成
        connection = doReConnect();
    }

    protected abstract Connection doReConnect();

    @Override
    public void close() {
        int s = state.get();
        if (s == CLOSE) {
            return;
        }
        if (state.compareAndSet(OPEN, CLOSE)) {
            connection.close();
            // 进行额外资源的关闭
            doClose();
        }
    }

    protected abstract void doClose();
}
