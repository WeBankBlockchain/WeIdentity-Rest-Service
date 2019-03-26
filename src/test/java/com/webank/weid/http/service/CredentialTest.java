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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.webank.weid.http.BaseTest;
import com.webank.weid.http.constant.WeIdentityFunctionNames;
import com.webank.weid.http.constant.WeIdentityParamKeyConstant;
import com.webank.weid.http.protocol.response.HttpResponseData;
import com.webank.weid.protocol.base.Credential;
import com.webank.weid.util.JsonUtil;

@Component
public class CredentialTest extends BaseTest {

    @Autowired
    TransactionService transactionService;
    @Autowired
    InvokerCredentialService invokerCredentialService;

    @Test
    public void TestCredentialAll() throws Exception {
        // test create
        Map<String, Object> funcArgMap = new LinkedHashMap<>();
        funcArgMap.put("cptId", "10");
        funcArgMap.put("issuer", "did:weid:0x12025448644151248e5c1115b23a3fe55f4158e4153");
        funcArgMap.put("expirationDate", "2019-04-18T21:12:33Z");
        Map<String, Object> claimMap = new LinkedHashMap<>();
        claimMap.put("acc", "10001");
        claimMap.put("name", "ppp");
        funcArgMap.put("claim", claimMap);
        Map<String, Object> txnArgMap = new LinkedHashMap<>();
        Map<String, Object> inputParamMap = new LinkedHashMap<>();
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_ARG, funcArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.TRANSACTION_ARG, txnArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.API_VERSION,
            WeIdentityParamKeyConstant.DEFAULT_API_VERSION);
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_NAME,
            WeIdentityFunctionNames.FUNCNAME_CREATE_CREDENTIAL);
        HttpResponseData<Object> resp1 =
            transactionService.invokeFunction(JsonUtil.objToJsonStr(inputParamMap));
        System.out.println("intermediate result: " + resp1);

        // simulate client side sign
        Map<String, Object> credMap = (Map<String, Object>) JsonUtil
            .jsonStrToObj(new HashMap<String, Object>(),
                JsonUtil.mapToCompactJson((Map<String, Object>) resp1.getRespBody()));
        String claimHash = credMap.get("claimHash").toString();
        credMap.remove("claimHash");
        Map<String, Object> credForSigMap = new HashMap<>(credMap);
        credForSigMap.replace("claim", claimHash);
        credForSigMap.remove("signature");
        //do sign
        //String sign = Sign(mapToJson(credForSigMap), privKey)
        String sign = "FeB5";
        credMap.replace("signature", sign);
        String credentialAfterSign = JsonUtil.mapToCompactJson(credMap);
        Credential credential = (Credential) JsonUtil
            .jsonStrToObj(new Credential(), credentialAfterSign);
        System.out.println("after sign: " + credential);

        //test verify
        Map<String, Object> credJsonMap = JsonUtil.objToMap(credential);
        inputParamMap = new LinkedHashMap<>();
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_ARG, credJsonMap);
        inputParamMap.put(WeIdentityParamKeyConstant.TRANSACTION_ARG, txnArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.API_VERSION,
            WeIdentityParamKeyConstant.DEFAULT_API_VERSION);
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_NAME,
            WeIdentityFunctionNames.FUNCNAME_VERIFY_CREDENTIAL);
        HttpResponseData<Object> resp3 =
            transactionService.invokeFunction(JsonUtil.objToJsonStr(inputParamMap));
        System.out.println(resp3);
    }
}
