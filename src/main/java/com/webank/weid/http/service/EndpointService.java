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

package com.webank.weid.http.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.webank.weid.http.protocol.request.EndpointRequest;
import com.webank.weid.http.protocol.response.EndpointInfo;
import com.webank.weid.http.protocol.response.HttpResponseData;

@Service
public interface EndpointService {

    /**
     * Get all registered endpoints.
     * @return
     */
    HttpResponseData<List<EndpointInfo>> getAllEndpoints();

    /**
     * Re-route an endpoint service to SDK.
     *
     * @param endpointRequest
     * @return
     */
    HttpResponseData<String> invokeEndpointService(EndpointRequest endpointRequest);

    /**
     * Get an endpoint info.
     *
     * @param endpointName
     * @return
     */
    HttpResponseData<EndpointInfo> getEndpoint(String endpointName);
}
