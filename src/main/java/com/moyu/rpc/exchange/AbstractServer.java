package com.moyu.rpc.exchange;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractServer implements Server {

    private static final int NEW = 0;
    private static final int OPEN = 1;
    private static final int CLOSE = -1;

    private final InetSocketAddress localAddress;
    /**
     * 持有的连接
     */
    private final Map<InetSocketAddress, Connection> connectionMap = new ConcurrentHashMap<>();
    /**
     * 服务器状态
     */
    private AtomicInteger state = new AtomicInteger(NEW);

    public AbstractServer(InetSocketAddress localAddress) {
        this.localAddress = localAddress;
    }

    @Override
    public CompletableFuture<Object> sendTo(Object message, InetSocketAddress address) {
        Connection connection = connectionMap.get(address);
        if (connection != null) {
            return connection.send(message);
        }
        throw new UnsupportedOperationException("不存在客户端");
    }

    @Override
    public CompletableFuture<Object> broadcast(Object message) {
        List<CompletableFuture<Object>> futures = connectionMap.values().stream().map(
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

    @Override
    public void open() {
        // TODO: 应该允许关闭后再开启吗?
        int s = state.get();
        if (s == OPEN) {

        } else if (s == NEW && state.compareAndSet(NEW, OPEN)) {
            doOpen();
        } else if (s == CLOSE && state.compareAndSet(CLOSE, OPEN)) {
            doOpen();
        } else {
            throw new RuntimeException("不存在的服务器状态");
        }
    }

    protected abstract void doOpen();

    @Override
    public void close() {
        int s = state.get();
        if (s != OPEN) {
            return;
        }
        // 如果失败了，说明有别的线程在负责关闭了
        if (state.compareAndSet(OPEN, CLOSE)) {
            // 关闭所有实际连接
            connectionMap.values().forEach(Connection::close);
            // 进一步关闭其他资源
            doClose();
        }

    }

    protected abstract void doClose();

    protected void addConnection(Connection connection) {
        connectionMap.put(connection.getRemoteAddress(), connection);
    }

    @Override
    public boolean isOpen() {
        return state.get() == OPEN;
    }

    @Override
    public boolean isClosed() {
        return state.get() == CLOSE;
    }
}
