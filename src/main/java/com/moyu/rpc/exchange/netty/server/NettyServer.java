package com.moyu.rpc.exchange.netty.server;

import com.moyu.rpc.exchange.AbstractServer;
import com.moyu.rpc.exchange.Server;
import com.moyu.rpc.exchange.netty.NettyCodec;
import com.moyu.rpc.exchange.netty.NettyConnection;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;

public class NettyServer extends AbstractServer {

    private ServerBootstrap bootstrap;
    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workGroup;
    private NettyConnection connection;

    public NettyServer(InetSocketAddress localAddress) {
        super(localAddress);
    }

    @Override
    protected void doOpen() {
        bossGroup = new NioEventLoopGroup(1);
        workGroup = new NioEventLoopGroup();
        bootstrap = new ServerBootstrap();

        connection = new NettyConnection();
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
                        NettyServerHandler handler = new NettyServerHandler();
                        handler.setConnection(connection);
                        ch.pipeline()
//                                .addLast("server-idle-handler", new IdleStateHandler(0, 0, idleTimeout, MILLISECONDS))
                                .addLast("handler", handler);
                    }
                });

        ChannelFuture future = bootstrap.bind(getLocalAddress());

        connection.setChannel(future.channel());
        connection.setTargetAddress(getLocalAddress());
        addConnection(connection);

        connection.addListener(new NettyServerListener());

        future.syncUninterruptibly();
    }

    @Override
    protected void doClose() {
        bossGroup.shutdownGracefully();
        workGroup.shutdownGracefully();
    }

    public static void main(String[] args) {
        Server server = new NettyServer(new InetSocketAddress("localhost", 8080));
        server.open();
    }
}
