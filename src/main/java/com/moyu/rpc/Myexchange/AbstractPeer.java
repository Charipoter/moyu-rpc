package com.moyu.rpc.Myexchange;


import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractPeer implements Peer {

    /**
     * 维护连接的数据结构
     */
    private final Map<InetSocketAddress, Connection> urlToConnection = new ConcurrentHashMap<>();

    @Override
    public List<InetSocketAddress> getConnectedAddresses() {
        return urlToConnection.keySet().stream().toList();
    }

    @Override
    public CompletableFuture<Object> send(Object message, InetSocketAddress address) {
        // 找到现有的连接
        Connection connection;

        while ((connection = urlToConnection.get(address)) == null) {
            openConnection(address);
        }

        return connection.send(message);
    }

    @Override
    public CompletableFuture<Object> broadcast(Object message) {
        List<CompletableFuture<Object>> futures = urlToConnection.values().stream().map(
                connection -> connection.send(message)
        ).toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenApply(
                r -> futures.stream()
                        .map(future -> future.getNow(null))
                        .allMatch(Objects::nonNull)
        );
    }

    @Override
    public void openConnection(InetSocketAddress address) {
        Connection connection = doOpenConnection(address);
        urlToConnection.put(address, connection);
    }

    protected abstract Connection doOpenConnection(InetSocketAddress address);

    @Override
    public void closeConnection(InetSocketAddress address) {
        Connection connection = urlToConnection.remove(address);
        doCloseConnection(connection);
    }

    protected abstract void doCloseConnection(Connection connection);
}
