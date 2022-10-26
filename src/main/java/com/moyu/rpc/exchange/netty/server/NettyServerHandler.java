package com.moyu.rpc.exchange.netty.server;

import com.moyu.rpc.exchange.AbstractServer;
import com.moyu.rpc.exchange.ListenableConnection;
import com.moyu.rpc.exchange.Message;
import com.moyu.rpc.exchange.Server;
import com.moyu.rpc.exchange.netty.NettyConnection;
import com.moyu.rpc.exchange.netty.listener.ServerLogListener;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.Setter;

import java.net.InetSocketAddress;

@Getter
@Setter
public class NettyServerHandler extends ChannelDuplexHandler {

    private ListenableConnection connection;

    private Server server;

    public NettyServerHandler() {

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        connection.onReceived((Message) msg);

        ctx.fireChannelRead(msg);
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        // 将连接放入 map
        NettyConnection newConnection = new NettyConnection();
        newConnection.setTargetAddress(remoteAddress);
        newConnection.addListener(new ServerLogListener());
        newConnection.setChannel(ctx.channel());
        server.addConnection(newConnection);

        connection.onConnected(remoteAddress);

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
