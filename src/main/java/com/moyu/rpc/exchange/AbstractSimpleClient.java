package com.moyu.rpc.exchange;

import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * 非线程安全的 client 抽象类
 */
@Slf4j
public abstract class AbstractSimpleClient extends AbstractClient {
    private volatile int state = NEW;

    public AbstractSimpleClient(InetSocketAddress remoteAddress) {
        super(remoteAddress);
    }

    /**
     * 这里的 open 就是进行 connect
     */
    @Override
    public void open() {
        if (state == NEW) {
            state = NOT_CONNECTED;
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
        if (state == NOT_CONNECTED) {
            // 由子类具体建立连接，null 说明创建失败
            try {
                clientConnection = doConnect();
            } catch (Exception e) {
                log.error(e.getMessage());
            }
            if (clientConnection != null && clientConnection.isActive()) {
                state = CONNECTED;
            } else {
                // 通过定时任务重试
                retryDelayConnect();
            }
        }
    }

    /**
     * 先断开连接再重连
     * 如果连接失败就延后重试
     */
    @Override
    public void reconnect() {

        if (state == CONNECTED) {
            // 先保证断连
            disconnect();
        }

        if (state == NOT_CONNECTED) {
            // 重连后对于具体的连接器可能有连接的变动，因此需要返回真正的连接
            clientConnection = doReconnect();
            if (clientConnection != null && clientConnection.isActive()) {
                state = CONNECTED;
            } else {
                retryDelayConnect();
            }
        } else if (state == CONNECTING) {
            // 此时正在尝试重连，已经准备好了基本资源
            // 重连后对于具体的连接器可能有连接的变动，因此需要返回真正的连接
            clientConnection = doReconnect();
            if (clientConnection != null && clientConnection.isActive()) {
                state = CONNECTED;
            } else {
                retryDelayConnect();
            }
        } else {
            throw new UnsupportedOperationException("不支持的客户端重连状态");
        }
    }

    /**
     * 尝试延后重连
     */
    private void retryDelayConnect() {
        if (state == CONNECTING || state == NOT_CONNECTED) {
            state = CONNECTING;
            // 如果重连失败，任务会自行投递，无需关心
            timer.schedule(new ReconnectTimerTask(), reconnectionInterval, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void disconnect() {
        if (state == CONNECTED) {
            // 由子类具体断开连接（主动断连不存在失败）
            doDisconnect();
            state = NOT_CONNECTED;
        }
    }

    @Override
    public void close() {
        if (state == CLOSE) {
            return;
        }

        int s = state;

        state = CLOSE;

        switch (s) {
            case CONNECTED, NOT_CONNECTED, CONNECTING -> {
                // 关闭连接
                clientConnection.close();
                // 关闭其他资源
                doClose();
            }
            case NEW -> {
                // 没有需要关闭的资源
            }
            default -> {}
        }
    }

    @Override
    public boolean isOpen() {
        return state != CLOSE;
    }

    @Override
    public boolean isClosed() {
        return state == CLOSE;
    }

    @Override
    public boolean isConnected() {
        return state == CONNECTED;
    }

    public class ReconnectTimerTask implements Runnable {
        @Override
        public void run() {
            if (state == CONNECTING) {
                log.info("尝试连接中...");
                // 重连后对于具体的连接器可能有连接的变动，因此需要返回真正的连接
                clientConnection = doReconnect();
                if (clientConnection != null && clientConnection.isActive()) {
                    state = CONNECTED;
                } else {
                    retryDelayConnect();
                }
            }
        }
    }
}
