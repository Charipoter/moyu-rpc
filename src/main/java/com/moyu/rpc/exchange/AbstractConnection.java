package com.moyu.rpc.exchange;

import lombok.Getter;
import lombok.Setter;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Setter
@Getter
public abstract class AbstractConnection implements ListenableConnection {
    private InetSocketAddress sourceAddress;
    private InetSocketAddress targetAddress;

    private List<ConnectionListener> listeners = new ArrayList<>();

    public AbstractConnection() {

    }

    public AbstractConnection(InetSocketAddress sourceAddress, InetSocketAddress targetAddress) {
        this.sourceAddress = sourceAddress;
        this.targetAddress = targetAddress;
    }

    @Override
    public CompletableFuture<Object> send(Object message) {
        // 放置一个 future
        MessageFuture future = MessageFuture.addFuture(message);
        doSend(future.getSent());
        // 调用监听器
        onSent(future.getSent());
        return future;
    }

    @Override
    public void receive(Message received) {
        MessageFuture.receive(received);
        onReceived(received);
    }

    @Override
    public void addListener(ConnectionListener listener) {
        listeners.add(listener);
    }

    @Override
    public void onDisConnected(InetSocketAddress remoteAddress) {
        listeners.forEach(listener -> listener.onDisConnected(remoteAddress));
    }

    @Override
    public void onConnected(InetSocketAddress remoteAddress) {
        listeners.forEach(listener -> listener.onConnected(remoteAddress));
    }

    @Override
    public void onReceived(Message received) {
        listeners.forEach(listener -> listener.onReceived(received));
    }

    @Override
    public void onSent(Message sent) {
        listeners.forEach(listener -> listener.onSent(sent));
    }
    @Override
    public void onOpen() {
        listeners.forEach(ConnectionListener::onOpen);
    }

    @Override
    public void onException(Exception e) {
        listeners.forEach(listener -> listener.onException(e));
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return sourceAddress;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return targetAddress;
    }

    protected abstract void doSend(Message sent);


}
