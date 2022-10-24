package com.moyu.rpc.Myexchange.netty;

import com.moyu.rpc.Myexchange.AbstractClient;
import com.moyu.rpc.Myexchange.Client;
import com.moyu.rpc.Myexchange.Connection;
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

        // 创建 handler
        NettyHandler handler = new NettyHandler();
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {

            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                // 添加编解码器
                NettyCodec.apply(ch);

                ch.pipeline()
//                        .addLast("client-idle-handler", new IdleStateHandler(heartbeatInterval, 0, 0, MILLISECONDS))
                        .addLast("handler", handler);
            }
        });
        bootstrap.remoteAddress(getRemoteAddress());
        ChannelFuture future = bootstrap.connect();

        Connection connection = new NettyConnection(future.channel());
        handler.setConnection(connection);
        setLocalAddress(connection.getSourceAddress());

        future.syncUninterruptibly();

        return connection;
    }

    @Override
    protected Connection doReConnect() {
        if (!isOpen()) {
            return doConnect();
        }
        throw new UnsupportedOperationException("客户端未关闭");
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

        while (true) {
            String msg = scanner.nextLine();
            client.send(msg);
        }
    }
}
