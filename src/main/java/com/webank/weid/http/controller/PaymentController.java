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

package com.webank.weid.http.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.webank.weid.http.constant.WeIdentityServiceEndpoint;
import com.webank.weid.http.protocol.response.HttpResponseData;
import com.webank.weid.http.service.WalletAgentBAC004Service;

/**
 * Transaction Controller - to create encodedTransaction and send to Chain.
 *
 * @author chaoxinhu and darwindu
 */
@RestController
@RequestMapping(value = WeIdentityServiceEndpoint.PAYMENT_ROOT)
public class PaymentController {
    
    @Autowired
    private WalletAgentBAC004Service walletAgentBAC004Service;
  
    
    /**
     * Invoke an SDK function.
     *
     * @param invokeFunctionJsonArgs the json format args. It should contain three keys: the same inputParams as in the createEncodeTransaction case,
     * the signedMessage based on previous encodedTransaction, and the functionName as to decide the SDK method endpoint.
     * @return the json string from SDK response.
     */
    @RequestMapping(value = WeIdentityServiceEndpoint.WALLET_AGENT_BAC004_FUNCTION, method = RequestMethod.POST)
    public HttpResponseData<Object> invokeWalletAgentBac004(
        @RequestBody String invokeFunctionJsonArgs
    ) {
        return walletAgentBAC004Service.invokeFunction(invokeFunctionJsonArgs);
    }
}
