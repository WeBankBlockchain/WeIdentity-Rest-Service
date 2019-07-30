package com.webank.weid.http.service.rpc;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.transport.AioSession;

public class RpcProcessor implements MessageProcessor<String> {

    private ExecutorService pool = Executors.newCachedThreadPool();

    @Override
    public void process(AioSession<String> session, String msg) {

    }

    @Override
    public void stateEvent(AioSession<String> session, StateMachineEnum stateMachineEnum,
        Throwable throwable) {

    }

}
