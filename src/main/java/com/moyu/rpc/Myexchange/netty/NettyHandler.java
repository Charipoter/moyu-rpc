package com.moyu.rpc.Myexchange.netty;

import com.moyu.rpc.Myexchange.Connection;
import com.moyu.rpc.Myexchange.Message;
import com.moyu.rpc.Myexchange.MessageHandler;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.Setter;

@Setter
public class NettyHandler extends ChannelDuplexHandler implements MessageHandler {

    private Connection connection;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Message message = (Message) msg;
        handleReceived(message);
    }

    @Override
    public void handleReceived(Message received) {
        System.out.println(received);
        connection.receive(received);
    }
}
