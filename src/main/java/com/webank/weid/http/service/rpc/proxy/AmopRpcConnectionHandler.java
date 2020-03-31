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

package com.webank.weid.http.service.rpc.proxy;

import com.webank.weid.constant.EndpointServiceConstant;
import com.webank.weid.http.constant.WeIdentityServiceEndpoint;
import com.webank.weid.http.service.rpc.RpcClient;
import com.webank.weid.util.DataToolUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client side handler, build remote rpc server connection and simple rpc client management, e.g. create/close, receive/listen. Provides simple
 * interfaces for external call. This is specifically designed for Amop Proxy Rpc service.
 */
public class AmopRpcConnectionHandler {

    private static final Logger logger = LoggerFactory.getLogger(AmopRpcConnectionHandler.class);

    private static RpcClient rpcClient;

    private static String host = "127.0.0.1:6095";
        //PropertiesUtil.getProperty("amop.proxy.server");

    private static String HANDSHAKE_STRING = "hi";

    /**
     * Background process to periodically fetch remote's endpoint info per defined seconds.
     */
    public static void init() {
        try {
            rpcClient = new RpcClient(host);
            // We need to enable reverse talk, so here we explicitly send a message first
            send(HANDSHAKE_STRING, AmopProxyCode.CLIENT_HANDSHAKE.getCode());
            // Pro-actively sleep 500ms to release thread block
            Thread.sleep(500);
        } catch (Exception e) {
            logger.error("Failed to establish RPC connection to host: ", host);
        }
    }

    /**
     * Build an auth-based connection to remote proxy server.
     * @param weId
     * @param privateKey
     * @return
     */
    public static boolean connect(String weId, String privateKey) {
        // 1. client -> server: create, this is a synchronous call
        String uuid = send(weId, AmopProxyCode.CLIENT_AUTH.getCode());
        if (StringUtils.isEmpty(uuid)) {
            return false;
        }
        // during here server verifies WeID etc.
        String reply = get(uuid);

        // 1b. server -> client: success or requestToken
        AmopProxyCode replyCode = extractCodeFromReply(reply);
        if (replyCode != AmopProxyCode.SUCCESS && replyCode != AmopProxyCode.SERVER_TOKEN) {
            logger.error("Illegal code when requesting token: "
                + replyCode.getCode() + ", error: " + replyCode.getCodeDesc());
            return false;
        }
        if (replyCode == AmopProxyCode.SUCCESS) {
            return true;
        }

        // 2. client signs token
        String sig = DataToolUtils.sign(extractMsgFromReply(reply), privateKey);
        uuid = send(weId + "```" + sig, AmopProxyCode.CLIENT_SIGNED_TOKEN.getCode());
        if (StringUtils.isEmpty(uuid)) {
            return false;
        }
        // during here server verifies signature
        reply = get(uuid);

        // 2b. server -> client: success or failure
        replyCode = extractCodeFromReply(reply);
        if (replyCode != AmopProxyCode.SUCCESS) {
            logger.error("Illegal code when verify sig: "
                + replyCode.getCode() + ", error: " + replyCode.getCodeDesc());
            return false;
        }
        return true;
    }

    /**
     * An explicit close connection to mute current rpc and de-register everything.
     * @return
     */
    public static boolean close() {
        // 1. client -> server: close
        String uuid = send("close", AmopProxyCode.CLIENT_CLOSE.getCode());
        if (StringUtils.isEmpty(uuid)) {
            return false;
        }
        // during here server verifies WeID etc and close everything.
        String reply = get(uuid);

        // server replies
        AmopProxyCode replyCode = extractCodeFromReply(reply);
        if (replyCode != AmopProxyCode.SUCCESS) {
            logger.error("Illegal code when closing connection: "
                + replyCode.getCode() + ", error: " + replyCode.getCodeDesc());
            return false;
        }
        return true;
    }

    public static boolean registerListeningTopic(String topic) {
        // 1. client -> server register
        String uuid = send(topic, AmopProxyCode.CLIENT_REGISTER_TOPIC.getCode());
        if (StringUtils.isEmpty(uuid)) {
            return false;
        }
        String reply = get(uuid);

        AmopProxyCode replyCode = extractCodeFromReply(reply);
        if (replyCode != AmopProxyCode.SUCCESS) {
            logger.error("Illegal code when closing connection: "
                + replyCode.getCode() + ", error: " + replyCode.getCodeDesc());
            return false;
        }
        return true;
    }

    public static boolean sendMessage(String msg) {
        String uuid = send(msg, AmopProxyCode.CLIENT_AMOP_SEND.getCode());
        if (StringUtils.isEmpty(uuid)) {
            return false;
        }
        String reply = get(uuid);

        AmopProxyCode replyCode = extractCodeFromReply(reply);
        if (replyCode != AmopProxyCode.SUCCESS) {
            logger.error("Illegal code when closing connection: "
                + replyCode.getCode() + ", error: " + replyCode.getCodeDesc());
            return false;
        }
        return true;
    }

    /**
     * Invoke a one-time RPC send based on given address and request and get the UUID reply.
     *
     * @param msg address to be sent to
     * @param code the endpoint request
     * @return String reply message
     */
    private static String send(String msg, Integer code) {
        try {
            if (!rpcClient.isValid()) {
                rpcClient.reconnect();
            }
            if (rpcClient == null) {
                rpcClient = new RpcClient(host);
            }
            String uuid = rpcClient
                .send(msg + WeIdentityServiceEndpoint.EPS_SEPARATOR + code)
                .getRespBody();
            if (!StringUtils.isEmpty(uuid)) {
                return uuid;
            } else {
                logger.error("Cannot obtain a valid session UUID!");
                return StringUtils.EMPTY;
            }
        } catch (Exception e) {
            logger.error("Failed to send to remote server! ", e);
            return StringUtils.EMPTY;
        }
    }

    /**
     * Async get the result from remote's reply.
     *
     * @param uuid the uuid
     * @return result
     */
    private static String get(String uuid) {
        if (rpcClient == null) {
            return StringUtils.EMPTY;
        }
        try {
            return rpcClient.get(uuid).getRespBody();
        } catch (Exception e) {
            logger.error("Failed to get this session UUID info: " + uuid);
            return StringUtils.EMPTY;
        }
    }

    public static AmopProxyCode extractCodeFromReply(String reply) {
        String[] msgArray = StringUtils
            .splitByWholeSeparator(reply, EndpointServiceConstant.EPS_SEPARATOR);
        return AmopProxyCode.getByCode(Integer.valueOf(msgArray[1]));
    }

    public static String extractMsgFromReply(String reply) {
        return StringUtils
            .splitByWholeSeparator(reply, EndpointServiceConstant.EPS_SEPARATOR)[0];
    }
}
