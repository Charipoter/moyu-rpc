package com.moyu.rpc.exchange.netty.server;

import com.moyu.rpc.exchange.ListenableConnection;
import com.moyu.rpc.exchange.Message;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.Setter;

import java.net.InetSocketAddress;

@Getter
@Setter
public class NettyServerHandler extends ChannelDuplexHandler {

    private ListenableConnection connection;

    public NettyServerHandler() {

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        connection.onReceived((Message) msg);
        // 返还消息
        ctx.writeAndFlush(msg);
        ctx.fireChannelRead(msg);
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        connection.onConnected((InetSocketAddress) ctx.channel().remoteAddress());

        ctx.fireChannelRegistered();
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        connection.onDisConnected((InetSocketAddress) ctx.channel().remoteAddress());

        ctx.fireChannelUnregistered();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        connection.onException((Exception) cause);
    }
}
