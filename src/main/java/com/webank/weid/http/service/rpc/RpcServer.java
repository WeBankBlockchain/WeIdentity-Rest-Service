package com.webank.weid.http.service.rpc;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.StringUtils;
import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.transport.AioQuickServer;
import org.smartboot.socket.transport.AioSession;

import com.webank.weid.http.constant.WeIdentityServiceEndpoint;
import com.webank.weid.http.protocol.response.EndpointInfo;
import com.webank.weid.http.util.EndpointDataUtil;
import com.webank.weid.util.DataToolUtils;

/**
 * A sample RPC Server implementation, kept mainstream-same as in Java-SDK.
 */
@Deprecated
public class RpcServer {

    private static final Integer LISTENER_PORT = 6090;

    public static void main(String[] args) throws Exception {
        ExecutorService pool = Executors.newCachedThreadPool();
        EndpointDataUtil.loadAllEndpointInfoFromProps();
        AioQuickServer<String> server = new AioQuickServer<String>(LISTENER_PORT,
            new StringProtocol(),
            new MessageProcessor<String>() {
                public void process(AioSession<String> session, String msg) {
                    pool.execute(() -> {
                        String uuid = msg.substring(msg.length() - 36);
                        System.out.println(
                            "RpcServer thread: " + Thread.currentThread().getId() + Thread
                                .currentThread().getName() + ", received msg: " + msg
                                + ", extracted UUID: " + uuid + ", session ID: " + session
                                .getSessionID());
                        String bizResult = processClientMessage(msg);
                        System.out.println(
                            Thread.currentThread().getId() + ", " + Thread.currentThread()
                                .getName());
                        // simulate biz execution lag
                        try {
                            Thread.sleep((int) (Math.random() * 1000));
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }
                        String reply =
                            bizResult + WeIdentityServiceEndpoint.EPS_SEPARATOR + uuid;
                        byte[] response = reply.getBytes();
                        byte[] head = {(byte) response.length};
                        synchronized (session) {
                            try {
                                session.writeBuffer().write(head);
                                session.writeBuffer().write(response);
                                session.writeBuffer().flush();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }

                public void stateEvent(AioSession<String> session,
                    StateMachineEnum stateMachineEnum, Throwable throwable) {
                }
            });
        server.setBossThreadNum(15);
        server.setWorkerThreadNum(20);
        server.start();
    }

    private static String processClientMessage(String msg) {
        String[] clientMsgArray = StringUtils
            .splitByWholeSeparator(msg, WeIdentityServiceEndpoint.EPS_SEPARATOR);
        String requestName = clientMsgArray[0];
        // Check if this is built-in methods
        if (requestName.equalsIgnoreCase(WeIdentityServiceEndpoint.FETCH_FUNCTION)) {
            return processFetch();
        }
        // See implementations in WeID-Java-SDK
        return StringUtils.EMPTY;
    }

    private static String processFetch() {
        List<EndpointInfo> endpointInfoList = EndpointDataUtil.getAllEndpointInfo();
        String reply = StringUtils.EMPTY;
        for (EndpointInfo endpointInfo : endpointInfoList) {
            reply += DataToolUtils.serialize(endpointInfo);
            reply += "```";
        }
        if (!StringUtils.isEmpty(reply)) {
            reply = reply.substring(0, reply.length() - 3);
        }
        return reply;
    }
}
