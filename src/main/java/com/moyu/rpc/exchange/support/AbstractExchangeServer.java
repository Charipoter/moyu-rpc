package com.moyu.rpc.exchange.support;

import com.moyu.rpc.exchange.ExchangeServer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.net.InetSocketAddress;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public abstract class AbstractExchangeServer implements ExchangeServer {

    private InetSocketAddress serverAddress;

}
