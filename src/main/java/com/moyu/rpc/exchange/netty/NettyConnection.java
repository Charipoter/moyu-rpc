package com.moyu.rpc.exchange.netty;

import com.moyu.rpc.exchange.AbstractConnection;
import com.moyu.rpc.exchange.Message;
import io.netty.channel.Channel;

import java.net.InetSocketAddress;

/**
 * netty 的连接，使用 channel 来代表
 */
public class NettyConnection extends AbstractConnection {

    private final Channel channel;

    public NettyConnection(Channel channel) {
        super((InetSocketAddress) channel.localAddress(), (InetSocketAddress) channel.remoteAddress());
        this.channel = channel;
    }

    @Override
    public void close() {
        // 等待关闭
        channel.close().syncUninterruptibly();
    }

    @Override
    protected void doSend(Message sent) {
        // 等待发送出去
        channel.writeAndFlush(sent).syncUninterruptibly();
    }
}
