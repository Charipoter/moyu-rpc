package com.moyu.rpc.exchange.support.netty;

import com.moyu.rpc.exchange.ExchangeHandler;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;

public class NettyServerHandler extends ChannelDuplexHandler {

    private ExchangeHandler handler = new NettyExchangeHandler();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        handler.receive(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        handler.caught(cause);
    }
}
