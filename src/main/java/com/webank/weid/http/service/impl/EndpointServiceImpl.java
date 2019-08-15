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

package com.webank.weid.http.service.impl;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.webank.weid.http.constant.HttpReturnCode;
import com.webank.weid.http.protocol.request.EndpointRequest;
import com.webank.weid.http.protocol.response.EndpointInfo;
import com.webank.weid.http.protocol.response.HttpResponseData;
import com.webank.weid.http.service.BaseService;
import com.webank.weid.http.service.EndpointService;
import com.webank.weid.http.service.rpc.RpcConnectionHandler;
import com.webank.weid.http.util.EndpointDataUtil;

@Component
public class EndpointServiceImpl extends BaseService implements EndpointService {

    private Logger logger = LoggerFactory.getLogger(EndpointServiceImpl.class);

    static {
        RpcConnectionHandler.init();
    }

    /**
     * Get all registered endpoints, locally.
     *
     * @return endpoint info list
     */
    public HttpResponseData<List<EndpointInfo>> getAllEndpoints() {
        try {
            EndpointDataUtil.loadAllEndpointInfoFromProps();
            return new HttpResponseData<>(EndpointDataUtil.getAllEndpointInfo(),
                HttpReturnCode.SUCCESS);
        } catch (Exception e) {
            return new HttpResponseData<>(null, HttpReturnCode.RPC_FETCH_FAIL.getCode(),
                HttpReturnCode.RPC_FETCH_FAIL.getCodeDesc() + e.getMessage());
        }
    }

    /**
     * Re-route an endpoint service to remote (SDK) machine. Check fisrt to make sure Remote machine
     * needs to be able to handle the request.
     *
     * @param endpointRequest endpoint request - name and body
     * @return any String
     */
    public HttpResponseData<String> invokeEndpointService(EndpointRequest endpointRequest) {
        List<EndpointInfo> endpointInfoList = EndpointDataUtil.getAllEndpointInfo();
        for (EndpointInfo endpointInfo : endpointInfoList) {
            String requestName = endpointInfo.getRequestName();
            if (requestName.equalsIgnoreCase(endpointRequest.getRequestName())) {
                try {
                    String uuid = RpcConnectionHandler
                        .randomSend(endpointInfo.getInAddr(), endpointRequest).getRespBody();
                    if (StringUtils.isEmpty(uuid)) {
                        return new HttpResponseData<>(null, HttpReturnCode.RPC_SEND_FAIL);
                    }
                    return RpcConnectionHandler.get(uuid);
                } catch (Exception e) {
                    return new HttpResponseData<>(null, HttpReturnCode.RPC_SEND_FAIL.getCode(),
                        HttpReturnCode.RPC_GET_FAIL.getCodeDesc() + e.getMessage());
                }
            }
        }
        return new HttpResponseData<>(null, HttpReturnCode.RPC_ENDPOINT_NOT_EXIST.getCode(),
            HttpReturnCode.RPC_ENDPOINT_NOT_EXIST.getCodeDesc() + ": " + endpointRequest
                .getRequestName());
    }

    /**
     * Get an endpoint info, locally.
     *
     * @param endpointName endpoint name
     * @return endpointInfo
     */
    public HttpResponseData<EndpointInfo> getEndpoint(String endpointName) {
        try {
            List<EndpointInfo> endpointInfoList = EndpointDataUtil.getAllEndpointInfo();
            for (EndpointInfo endpointInfo : endpointInfoList) {
                if (endpointInfo.getRequestName().equalsIgnoreCase(endpointName)) {
                    return new HttpResponseData<>(endpointInfo, HttpReturnCode.SUCCESS);
                }
            }
            return new HttpResponseData<>(null, HttpReturnCode.RPC_GET_FAIL);
        } catch (Exception e) {
            return new HttpResponseData<>(null, HttpReturnCode.RPC_GET_FAIL.getCode(),
                HttpReturnCode.RPC_GET_FAIL.getCodeDesc() + e.getMessage());
        }
    }
}
