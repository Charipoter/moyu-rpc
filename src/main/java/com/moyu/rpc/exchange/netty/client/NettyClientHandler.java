package com.moyu.rpc.exchange.netty.client;

import com.moyu.rpc.exchange.ListenableConnection;
import com.moyu.rpc.exchange.Message;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.Setter;

import java.net.InetSocketAddress;

@Getter
@Setter
public class NettyClientHandler extends ChannelDuplexHandler {

    private ListenableConnection connection;

    public NettyClientHandler() {

    }

    public NettyClientHandler(ListenableConnection connection) {
        this.connection = connection;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        connection.onReceived((Message) msg);

        ctx.fireChannelRead(msg);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        connection.onConnected((InetSocketAddress) ctx.channel().remoteAddress());

        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        connection.onDisConnected((InetSocketAddress) ctx.channel().remoteAddress());

        ctx.fireChannelInactive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        connection.onException((Exception) cause);
    }
}
