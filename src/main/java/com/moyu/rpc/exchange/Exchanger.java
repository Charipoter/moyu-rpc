package com.moyu.rpc.exchange;

import java.net.URL;

/**
 * 数据交换器，既可以是客户端也可以是服务端
 */
public interface Exchanger {
    /**
     * 作为客户端连接上某个服务端
     */
    ExchangeClient connect(URL url, ExchangeHandler handler);
    /**
     * 作为服务端监听端口
     */
    ExchangeServer bind(URL url, ExchangeHandler handler);

}
