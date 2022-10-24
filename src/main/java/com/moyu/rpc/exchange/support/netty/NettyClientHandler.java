package com.moyu.rpc.exchange.support.netty;

import com.moyu.rpc.exchange.ExchangeHandler;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.net.InetSocketAddress;

public class NettyClientHandler extends ChannelDuplexHandler {

    private ExchangeHandler handler = new NettyExchangeHandler();

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        handler.connected((InetSocketAddress) ctx.channel().remoteAddress());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        handler.receive(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        handler.caught(cause);
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        // 尝试重连
    }
}
