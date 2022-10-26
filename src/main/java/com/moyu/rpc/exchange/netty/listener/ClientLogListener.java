package com.moyu.rpc.exchange.netty.listener;

import com.moyu.rpc.exchange.ConnectionListener;
import com.moyu.rpc.exchange.Message;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

@Slf4j
public class ClientLogListener implements ConnectionListener {

    @Override
    public void onConnected(InetSocketAddress remoteAddress) {
        log.info("已和 {} 建立了连接", remoteAddress);
    }

    @Override
    public void onReceived(Message received) {
        log.info("收到消息:{}", received);
    }

    @Override
    public void onDisConnected(InetSocketAddress remoteAddress) {
        log.info("已和 {} 断开了连接", remoteAddress);
    }

    @Override
    public void onException(Exception e) {
        log.warn("发生异常: {}", e.getMessage());
    }
}
