package com.moyu.rpc.Myexchange;

import lombok.Setter;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Setter
public abstract class AbstractConnection implements ListenableConnection {
    private InetSocketAddress sourceAddress;
    private InetSocketAddress targetAddress;

    private List<MessageListener> listeners = new ArrayList<>();

    public AbstractConnection(InetSocketAddress sourceAddress, InetSocketAddress targetAddress) {
        this.sourceAddress = sourceAddress;
        this.targetAddress = targetAddress;
    }

    @Override
    public CompletableFuture<Object> send(Object message) {
        // 放置一个 future
        MessageFuture future = MessageFuture.addFuture(message);
        doSend(future.getSent());
        return future;
    }

    @Override
    public void receive(Message received) {
        MessageFuture.receive(received);
        // 调用所有监听器
        listeners.forEach(listener -> listener.onReceive(received));
    }

    @Override
    public void addListener(MessageListener listener) {
        listeners.add(listener);
    }

    @Override
    public InetSocketAddress getSourceAddress() {
        return sourceAddress;
    }

    @Override
    public InetSocketAddress getTargetAddress() {
        return targetAddress;
    }

    protected abstract void doSend(Message sent);


}
