package com.moyu.rpc.Myexchange;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractServer implements Server {

    private final InetSocketAddress localAddress;
    /**
     * 持有的连接
     */
    private final Map<InetSocketAddress, Connection> connectionMap = new ConcurrentHashMap<>();

    private volatile boolean isOpen = false;

    public AbstractServer(InetSocketAddress localAddress) {
        this.localAddress = localAddress;
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    @Override
    public void open() {
        doOpen();
        isOpen = true;
    }

    protected abstract void doOpen();

    @Override
    public void close() {
        connectionMap.values().forEach(Connection::close);
        doClose();
        isOpen = false;
    }

    protected abstract void doClose();

    protected void addConnection(Connection connection) {
        connectionMap.put(connection.getTargetAddress(), connection);
    }
}
