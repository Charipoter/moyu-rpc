package com.moyu.rpc.exchange.support.netty;

import com.moyu.rpc.exchange.ExchangeClient;
import com.moyu.rpc.exchange.Request;
import com.moyu.rpc.exchange.support.AbstractExchangeClient;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;

import java.net.InetSocketAddress;
import java.util.Scanner;

import static org.apache.dubbo.remoting.Constants.DEFAULT_CONNECT_TIMEOUT;
import static org.apache.dubbo.remoting.transport.netty4.NettyEventLoopFactory.socketChannelClass;

public class NettyExchangeClient extends AbstractExchangeClient {

    private Channel channel;
    private Bootstrap bootstrap;
    private final NioEventLoopGroup EVENT_LOOP_GROUP = new NioEventLoopGroup();

    public NettyExchangeClient(InetSocketAddress remoteAddress, int connectTimeout) {
        super(remoteAddress, connectTimeout);
    }


    @Override
    public void open() {
        final NettyClientHandler nettyClientHandler = new NettyClientHandler();
        bootstrap = new Bootstrap();
        bootstrap.group(EVENT_LOOP_GROUP)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                //.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, getTimeout())
                .channel(socketChannelClass());

        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.max(DEFAULT_CONNECT_TIMEOUT, getConnectTimeout()));
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {

            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                // 添加编解码器
                NettyCodec.apply(ch);

                ch.pipeline()
//                        .addLast("client-idle-handler", new IdleStateHandler(heartbeatInterval, 0, 0, MILLISECONDS))
                        .addLast("handler", nettyClientHandler);
            }
        });
    }

    @Override
    public void connect() {
        ChannelFuture future = bootstrap.connect(getRemoteAddress());
        future.awaitUninterruptibly(getConnectTimeout());
        channel = future.channel();
    }

    @Override
    public void close() {

    }

    @Override
    protected void doSend(Request request) {
        try {
            channel.writeAndFlush(request).sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        ExchangeClient client = new NettyExchangeClient(new InetSocketAddress("localhost", 8080),
                2000);

        client.open();
        client.connect();

        Scanner scanner = new Scanner(System.in);

        String msg;
        while (true) {
            msg = scanner.nextLine();
            if (msg.equals("quit")) {
                break;
            }
            client.send(msg);
        }

        client.close();
    }
}
