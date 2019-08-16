/*
 *       Copyright© (2019) WeBank Co., Ltd.
 *
 *       This file is part of weid-http-service.
 *
 *       weid-http-service is free software: you can redistribute it and/or modify
 *       it under the terms of the GNU Lesser General Public License as published by
 *       the Free Software Foundation, either version 3 of the License, or
 *       (at your option) any later version.
 *
 *       weid-http-service is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU Lesser General Public License for more details.
 *
 *       You should have received a copy of the GNU Lesser General Public License
 *       along with weid-http-service.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.webank.weid.http.service.rpc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioSession;

import com.webank.weid.http.constant.HttpReturnCode;
import com.webank.weid.http.constant.WeIdentityServiceEndpoint;
import com.webank.weid.http.protocol.response.HttpResponseData;


public class RpcClient {

    private static final Logger logger = LoggerFactory.getLogger(RpcClient.class);

    private static final int STATE_PENDING = 0;
    private static final int STATE_RECEIVED = 1;
    private static final int STATE_TIMEOUT = 2;
    private static final int STATE_FAILED = 3;
    private static final int POLL_INTERNAL_MILLIS = 500;
    private static final int MAX_RETRIES = 20;

    // The results and states per uuid w.r.t session.
    private AioSession<String> session = null;
    private Map<String, String> resultMap = new ConcurrentHashMap<>();
    private Map<String, Integer> stateMap = new ConcurrentHashMap<>();

    // The host/port information for this client.
    @Getter
    @Setter
    private String host;
    @Getter
    @Setter
    private int port;

    /**
     * Constructor with supplied host and port String.
     */
    protected RpcClient(String hostport) throws Exception {
        setHostPort(hostport);
        session = getNewSession(this.host, this.port);
    }

    /**
     * Enforce a client re-connect based on pre-set host and port.
     */
    protected void reconnect() throws Exception {
        if (isValid()) {
            session.close();
        }
        session = getNewSession(host, port);
    }

    /**
     * Return the validity of this client.
     */
    protected boolean isValid() {
        return (session != null) && !session.isInvalid();
    }

    /**
     * Send the message to RPC server.
     */
    protected HttpResponseData<String> send(String msg) {
        String uuid = UUID.randomUUID().toString();
        String message = msg + WeIdentityServiceEndpoint.EPS_SEPARATOR + uuid;
        System.out.println("Sending msg: " + message + session.getSessionID());
        ByteBuffer byteBuffer = FixedLengthProtocol.encode(message);
        byte[] resp = new byte[byteBuffer.remaining()];
        byteBuffer.get(resp, 0, resp.length);
        resultMap.put(uuid, StringUtils.EMPTY);
        stateMap.put(uuid, STATE_PENDING);
        try {
            session.writeBuffer().write(resp);
            session.writeBuffer().flush();
        } catch (IOException e) {
            stateMap.put(uuid, STATE_FAILED);
            logger.error("Failed to send message. ", e.getMessage());
            return new HttpResponseData<>(StringUtils.EMPTY,
                HttpReturnCode.RPC_SEND_FAIL.getCode(), e.getMessage());
        }
        return new HttpResponseData<>(uuid, HttpReturnCode.SUCCESS);
    }

    /**
     * Periodically fetch the result from the given UUID.
     */
    protected HttpResponseData<String> get(String uuid) throws Exception {
        for (int i = 0; i < MAX_RETRIES; i++) {
            if (stateMap.get(uuid) == STATE_RECEIVED) {
                return new HttpResponseData<>(resultMap.get(uuid), HttpReturnCode.SUCCESS);
            }
            Thread.sleep(POLL_INTERNAL_MILLIS);
        }
        stateMap.put(uuid, STATE_TIMEOUT);
        return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.RPC_FETCH_FAIL);
    }

    /**
     * Close the RPC connection. Can be re-opened via the reconnect() function.
     */
    protected void close() {
        session.close();
    }

    private AioSession<String> getNewSession(String host, int port)
        throws InterruptedException, ExecutionException, IOException {
        AioQuickClient<String> client = new AioQuickClient<String>(host, port,
            new FixedLengthProtocol(), new MessageProcessor<String>() {
            public void process(AioSession<String> session, String msg) {
                System.out.println("received msg: " + msg + session.getSessionID());
                String uuid = msg.substring(msg.length() - 36);
                resultMap.put(uuid, msg.substring(0, msg.length() - 39));
                stateMap.put(uuid, STATE_RECEIVED);
            }

            public void stateEvent(AioSession<String> session, StateMachineEnum stateMachineEnum,
                Throwable throwable) {
            }
        });
        return client.start();
    }

    protected void setHostPort(String hostPort) {
        this.host = StringUtils.splitByWholeSeparator(hostPort, ":")[0];
        this.port = Integer.valueOf(StringUtils.splitByWholeSeparator(hostPort, ":")[1]);
    }
}
