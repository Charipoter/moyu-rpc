package com.moyu.rpc.Myexchange;

import lombok.Getter;
import lombok.Setter;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

@Getter
@Setter
public abstract class AbstractClient implements Client {
    private Connection connection;
    private InetSocketAddress localAddress;
    private InetSocketAddress remoteAddress;

    private volatile boolean isOpen = false;

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
        // 尝试连上远程
        connect();
        isOpen = true;
    }

    @Override
    public void connect() {
        connection = doConnect();
    }

    protected abstract Connection doConnect();

    @Override
    public void reConnect() {
        connection = doReConnect();
    }

    protected abstract Connection doReConnect();

    @Override
    public void close() {
        // 关闭连接
        if (isOpen) {
            connection.close();
            // 子类进行额外的关闭
            doClose();
        }
    }

    protected abstract void doClose();
}
