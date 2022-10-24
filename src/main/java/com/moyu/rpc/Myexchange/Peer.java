package com.moyu.rpc.Myexchange;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 网络中的一个对等点，既可以是客户端也可以是服务端
 */
public interface Peer {
    /**
     * 获取与该点建立了连接的点
     */
    List<InetSocketAddress> getConnectedAddresses();
    /**
     * 发送数据给某个远程 url
     * 如果没建立过连接，则建立
     */
    CompletableFuture<Object> send(Object message, InetSocketAddress address);
    /**
     * 发送数据给所有建立了连接的节点
     */
    CompletableFuture<Object> broadcast(Object message);
    /**
     * 与某个节点建立连接
     */
    void openConnection(InetSocketAddress address);
    /**
     * 如果建立过连接，则断开该连接
     */
    void closeConnection(InetSocketAddress address);

}
