package com.webank.weid.http.service.rpc.proxy;


import static com.webank.weid.http.constant.WeIdentityServiceEndpoint.EPS_SEPARATOR;

import com.webank.weid.constant.EndpointServiceConstant;
import com.webank.weid.http.service.rpc.FixedLengthProtocol;
import com.webank.weid.rpc.callback.AmopCallback;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.transport.AioQuickServer;
import org.smartboot.socket.transport.AioSession;

/**
 * A Simple Proxy Server based on smart socket. In Rest-Server, this will be used as the channel to convey control and data bulks between SDK clients,
 * in order to proxy traffic to blockchain nodes.
 *
 * @author chaoxinhu 2020.4
 */
public class AmopProxyServer {

    private static final Logger logger = LoggerFactory.getLogger(AmopProxyServer.class);
    private static final Integer DEFAULT_BOSS_THREAD_NUM = 10;
    private static final Integer DEFAULT_WORKER_THREAD_NUM = 20;
    private static final Integer UUID_LENGTH = 36;

    private ExecutorService pool = new ThreadPoolExecutor(10, 200, 0L, TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<>(1024), new ThreadPoolExecutor.AbortPolicy());

    /**
     * Map connected host to session. Here, host/session is 1:1.
     */
    private Map<String, AioSession<String>> hostSessionMap = new ConcurrentHashMap<>();

    /**
     * Map registered listening topic to host. Here, topic/host can be M:1. topicHostMap -> hostSessionMap, need check hostStateMap.
     */
    private Map<String, String> topicHostMap = new ConcurrentHashMap<>();

    /**
     * Map client side auth WeID and the generated token UUID. This is 1:1.
     */
    private Map<String, String> weIdTokenMap = new ConcurrentHashMap<>();

    /**
     * Map a host to its current state.
     */
    private Map<String, Integer> hostStateMap = new ConcurrentHashMap<>();

    public enum ConnectionState {
        CONNECTED(0),
        AUTHENTICATING(1),
        AUTHENTICATED(2);

        private int code;

        ConnectionState(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }

    /**
     * Background process to periodically fetch remote's endpoint info per defined seconds.
     */
    public AmopProxyServer() throws Exception {
        // Do check blockchain connections

    }

