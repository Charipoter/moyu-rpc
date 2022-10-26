package com.moyu.rpc.exchange;

import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程安全的 client 抽象类
 */
@Slf4j
public abstract class AbstractConcurrentClient extends AbstractClient {
    private AtomicInteger state = new AtomicInteger(NEW);

    public AbstractConcurrentClient(InetSocketAddress remoteAddress) {
        super(remoteAddress);
    }
    /**
     * 这里的 open 就是进行 connect
     */
    @Override
    public void open() {
        // 该方法只允许在初始化时调用一次
        if (state.get() == NEW && state.compareAndSet(NEW, NOT_CONNECTED)) {
            // 去建立连接
            connect();
        } else {
            log.info("client 的 open 方法只允许初始时调用一次");
        }
    }

    /**
     * 该方法只在初始时被调用一次，后续都将调用 reconnect
     * 连不上就重试
     */
    @Override
    public void connect() {
        // 只允许连接未建立状态调用该方法
        if (state.get() == NOT_CONNECTED) {
            synchronized (this) {
                if (state.get() == NOT_CONNECTED) {
                    // 由子类具体建立连接，null 说明创建失败
                    try {
                        clientConnection = doConnect();
                    } catch (Exception e) {
                        log.error(e.getMessage());
                    }
                    if (clientConnection != null && clientConnection.isActive()) {
                        if (!state.compareAndSet(NOT_CONNECTED, CONNECTED)) {
                            log.error("出现了不可预期的情况");
                        }
                    } else {
                        // 通过定时任务重试
                        retryDelayConnect();
                    }
                }
            }
        }
    }

    /**
     * 先断开连接再重连
     * 如果连接失败就延后重试
     */
    @Override
    public void reconnect() {

        if (state.get() == CONNECTED) {
            // 先保证断连
            disconnect();
        }

        if (state.get() == NOT_CONNECTED) {
            synchronized (this) {
                if (state.get() == NOT_CONNECTED) {
                    // 重连后对于具体的连接器可能有连接的变动，因此需要返回真正的连接
                    clientConnection = doReconnect();
                    if (clientConnection != null && clientConnection.isActive()) {
                        state.compareAndSet(NOT_CONNECTED, CONNECTED);
                    } else {
                        retryDelayConnect();
                    }
                }
            }
        } else if (state.get() == CONNECTING) {
            // 此时正在尝试重连，已经准备好了基本资源
            synchronized (this) {
                if (state.get() == CONNECTING) {
                    // 重连后对于具体的连接器可能有连接的变动，因此需要返回真正的连接
                    clientConnection = doReconnect();
                    if (clientConnection != null && clientConnection.isActive()) {
                        state.compareAndSet(CONNECTING, CONNECTED);
                    } else {
                        retryDelayConnect();
                    }
                }
            }
        } else {
            throw new UnsupportedOperationException("不支持的客户端重连状态");
        }
    }

    /**
     * 尝试延后重连
     */
    private void retryDelayConnect() {
        if (state.get() == CONNECTING || (state.get() == NOT_CONNECTED && state.compareAndSet(NOT_CONNECTED, CONNECTING))) {
            // 如果重连失败，任务会自行投递，无需关心
            timer.schedule(new ReconnectTimerTask(), reconnectionInterval, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void disconnect() {
        if (state.get() == CONNECTED) {
            synchronized (this) {
                if (state.get() == CONNECTED) {
                    // 由子类具体断开连接（主动断连不存在失败）
                    doDisconnect();
                    state.compareAndSet(CONNECTED, NOT_CONNECTED);
                }
            }
        }
    }

    @Override
    public void close() {
        if (state.get() == CLOSE) {
            return;
        }
        synchronized (this) {
            for (;;) {
                int s = state.get();
                if (s != CLOSE) {
                    boolean advance = false;

                    if (s == NEW && state.compareAndSet(NEW, CLOSE)) {
                        // 资源都未创建，无需关闭
                    } else if (s == CONNECTED && state.compareAndSet(CONNECTED, CLOSE)) {
                        // 关闭连接
                        clientConnection.close();
                        // 关闭其他资源
                        doClose();
                    } else if (s == NOT_CONNECTED && state.compareAndSet(NOT_CONNECTED, CLOSE)) {
                        // 建立了连接但中途被关闭了
                        clientConnection.close();
                        doClose();
                    } else if (s == CONNECTING && state.compareAndSet(CONNECTING, CLOSE)) {
                        // 对于 connecting 情况，标志位一改任务便会结束，但有可能 CAS 失败，需要重试
                        clientConnection.close();
                        doClose();
                    } else {
                        advance = true;
                    }
                    if (!advance) {
                        break;
                    }
                }
            }
        }

    }

    @Override
    public boolean isOpen() {
        return state.get() != CLOSE;
    }

    @Override
    public boolean isClosed() {
        return state.get() == CLOSE;
    }

    @Override
    public boolean isConnected() {
        return state.get() == CONNECTED;
    }

    public class ReconnectTimerTask implements Runnable {
        @Override
        public void run() {
            if (state.get() == CONNECTING) {
                log.info("尝试连接中...");
                synchronized (this) {
                    if (state.get() == CONNECTING) {
                        // 重连后对于具体的连接器可能有连接的变动，因此需要返回真正的连接
                        clientConnection = doReconnect();
                        if (clientConnection != null && clientConnection.isActive()) {
                            state.compareAndSet(CONNECTING, CONNECTED);
                        } else {
                            retryDelayConnect();
                        }
                    }
                }
            }
        }
    }
}
