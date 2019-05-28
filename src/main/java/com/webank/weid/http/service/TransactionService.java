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

import org.springframework.stereotype.Service;

import com.webank.weid.http.protocol.response.HttpResponseData;

/**
 * Handling Transaction related services.
 *
 * @author chaoxinhu and darwindu
 **/
@Service
public interface TransactionService {

    /**
     * Create an Encoded Transaction.
     *
     * @param encodeTransactionJsonArgs json format args. It should contain 4 keys: functionArgs
     * (including all business related params), transactionArgs, functionName and apiVersion.
     * Hereafter, functionName will decide which WeID SDK method to engage, and assemble all input
     * params into SDK readable format to send there; apiVersion is for extensibility purpose.
     * @return encoded transaction in Base64 format, and the data segment in RawTransaction.
     */
    HttpResponseData<Object> encodeTransaction(String encodeTransactionJsonArgs);

    /**
     * Send Transaction to Blockchain.
     *
     * @param sendTransactionJsonArgs the json format args. It should contain 4 keys: functionArgs
     * (including all business related params), transactionArgs, functionName and apiVersion.
     * @return the json string from SDK response.
     */
    HttpResponseData<Object> sendTransaction(String sendTransactionJsonArgs);

    /**
     * Directly invoke an SDK function. No client-side sign needed.
     *
     * @param invokeFunctionJsonArgs the json format args. It should contain 4 keys: functionArgs,
     * (including all business related params), EMPTY transactionArgs, functionName and apiVersion.
     * @return the json string from SDK response.
     */
    HttpResponseData<Object> invokeFunction(String invokeFunctionJsonArgs);
}
