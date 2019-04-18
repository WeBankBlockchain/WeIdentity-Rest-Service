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

package com.webank.weid.http.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.webank.weid.http.constant.WeIdentityServiceEndpoint;
import com.webank.weid.http.protocol.response.HttpResponseData;
import com.webank.weid.http.service.TransactionService;

/**
 * Transaction Controller - to create encodedTransaction and send to Chain.
 *
 * @author chaoxinhu and darwindu
 */
@RestController
@RequestMapping(value = WeIdentityServiceEndpoint.API_ROOT)
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    /**
     * Create an Encoded Transaction.
     *
     * @param encodeTransactionJsonArgs the json format args. It should contain two keys:
     *   inputParams (including all business related params as well as signatures if required),
     *   and functionName. Hereafter, functionName will decide which WeID SDK method to engage,
     *   and assemble the inputParams to construct the response.
     * @return the json string wrapper which contains two keys: the encoded transaction byte
     *   array in Base64 format, and the rawTransaction related params for future encoding.
     */
    @RequestMapping(value = WeIdentityServiceEndpoint.ENCODE_TRANSACTION, method = RequestMethod.POST)
    public HttpResponseData<Object> encodeTransaction(
        @RequestBody String encodeTransactionJsonArgs) {
        return transactionService.encodeTransaction(encodeTransactionJsonArgs);
    }

    /**
     * Send a signed Transaction to Chain.
     *
     * @param sendTransactionJsonArgs the json format args. It should contain three keys:
     *   the same inputParams as in the createEncodeTransaction case, the signedMessage based on
     *   previous encodedTransaction, and the functionName as to decide the SDK method endpoint.
     * @return the json string from SDK response.
     */
    @RequestMapping(value = WeIdentityServiceEndpoint.SEND_TRANSACTION, method = RequestMethod.POST)
    public HttpResponseData<Object> sendTransaction(
        @RequestBody String sendTransactionJsonArgs) {
        return transactionService.sendTransaction(sendTransactionJsonArgs);
    }

    /**
     * Invoke an SDK function.
     *
     * @param invokeFunctionJsonArgs the json format args. It should contain three keys:
     *   the same inputParams as in the createEncodeTransaction case, the signedMessage based on
     *   previous encodedTransaction, and the functionName as to decide the SDK method endpoint.
     * @return the json string from SDK response.
     */
    @RequestMapping(value = WeIdentityServiceEndpoint.INVOKE_FUNCTION, method = RequestMethod.POST)
    public HttpResponseData<Object> invokeFunction(
        @RequestBody String invokeFunctionJsonArgs) {
        return transactionService.invokeFunction(invokeFunctionJsonArgs);
    }
}
