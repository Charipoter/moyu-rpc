package com.moyu.rpc.exchange.netty;

import com.moyu.rpc.exchange.AbstractListener;
import com.moyu.rpc.exchange.netty.client.NettyClient;
import com.moyu.rpc.timer.Timer;
import com.moyu.rpc.timer.support.TimeWheelTimer;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RetryConnectionListener extends AbstractListener {

    private NettyClient client;

    private Timer timer = new TimeWheelTimer(1, TimeUnit.SECONDS);

    public RetryConnectionListener(NettyClient client) {
        this.client = client;
    }

    @Override
    public void onDisConnected(InetSocketAddress remoteAddress) {
        ReconnectTask task = new ReconnectTask(timer);
        // 3 秒后尝试重连
        timer.schedule(task, 3, TimeUnit.SECONDS);

    }

    private class ReconnectTask implements Runnable {

        private Timer executor;
        ReconnectTask(Timer timer) {
            this.executor = timer;
        }
        @Override
        public void run() {
            try {
                log.info("尝试重连...");
                client.reconnect();
            } catch (Exception e) {

            }

            if (!client.isConnected()) {
                log.info("重连失败，3 秒后再次尝试");
                timer.schedule(this, 3, TimeUnit.SECONDS);
            }
        }
    }
}
