package com.moyu.rpc.exchange.support.netty;

import com.moyu.rpc.exchange.support.AbstractExchangeServer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import lombok.NoArgsConstructor;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;

@NoArgsConstructor
public class NettyExchangeServer extends AbstractExchangeServer {

    private Channel channel;
    private ServerBootstrap bootstrap;
    private final NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private final NioEventLoopGroup workGroup = new NioEventLoopGroup();

    public NettyExchangeServer(InetSocketAddress serverAddress) {
        super(serverAddress);
    }


    @Override
    public void open() {
        final NettyServerHandler nettyServerHandler = new NettyServerHandler();
        bootstrap = new ServerBootstrap();

        bootstrap.group(bossGroup, workGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        // FIXME: should we use getTimeout()?
//                        int idleTimeout = UrlUtils.getIdleTimeout(getUrl());
                        ch.pipeline()
                                // TODO: 添加编解码器
                                .addLast("decoder", new StringDecoder())
                                .addLast("encoder", new StringEncoder())
//                                .addLast("server-idle-handler", new IdleStateHandler(0, 0, idleTimeout, MILLISECONDS))
                                .addLast("handler", nettyServerHandler);
                    }
                });

        ChannelFuture channelFuture = bootstrap.bind(getServerAddress());
        channelFuture.syncUninterruptibly();
        channel = channelFuture.channel();
    }

    @Override
    public void close() {

    }

    public static void main(String[] args) throws MalformedURLException {
        NettyExchangeServer server = new NettyExchangeServer(
                new InetSocketAddress("localhost", 8080));
        server.open();
    }
}
