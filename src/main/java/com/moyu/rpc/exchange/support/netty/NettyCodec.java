package com.moyu.rpc.exchange.support.netty;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.List;

public class NettyCodec {

    private static final Encoder encoder = new Encoder();
    private static final Decoder decoder = new Decoder();

    public static class Encoder extends MessageToByteEncoder<Object> {

        @Override
        protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
            out.writeBytes(JSON.toJSONString(msg).getBytes());
        }
    }

    public static class Decoder extends ByteToMessageDecoder {

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
            byte[] bytes = new byte[in.readableBytes()];
            in.readBytes(bytes);
            out.add(JSON.parseObject(bytes, Object.class));
        }
    }

    public static void apply(Channel channel) {
        channel.pipeline()
                .addLast(encoder)
                .addLast(decoder);
    }

}
