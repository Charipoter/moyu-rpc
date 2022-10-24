package com.moyu.rpc.Myexchange.netty;

import com.alibaba.fastjson.JSON;
import com.moyu.rpc.Myexchange.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.List;

public class NettyCodec {

    private static final Encoder encoder = new Encoder();
    private static final Decoder decoder = new Decoder();

    public static class Encoder extends MessageToByteEncoder<Message> {

        @Override
        protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception {
            out.writeBytes(JSON.toJSONString(msg).getBytes());
        }
    }

    public static class Decoder extends ByteToMessageDecoder {

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
            byte[] bytes = new byte[in.readableBytes()];
            in.readBytes(bytes);
            Message received = JSON.parseObject(bytes, Message.class);
            out.add(received);
        }
    }

    public static void apply(Channel channel) {
        channel.pipeline()
                .addLast(encoder)
                .addLast(decoder);
    }

}
