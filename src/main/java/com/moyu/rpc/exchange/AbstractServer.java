package com.moyu.rpc.exchange;

import lombok.Getter;
import lombok.Setter;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * server 基本抽象类
 */
@Getter
@Setter
public abstract class AbstractServer implements Server {

    protected static final int NEW = 0;
    protected static final int OPEN = 1;
    protected static final int CLOSE = -1;

    protected final InetSocketAddress localAddress;
    /**
     * 持有的与客户端的连接
     */
    protected final Map<InetSocketAddress, Connection> clientConnectionMap = new ConcurrentHashMap<>();
    /**
     * 自己持有的连接
     */
    protected ListenableConnection serverConnection;

    public AbstractServer(InetSocketAddress localAddress) {
        this.localAddress = localAddress;
    }

    @Override
    public CompletableFuture<Object> sendTo(Object message, InetSocketAddress address) {
        Connection connection = clientConnectionMap.get(address);
        if (connection != null) {
            return connection.send(message);
        }
        throw new UnsupportedOperationException("不存在客户端");
    }

    @Override
    public CompletableFuture<Object> broadcast(Object message) {
        List<CompletableFuture<Object>> futures = clientConnectionMap.values().stream().map(
                connection -> connection.send(message)
        ).toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenApply(
                r -> futures.stream()
                        .map(future -> future.getNow(null))
                        .allMatch(Objects::nonNull)
        );
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }
    protected abstract void doOpen();
    protected abstract void doClose();

    public void addConnection(Connection connection) {
        clientConnectionMap.put(connection.getRemoteAddress(), connection);
    }

}
