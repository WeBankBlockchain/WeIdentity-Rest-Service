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

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.webank.weid.constant.ParamKeyConstant;
import com.webank.weid.http.BaseTest;
import com.webank.weid.http.constant.WeIdentityFunctionNames;
import com.webank.weid.http.constant.WeIdentityParamKeyConstant;
import com.webank.weid.http.protocol.response.HttpResponseData;
import com.webank.weid.http.util.JsonUtil;

@Component
public class PureInvokerTest extends BaseTest {

    @Autowired
    TransactionService transactionService;

    @Test
    public void testInvokeIntegration() throws Exception {
        // create a WeID
        Map<String, Object> funcArgMap = new LinkedHashMap<>();
        Map<String, Object> txnArgMap = new LinkedHashMap<>();
        Map<String, Object> inputParamMap = new LinkedHashMap<>();
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_ARG, funcArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.TRANSACTION_ARG, txnArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.API_VERSION,
            WeIdentityParamKeyConstant.DEFAULT_API_VERSION);
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_NAME,
            WeIdentityFunctionNames.FUNCNAME_CREATE_WEID);
        HttpResponseData<Object> resp1 = transactionService
            .invokeFunction(JsonUtil.objToJsonStr(inputParamMap));
        System.out.println(JsonUtil.objToJsonStr(resp1));
        String weId = (String) resp1.getRespBody();
        Assert.assertTrue(!StringUtils.isEmpty(weId));

        // get WeID document
        funcArgMap = new LinkedHashMap<>();
        funcArgMap.put("weId", weId);
        txnArgMap = new LinkedHashMap<>();
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_ARG, funcArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.TRANSACTION_ARG, txnArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.API_VERSION,
            WeIdentityParamKeyConstant.DEFAULT_API_VERSION);
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_NAME,
            WeIdentityFunctionNames.FUNCNAME_GET_WEID_DOCUMENT);
        HttpResponseData<Object> resp2 =
            transactionService.invokeFunction(JsonUtil.objToJsonStr(inputParamMap));
        System.out.println(JsonUtil.objToJsonStr(resp2));
        Assert.assertTrue(resp2.getRespBody() != null);

        // register authority issuer using dumb weid, should fail
        funcArgMap = new LinkedHashMap<>();
        funcArgMap.put("weId", weId);
        funcArgMap.put("name", "Sample College");
        txnArgMap = new LinkedHashMap<>();
        txnArgMap.put(WeIdentityParamKeyConstant.KEY_INDEX, weId);
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_ARG, funcArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.TRANSACTION_ARG, txnArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.API_VERSION,
            WeIdentityParamKeyConstant.DEFAULT_API_VERSION);
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_NAME,
            WeIdentityFunctionNames.FUNCNAME_REGISTER_AUTHORITY_ISSUER);
        HttpResponseData<Object> resp3 =
            transactionService.invokeFunction(JsonUtil.objToJsonStr(inputParamMap));
        System.out.println(JsonUtil.objToJsonStr(resp3));
        Assert
            .assertTrue((resp3.getRespBody().toString()).equalsIgnoreCase(Boolean.FALSE.toString()));

        // register authority Issuer (use SDK privkey, should succeed)
        funcArgMap = new LinkedHashMap<>();
        funcArgMap.put("weId", weId);
        funcArgMap.put("name", "Sample College" + Math.random() * 100);
        txnArgMap = new LinkedHashMap<>();
        txnArgMap.put(WeIdentityParamKeyConstant.KEY_INDEX, "0xffffffff");
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_ARG, funcArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.TRANSACTION_ARG, txnArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.API_VERSION,
            WeIdentityParamKeyConstant.DEFAULT_API_VERSION);
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_NAME,
            WeIdentityFunctionNames.FUNCNAME_REGISTER_AUTHORITY_ISSUER);
        HttpResponseData<Object> resp4 =
            transactionService.invokeFunction(JsonUtil.objToJsonStr(inputParamMap));
        System.out.println(JsonUtil.objToJsonStr(resp4));
        Assert.assertTrue((resp4.getRespBody().toString()).equalsIgnoreCase(Boolean.TRUE.toString()));

        // query authority issuer
        funcArgMap = new LinkedHashMap<>();
        funcArgMap.put("weId", weId);
        txnArgMap = new LinkedHashMap<>();
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_ARG, funcArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.TRANSACTION_ARG, txnArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.API_VERSION,
            WeIdentityParamKeyConstant.DEFAULT_API_VERSION);
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_NAME,
            WeIdentityFunctionNames.FUNCNAME_QUERY_AUTHORITY_ISSUER);
        HttpResponseData<Object> resp5 =
            transactionService.invokeFunction(JsonUtil.objToJsonStr(inputParamMap));
        System.out.println(JsonUtil.objToJsonStr(resp5));
        Assert.assertTrue(resp5.getRespBody() != null);

        // register cpt
        funcArgMap = new LinkedHashMap<>();
        funcArgMap.put("weId", weId);
        Map<String, Object> cptJsonSchemaMap = new LinkedHashMap<>();
        cptJsonSchemaMap.put("title", "a CPT schema");
        funcArgMap.put("cptJsonSchema", cptJsonSchemaMap);
        txnArgMap = new LinkedHashMap<>();
        txnArgMap.put(WeIdentityParamKeyConstant.KEY_INDEX, weId);
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_ARG, funcArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.TRANSACTION_ARG, txnArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.API_VERSION,
            WeIdentityParamKeyConstant.DEFAULT_API_VERSION);
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_NAME,
            WeIdentityFunctionNames.FUNCNAME_REGISTER_CPT);
        HttpResponseData<Object> resp6 =
            transactionService.invokeFunction(JsonUtil.objToJsonStr(inputParamMap));
        System.out.println(JsonUtil.objToJsonStr(resp6));
        Assert.assertTrue(resp6.getRespBody() != null);

        // query cpt
        Map<String, Object> cptMap = ((Map<String, Object>) resp6.getRespBody());
        Integer cptId = Integer.valueOf(cptMap.get(ParamKeyConstant.CPT_ID).toString());
        System.out.println("cptId is: " + cptId);
        funcArgMap = new LinkedHashMap<>();
        funcArgMap.put("cptId", cptId);
        txnArgMap = new LinkedHashMap<>();
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_ARG, funcArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.TRANSACTION_ARG, txnArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.API_VERSION,
            WeIdentityParamKeyConstant.DEFAULT_API_VERSION);
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_NAME,
            WeIdentityFunctionNames.FUNCNAME_QUERY_CPT);
        HttpResponseData<Object> resp7 =
            transactionService.invokeFunction(JsonUtil.objToJsonStr(inputParamMap));
        System.out.println(JsonUtil.objToJsonStr(resp7));
        Assert.assertTrue(resp7.getRespBody() != null);

        // create credential using this cpt id
        funcArgMap = new LinkedHashMap<>();
        funcArgMap.put("cptId", cptId);
        funcArgMap.put("issuer", weId);
        funcArgMap.put("expirationDate", "2500-01-01T21:12:33Z");
        Map<String, Object> claimMap = new LinkedHashMap<>();
        claimMap.put("account", "10000");
        claimMap.put("name", "Anon");
        funcArgMap.put("claim", claimMap);
        txnArgMap = new LinkedHashMap<>();
        txnArgMap.put(WeIdentityParamKeyConstant.KEY_INDEX, weId);
        inputParamMap = new LinkedHashMap<>();
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_ARG, funcArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.TRANSACTION_ARG, txnArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.API_VERSION,
            WeIdentityParamKeyConstant.DEFAULT_API_VERSION);
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_NAME,
            WeIdentityFunctionNames.FUNCNAME_CREATE_CREDENTIAL);
        HttpResponseData<Object> resp8 =
            transactionService.invokeFunction(JsonUtil.objToJsonStr(inputParamMap));
        System.out.println(JsonUtil.objToJsonStr(resp8));
        Assert.assertTrue(resp8.getRespBody() != null);

        // verify credential. A not null answer is good enough.
        Map<String, Object> credJsonMap = (Map<String, Object>) resp8.getRespBody();
        txnArgMap = new LinkedHashMap<>();
        inputParamMap = new LinkedHashMap<>();
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_ARG, credJsonMap);
        inputParamMap.put(WeIdentityParamKeyConstant.TRANSACTION_ARG, txnArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.API_VERSION,
            WeIdentityParamKeyConstant.DEFAULT_API_VERSION);
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_NAME,
            WeIdentityFunctionNames.FUNCNAME_VERIFY_CREDENTIAL);
        HttpResponseData<Object> resp9 =
            transactionService.invokeFunction(JsonUtil.objToJsonStr(inputParamMap));
        System.out.println(JsonUtil.objToJsonStr(resp9));
        Assert.assertNotNull(resp9.getRespBody());
    }
}
