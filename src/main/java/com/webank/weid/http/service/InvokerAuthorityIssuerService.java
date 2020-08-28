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

package com.webank.weid.http.service;

import org.springframework.stereotype.Service;

import com.webank.weid.http.protocol.request.InputArg;
import com.webank.weid.http.protocol.response.HttpResponseData;

@Service
public interface InvokerAuthorityIssuerService {

    /**
     * Register a new Authority Issuer on Chain via Invoke function.
     *
     * @param registerArgs the args
     * @return the Boolean response data
     */
    HttpResponseData<Object> registerAuthorityIssuerInvoke(InputArg registerArgs);

    /**
     * Call to WeID SDK with direct transaction hex String, to register AuthorityIssuer.
     *
     * @param transactionHex the transactionHex value
     * @return String in ResponseData
     */
    HttpResponseData<String> registerAuthorityIssuerWithTransactionHex(
        String transactionHex);

    /**
     * Query Authority Issuer via the InvokeFunction API.
     *
     * @param queryArgs the query WeID
     * @return the authorityIssuer
     */
    HttpResponseData<Object> queryAuthorityIssuerInfoInvoke(InputArg queryArgs);

    HttpResponseData<Object> getWeIdByNameInvoke(InputArg args);
    
    /**
     * Register Issuer Type  on chain.
     * @param registerArgs the args
     * @return the Boolean response data
     */
    HttpResponseData<Object> addWeIdToIssuerTypeInvoke(InputArg args);
    
    /**
     * Check weId is exists in IssuerType on chain.
     * @param CheckArgs the args
     * @return the Boolean response data
     */
    HttpResponseData<Object> checkWeIdByIssuerTypeInvoke(InputArg args);
}
