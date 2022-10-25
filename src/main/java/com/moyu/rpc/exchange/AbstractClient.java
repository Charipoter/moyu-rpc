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

    private static final int CONNECTED = 2;

    private static final int NOT_CONNECTED = -2;
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
            reconnect();
        } else if (s == NEW && state.compareAndSet(NEW, OPEN)) {
            connect();
        } else {
            throw new RuntimeException("不存在的客户端状态");
        }
    }

    @Override
    public synchronized void connect() {
        // 交给子类完成
        try {
            if (state.get() != CONNECTED) {
                connection = doConnect();
                if (connection != null) {
                    state.compareAndSet(OPEN, CONNECTED);
                }
            }
        } catch (Exception e) {

        }
    }

    protected abstract Connection doConnect();

    @Override
    public synchronized void reconnect() {

        try {// 交给子类完成
            disconnect();
            if (state.get() == NOT_CONNECTED) {
                if (doReconnect() != null) {
                    state.compareAndSet(NOT_CONNECTED, CONNECTED);
                }
            }
        } catch (Exception e) {

        }
    }

    protected abstract Connection doReconnect();

    @Override
    public void disconnect() {
        try {
            if (state.get() == CONNECTED) {
                doDisconnect();
                state.compareAndSet(CONNECTED, NOT_CONNECTED);
            }
        } catch (Exception e) {

        }
    }

    protected abstract void doDisconnect();

    @Override
    public synchronized void close() {
        int s = state.get();
        if (s == CLOSE) {
            return;
        }
        state.set(CLOSE);
        connection.close();
        // 进行额外资源的关闭
        doClose();

    }

    protected abstract void doClose();

    @Override
    public boolean isOpen() {
        return state.get() == OPEN;
    }

    @Override
    public boolean isClosed() {
        return state.get() == CLOSE;
    }

    @Override
    public boolean isConnected() {
        return state.get() == CONNECTED;
    }
}
