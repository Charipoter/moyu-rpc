package com.moyu.rpc.exchange.netty.client;

import com.moyu.rpc.exchange.AbstractClient;
import com.moyu.rpc.exchange.Client;
import com.moyu.rpc.exchange.Connection;
import com.moyu.rpc.exchange.netty.NettyCodec;
import com.moyu.rpc.exchange.netty.NettyConnection;
import com.moyu.rpc.exchange.netty.RetryConnectionListener;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class NettyClient extends AbstractClient {

    private Bootstrap bootstrap;

    private NioEventLoopGroup workGroup;

    public NettyClient(InetSocketAddress remoteAddress) {
        super(remoteAddress);
    }

    @Override
    protected Connection doConnect() {
        workGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(workGroup)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                //.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, getTimeout())
                .channel(NioSocketChannel.class);

//        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.max(DEFAULT_CONNECT_TIMEOUT, getConnectTimeout()));

        NettyConnection connection = new NettyConnection();
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                // 添加编解码器
                NettyCodec.apply(ch);
                NettyClientHandler handler = new NettyClientHandler();
                handler.setConnection(connection);
                ch.pipeline()
//                        .addLast("client-idle-handler", new IdleStateHandler(heartbeatInterval, 0, 0, MILLISECONDS))
                        .addLast("handler", handler);
            }
        });
        bootstrap.remoteAddress(getRemoteAddress());
        ChannelFuture future = bootstrap.connect();


        connection.setChannel(future.channel());
        setConnection(connection);
        setLocalAddress(connection.getLocalAddress());
        // 添加客户端需要的用于基本处理的监听器
        connection.addListener(new NettyClientListener());
        connection.addListener(new RetryConnectionListener(this));

        future.syncUninterruptibly();

        return future.isSuccess() ? connection : null;
    }

    @Override
    protected Connection doReconnect() {
        ChannelFuture future = bootstrap.connect();

        future.awaitUninterruptibly(3, TimeUnit.SECONDS);

        if (future.isSuccess()) {
            // 重连后 channel 会变
            NettyConnection connection = (NettyConnection) getConnection();
            connection.setChannel(future.channel());
        }

        return future.isSuccess() ? getConnection() : null;
    }

    @Override
    protected void doDisconnect() {

    }

    @Override
    protected void doClose() {
        // 关闭线程池
        workGroup.shutdownGracefully().syncUninterruptibly();
    }

    public static void main(String[] args) {
        Client client = new NettyClient(new InetSocketAddress("localhost", 8080));
        client.open();

        Scanner scanner = new Scanner(System.in);
        for (;;) {
            String msg = scanner.nextLine();
            if (msg.equals("quit")) {
                client.close();
                break;
            }
            client.send(msg);
        }
    }
}
