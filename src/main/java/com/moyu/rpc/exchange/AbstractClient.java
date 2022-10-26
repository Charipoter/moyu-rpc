package com.moyu.rpc.exchange;

import com.moyu.rpc.timer.Timer;
import com.moyu.rpc.timer.support.TimeWheelTimer;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * client 基本抽象类
 */
@Slf4j
@Getter
@Setter
public abstract class AbstractClient implements Client {
    /**
     * 初始状态，该状态之后不会再被到达
     */
    protected static final int NEW = 0;
    /**
     * 正在连接中
     */
    protected static final int CONNECTING = 1;
    /**
     * 未建立连接，该状态可多次到达
     */
    protected static final int NOT_CONNECTED = 2;
    /**
     * 连接已建立
     */
    protected static final int CONNECTED = 3;
    /**
     * 客户端已关闭，不再提供服务
     */
    protected static final int CLOSE = -1;

    protected Connection clientConnection;
    protected InetSocketAddress localAddress;

    protected InetSocketAddress remoteAddress;
    /**
     * 如果连接失败了，等多久再重试，毫秒单位
     */
    protected int reconnectionInterval = 1000;
    /**
     * 连接时间限制，毫秒单位
     */
    protected int connectionTimeout = 3000;

    protected final Timer timer = new TimeWheelTimer(1, TimeUnit.SECONDS);

    public AbstractClient(InetSocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    @Override
    public CompletableFuture<Object> send(Object message) {
        return clientConnection.send(message);
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    protected abstract Connection doConnect();

    protected abstract Connection doReconnect();

    protected abstract void doDisconnect();

    protected abstract void doClose();
}
