package com.moyu.rpc.exchange.netty.client;

import com.moyu.rpc.exchange.AbstractSimpleClient;
import com.moyu.rpc.exchange.Client;
import com.moyu.rpc.exchange.Connection;
import com.moyu.rpc.exchange.netty.NettyCodec;
import com.moyu.rpc.exchange.netty.NettyConnection;
import com.moyu.rpc.exchange.netty.listener.ClientLogListener;
import com.moyu.rpc.exchange.netty.listener.ReconnectListener;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Scanner;

@Slf4j
public class SimpleNettyClient extends AbstractSimpleClient {

    private Bootstrap bootstrap;

    private NioEventLoopGroup workGroup;

    public SimpleNettyClient(InetSocketAddress remoteAddress) {
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
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, getConnectionTimeout())
                .channel(NioSocketChannel.class);

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
        // 保证有一个非 null 的连接，不管有不有效
        setClientConnection(connection);
        // 获取被分配的地址
        setLocalAddress(connection.getLocalAddress());
        // 添加客户端需要的用于基本处理的监听器
        connection.addListener(new ClientLogListener());
        connection.addListener(new ReconnectListener(this));

        future.awaitUninterruptibly(getConnectionTimeout());

        return connection;
    }

    @Override
    protected Connection doReconnect() {
        // 新创建一个 channel 尝试连接
        ChannelFuture future = bootstrap.connect();

        future.awaitUninterruptibly(getConnectionTimeout());

        NettyConnection connection = (NettyConnection) getClientConnection();

        if (connection == null) {
            throw new RuntimeException("connection 为 null，业务错误");
        }
        // 重连后 channel 会变
        connection.setChannel(future.channel());

        return connection;
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
        Client client = new SimpleNettyClient(new InetSocketAddress("localhost", 8080));
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
