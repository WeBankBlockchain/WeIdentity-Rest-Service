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
import com.webank.weid.protocol.response.CreateWeIdDataResult;
import com.webank.weid.blockchain.protocol.response.ResponseData;

@Service
public interface InvokerWeIdService {

    /**
     * Create WeIdentity DID - a raw method for test purpose only.
     *
     * @return the response data
     */
    ResponseData<CreateWeIdDataResult> createWeId();

    /**
     * Check if WeIdentity DID exists on Chain - for test purpose only.
     *
     * @param weId the WeIdentity DID
     * @return true if exists, false otherwise
     */
    ResponseData<Boolean> isWeIdExist(String weId);

    /**
     * Call to WeID SDK with direct transaction hex String, to create WeID.
     *
     * @param transactionHex the transactionHex value
     * @return String in ResponseData
     */
    HttpResponseData<String> createWeIdWithTransactionHex(String transactionHex);

    /**
     * Get a WeIdentity DID DocumentStream in Json-ld via the InvokeFunction API.
     *
     * @param getWeIdDocumentJsonArgs the WeIdentity DID
     * @return the WeIdentity DID document json
     */
    HttpResponseData<Object> getWeIdDocumentJsonInvoke(InputArg getWeIdDocumentJsonArgs);

    /**
     * Get a WeIdentity DID Document via the InvokeFunction API.
     *
     * @param getWeIdDocumentArgs the WeIdentity DID
     * @return the WeIdentity DID document json
     */
    HttpResponseData<Object> getWeIdDocumentInvoke(InputArg getWeIdDocumentArgs);

    /**
     * Create WeId via the InvokeFunction API.
     *
     * @param createWeIdJsonArgs the input args, should be almost null
     * @return the WeIdentity DID
     */
    HttpResponseData<Object> createWeIdInvoke(InputArg createWeIdJsonArgs);

    /**
     * Create WeId via the InvokeFunction API.
     *
     * @param createWeIdJsonArgs the input args, should be almost null
     * @return the WeIdentity DID
     */
    HttpResponseData<Object> createWeIdInvoke2(InputArg createWeIdJsonArgs);
    
    HttpResponseData<Object> createWeIdWithPubKey(InputArg arg);

    HttpResponseData<Object> createWeIdWithPubKey2(InputArg arg);
    
    HttpResponseData<Object> getWeIdListByPubKeyList(InputArg arg) throws Exception ;
}
