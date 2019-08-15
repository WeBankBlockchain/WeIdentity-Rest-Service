/*
 *       Copyright© (2019) WeBank Co., Ltd.
 *
 *       This file is part of weidentity-http-service.
 *
 *       weidentity-http-service is free software: you can redistribute it and/or modify
 *       it under the terms of the GNU Lesser General Public License as published by
 *       the Free Software Foundation, either version 3 of the License, or
 *       (at your option) any later version.
 *
 *       weidentity-http-service is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU Lesser General Public License for more details.
 *
 *       You should have received a copy of the GNU Lesser General Public License
 *       along with weidentity-http-service.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.webank.weid.http.service.rpc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.webank.weid.http.constant.HttpReturnCode;
import com.webank.weid.http.constant.WeIdentityServiceEndpoint;
import com.webank.weid.http.protocol.request.EndpointRequest;
import com.webank.weid.http.protocol.response.EndpointInfo;
import com.webank.weid.http.protocol.response.HttpResponseData;
import com.webank.weid.http.util.EndpointDataUtil;
import com.webank.weid.http.util.PropertiesUtil;
import com.webank.weid.util.DataToolUtils;

/**
 * Client side handler, build remote rpc server connection and simple rpc client management, e.g.
 * create/close, multi-active, and auto-clean. Provides simple interfaces for external call.
 */
public class RpcConnectionHandler {

    private static final Logger logger = LoggerFactory.getLogger(RpcConnectionHandler.class);

    // Mapping key: request (UUID), value: a remote host/port
    private static Map<String, String> uuidHostMap = new ConcurrentHashMap<>();

    // Mapping key: hostport, value: one active RpcClient
    private static Map<String, RpcClient> hostClientsMap = new ConcurrentHashMap<>();

    /**
     * Background process to periodically fetch remote's endpoint info per defined seconds.
     */
    public static void init() {
        Runnable runnable = new Runnable() {
            public void run() {
                List<String> inAddrList = Arrays
                    .asList(PropertiesUtil.getProperty("server.hostport.list").split(","));
                fetchAndMergeEndpoints(inAddrList);
            }
        };
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
        scheduler.scheduleAtFixedRate(runnable, 1,
            Integer.valueOf(PropertiesUtil.getProperty("fetch.period.seconds")), TimeUnit.SECONDS);
    }

    /**
     * Invoke a one-time RPC send based on given address and request and get the UUID reply.
     *
     * @param hostport address to be sent to
     * @param endpointRequest the endpoint request
     * @return String reply message
     */
    public static HttpResponseData<String> send(String hostport, EndpointRequest endpointRequest) {
        RpcClient rpcClient;
        try {
            if (hostClientsMap.containsKey(hostport)) {
                rpcClient = hostClientsMap.get(hostport);
                if (!rpcClient.isValid()) {
                    rpcClient.reconnect();
                }
            } else {
                rpcClient = new RpcClient(hostport);
                hostClientsMap.put(hostport, rpcClient);
            }
            String uuid = rpcClient
                .send(endpointRequest.getRequestName() + WeIdentityServiceEndpoint.EPS_SEPARATOR
                    + endpointRequest.getRequestBody())
                .getRespBody();
            if (!StringUtils.isEmpty(uuid)) {
                uuidHostMap.put(uuid, hostport);
                return new HttpResponseData<>(uuid, HttpReturnCode.SUCCESS);
            } else {
                logger.info("Cannot obtain a valid session UUID when sending to: ", hostport);
                return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.RPC_GET_FAIL);
            }
        } catch (Exception e) {
            logger.error("Failed to send to remote server: ", hostport);
            return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.RPC_SEND_FAIL);
        }
    }

    /**
     * Invoke an RPC random send based on given Endpoint info and request and get the UUID reply.
     * This is a random Send procedure - sender will randomly pick an address in the list and send
     * out the request. If it fails, then send to next VM until succeeded.
     *
     * @param inAddrList destination address list
     * @return UUID index
     */
    public static HttpResponseData<String> randomSend(List<String> inAddrList,
        EndpointRequest endpointRequest) {
        double gap = (double) 1 / inAddrList.size();
        double pick = Math.random();
        for (int i = 0; i < inAddrList.size(); i++) {
            // check whether to send in this round
            if (pick < (((double) (i + 1)) * gap)) {
                HttpResponseData<String> sendReply = send(inAddrList.get(i), endpointRequest);
                if (StringUtils.isEmpty(sendReply.getRespBody())) {
                    pick += gap;
                    logger.error("A RandomSend attempt failed: " + inAddrList.get(i));
                } else {
                    return sendReply;
                }
            }
        }
        return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.RPC_SEND_FAIL);
    }

    /**
     * Async get the result from remote's reply.
     *
     * @param uuid the uuid
     * @return result
     */
    public static HttpResponseData<String> get(String uuid) {
        String hostPort = uuidHostMap.get(uuid);
        if (StringUtils.isEmpty(hostPort)) {
            return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.INPUT_ILLEGAL);
        }
        RpcClient rpcClient = hostClientsMap.get(hostPort);
        if (rpcClient == null) {
            return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.RPC_NETWORK_ERROR);
        }
        try {
            return rpcClient.get(uuid);
        } catch (Exception e) {
            logger.error("Failed to get this session UUID info: " + uuid);
            return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.RPC_GET_FAIL);
        }
    }

    /**
     * Fetch remote all server's endpoints in a best-effort manner, and merge to local data store.
     *
     * @param addrList remote address list
     */
    public static void fetchAndMergeEndpoints(List<String> addrList) {
        logger.info("Now: " + System.currentTimeMillis() + " Starting to poll remote service at: "
            + EndpointDataUtil.stringListToString(addrList));
        System.out.println("Now: " + System.currentTimeMillis() + " Starting to poll remote service"
            + " at: " + EndpointDataUtil.stringListToString(addrList));
        EndpointRequest endpointRequest = new EndpointRequest();
        endpointRequest.setRequestName(WeIdentityServiceEndpoint.FETCH_FUNCTION);
        EndpointDataUtil.clearProps();
        for (String hostport : addrList) {
            try {
                String uuid = send(hostport, endpointRequest).getRespBody();
                if (!StringUtils.isEmpty(uuid)) {
                    String reply = get(uuid).getRespBody();
                    List<String> endpointInfoList = new ArrayList<>(
                        Arrays.asList(reply.split("```")));
                    for (String endpointInfoString : endpointInfoList) {
                        EndpointInfo endpointInfo = DataToolUtils
                            .deserialize(endpointInfoString, EndpointInfo.class);
                        // Fillin the external network addr to be the one in here
                        List<String> newList = new ArrayList<>();
                        newList.add(hostport);
                        endpointInfo.setInAddr(newList);
                        logger.debug("Fetched Endpoint: " + DataToolUtils.serialize(endpointInfo));
                        EndpointDataUtil.mergeToCentral(endpointInfo);
                    }
                } else {
                    logger.info("Failed to connect to remote host (reply null): " + hostport);
                }
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
        try {
            EndpointDataUtil.saveEndpointsToFile();
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }
}
