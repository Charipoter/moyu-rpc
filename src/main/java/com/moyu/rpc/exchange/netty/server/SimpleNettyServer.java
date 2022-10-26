package com.moyu.rpc.exchange.netty.server;

import com.moyu.rpc.exchange.AbstractSimpleServer;
import com.moyu.rpc.exchange.Server;
import com.moyu.rpc.exchange.netty.NettyCodec;
import com.moyu.rpc.exchange.netty.NettyConnection;
import com.moyu.rpc.exchange.netty.listener.ServerLogListener;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;
import java.util.Scanner;

public class SimpleNettyServer extends AbstractSimpleServer {
    private ServerBootstrap bootstrap;
    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workGroup;


    public SimpleNettyServer(InetSocketAddress localAddress) {
        super(localAddress);
    }

    @Override
    protected void doOpen() {

        bossGroup = new NioEventLoopGroup(1);
        workGroup = new NioEventLoopGroup();
        bootstrap = new ServerBootstrap();

        NettyConnection connection = new NettyConnection();
        NettyServerHandler handler = new NettyServerHandler();
        handler.setConnection(connection);
        handler.setServer(this);

        bootstrap.group(bossGroup, workGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        // 添加编解码器
                        NettyCodec.apply(ch);
                        ch.pipeline()
//                                .addLast("server-idle-handler", new IdleStateHandler(0, 0, idleTimeout, MILLISECONDS))
                                .addLast("handler", handler);
                    }
                });

        ChannelFuture future = bootstrap.bind(getLocalAddress());

        connection.setChannel(future.channel());
        connection.setTargetAddress(getLocalAddress());
        connection.addListener(new ServerLogListener());
        setServerConnection(connection);

        future.syncUninterruptibly();
    }

    @Override
    protected void doClose() {
        bossGroup.shutdownGracefully();
        workGroup.shutdownGracefully();
    }

    public static void main(String[] args) {
        Server server = new SimpleNettyServer(new InetSocketAddress("localhost", 8080));
        server.open();

        Scanner scanner = new Scanner(System.in);
        for (;;) {
            String msg = scanner.nextLine();
            if (msg.equals("quit")) {
                server.close();
                break;
            }
            server.broadcast(msg);
        }
    }
}