    /**
     * The main entrance for RPC server process.
     *
     * @throws Exception any exception
     */
    public void run() throws Exception {
        Integer listenerPort;
        listenerPort = 6095;
        System.out.println("Proxy: trying to receive incoming traffic at Port: " + listenerPort);
        logger.info("Proxy: trying to receive incoming traffic at Port: " + listenerPort);

        AioQuickServer<String> server = new AioQuickServer<String>(listenerPort,
            new FixedLengthProtocol(),
            new MessageProcessor<String>() {
                @Override
                public void process(AioSession<String> session, String msg) {
                    pool.execute(() -> {
                        System.out.println("[Server] Current id is: " + session.getSessionID() + ", Received from client: " + msg);
                        String clientHost;
                        try {
                            clientHost = session.getRemoteAddress().getHostString();
                            hostSessionMap.put(clientHost, session);
                            if (!hostStateMap.containsKey(clientHost)) {
                                System.out.println("[Server] Successfully established connection from: " + clientHost);
                                hostStateMap.put(clientHost, ConnectionState.CONNECTED.getCode());
                            }
                        } catch (IOException e) {
                            logger.error("Failed to get client host name for current session: " + session.getSessionID());
                            return;
                        }
                        String bizResult = processClientMessage(clientHost, msg);
                        if (StringUtils.isEmpty(bizResult)) {
                            return;
                        }
                        String uuid = msg.substring(msg.length() - UUID_LENGTH);
                        String reply = bizResult + EndpointServiceConstant.EPS_SEPARATOR + uuid;
                        byte[] resp = FixedLengthProtocol.encodeStrToByte(reply);
                        synchronized (session) {
                            try {
                                session.writeBuffer().write(resp);
                                session.writeBuffer().flush();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }

                @Override
                public void stateEvent(AioSession<String> session,
                    StateMachineEnum stateMachineEnum, Throwable throwable) {
                }
            });
        server.setBossThreadNum(DEFAULT_BOSS_THREAD_NUM);
        server.setWorkerThreadNum(DEFAULT_WORKER_THREAD_NUM);
        server.start();
    }

    private String processClientMessage(String host, String msg) {
        AmopProxyCode code = extractCodeFromReply(msg);
        String reply = StringUtils.EMPTY;
        switch (code) {
            case CLIENT_HANDSHAKE:
                break;
            case CLIENT_AUTH:
                // Always override existing token by creating a new one
                String weId = extractMsgFromReply(msg);
                String token = UUID.randomUUID().toString();
                weIdTokenMap.put(weId, token);
                reply = token + EndpointServiceConstant.EPS_SEPARATOR + AmopProxyCode.SERVER_TOKEN.getCode();
                hostStateMap.put(host, ConnectionState.AUTHENTICATING.getCode());
                break;
            case CLIENT_SIGNED_TOKEN:
                if (!hostStateMap.get(host).equals(ConnectionState.AUTHENTICATING.getCode())) {
                    logger.error("Invalid request: state illegal. " + host + "," + msg);
                    reply = "failed" + EndpointServiceConstant.EPS_SEPARATOR + AmopProxyCode.INVALID_STATUS.getCode();
                    break;
                }
                String[] msgArray = StringUtils.splitByWholeSeparator(extractMsgFromReply(msg), "```");
                weId = msgArray[0];
                String signedToken = msgArray[1];
                String originalToken = weIdTokenMap.get(weId);
                // todo Go to WeID and verify signature via WeID
                hostStateMap.put(host, ConnectionState.AUTHENTICATED.getCode());
                reply = "successfully connected" + EndpointServiceConstant.EPS_SEPARATOR + AmopProxyCode.SUCCESS.getCode();
                break;
            case CLIENT_CLOSE:
                for (Map.Entry<String, String> pair : topicHostMap.entrySet()) {
                    if (host.equalsIgnoreCase(pair.getValue())) {
                        // todo AMOP de-register here
                        topicHostMap.remove(pair.getKey());
                    }
                }
                hostStateMap.put(host, ConnectionState.CONNECTED.getCode());
                reply = "successfully closed" + EndpointServiceConstant.EPS_SEPARATOR + AmopProxyCode.SUCCESS.getCode();
                break;
            case CLIENT_REGISTER_TOPIC:
                if (!hostStateMap.get(host).equals(ConnectionState.AUTHENTICATED.getCode())) {
                    logger.error("Invalid request: state illegal. " + host + "," + msg);
                    reply = "failed" + EndpointServiceConstant.EPS_SEPARATOR + AmopProxyCode.INVALID_STATUS.getCode();
                    break;
                }
                String topic = extractMsgFromReply(msg);
                // todo AMOP register here
                topicHostMap.put(topic, host);
                reply = "succeed" + EndpointServiceConstant.EPS_SEPARATOR + AmopProxyCode.SUCCESS.getCode();
                break;
            case CLIENT_AMOP_SEND:
                if (!hostStateMap.get(host).equals(ConnectionState.AUTHENTICATED.getCode())) {
                    logger.error("Invalid request: state illegal. " + host + "," + msg);
                    reply = "failed" + EndpointServiceConstant.EPS_SEPARATOR + AmopProxyCode.INVALID_STATUS.getCode();
                    break;
                }
                msgArray = StringUtils.splitByWholeSeparator(extractMsgFromReply(msg), "```");
                String msgTo = msgArray[0];
                String topicTo = msgArray[1];
                // todo AMOP send here
                reply = "succeed" + EndpointServiceConstant.EPS_SEPARATOR + AmopProxyCode.SUCCESS.getCode();
                break;
            default:
                reply = "failed" + EndpointServiceConstant.EPS_SEPARATOR + AmopProxyCode.FAILED.getCode();
        }
        return reply;
    }

    public void receiveAmopMessage(AmopCallback callback) {
        Set<String> allTopics = topicHostMap.keySet();
        // todo step 1: register callbacks for all topics
        // todo step 2: once receive, call send() to the host with AmopProxyCode.SERVER_AMOP_SEND
    }

    public void broadcastToAllHosts(String message) {
        for (String hostAddress : hostSessionMap.keySet()) {
            send(hostAddress, message, AmopProxyCode.SERVER_SEND.getCode());
        }
    }

    public void send(String hostAddress, String message, Integer code) {
        AioSession<String> session = hostSessionMap.get(hostAddress);
        if (session == null || session.isInvalid()) {
            logger.error("Cannot find an active session for host: " + hostAddress);
            return;
        }
        pool.execute(() -> {
            try {
                String msg = message + EPS_SEPARATOR + code + EPS_SEPARATOR + UUID.randomUUID();
                System.out.println("[Server] Current id is: " + session.getSessionID() + ", Sending: " + msg);
                byte[] resp = FixedLengthProtocol.encodeStrToByte(msg);
                synchronized (session) {
                    try {
                        session.writeBuffer().write(resp);
                        session.writeBuffer().flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private AmopProxyCode extractCodeFromReply(String reply) {
        String[] msgArray = StringUtils
            .splitByWholeSeparator(reply, EndpointServiceConstant.EPS_SEPARATOR);
        return AmopProxyCode.getByCode(Integer.valueOf(msgArray[1]));
    }

    private String extractMsgFromReply(String reply) {
        return StringUtils
            .splitByWholeSeparator(reply, EndpointServiceConstant.EPS_SEPARATOR)[0];
    }
}
