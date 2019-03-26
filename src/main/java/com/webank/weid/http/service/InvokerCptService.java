/*
 *       Copyright© (2019) WeBank Co., Ltd.
 *
 *       This file is part of weidentity-java-sdk.
 *
 *       weidentity-java-sdk is free software: you can redistribute it and/or modify
 *       it under the terms of the GNU Lesser General Public License as published by
 *       the Free Software Foundation, either version 3 of the License, or
 *       (at your option) any later version.
 *
 *       weidentity-java-sdk is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU Lesser General Public License for more details.
 *
 *       You should have received a copy of the GNU Lesser General Public License
 *       along with weidentity-java-sdk.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.webank.weid.http.service;

import org.springframework.stereotype.Service;

import com.webank.weid.http.protocol.request.InputArg;
import com.webank.weid.http.protocol.response.HttpResponseData;

@Service
public interface InvokerCptService {

    /**
     * Register a new CPT to blockchain via Invoke function.
     *
     * @param registerArgs the args
     * @return the resp data
     */
    HttpResponseData<Object> registerCptInvoke(InputArg registerArgs);

    /**
     * Call to WeID SDK with direct transaction hex String, to register CPT.
     *
     * @param transactionHex the transactionHex value
     * @return String in ResponseData
     */
    HttpResponseData<String> registerCptWithTransactionHex(String transactionHex);

    /**
     * Query CPT via the InvokeFunction API.
     *
     * @param queryArgs the query arg
     * @return the CPT data
     */
    HttpResponseData<Object> queryCptInvoke(InputArg queryArgs);
}
