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

import com.webank.weid.constant.ParamKeyConstant;
import com.webank.weid.http.BaseTest;
import com.webank.weid.http.constant.WeIdentityFunctionNames;
import com.webank.weid.http.constant.WeIdentityParamKeyConstant;
import com.webank.weid.http.protocol.response.HttpResponseData;
import com.webank.weid.http.service.impl.TransactionServiceImpl;
import com.webank.weid.http.util.JsonUtil;
import com.webank.weid.http.util.KeyUtil;
import com.webank.weid.http.util.PropertiesUtil;
import com.webank.weid.http.util.TransactionEncoderUtilV2;
import com.webank.weid.protocol.base.CredentialPojo;
import com.webank.weid.util.CredentialPojoUtils;
import com.webank.weid.util.DataToolUtils;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.bcos.web3j.crypto.ECKeyPair;
import org.bcos.web3j.crypto.Sign;
import org.junit.Assert;
import org.junit.Test;

public class PureInvokerTest extends BaseTest {

    TransactionService transactionService = new TransactionServiceImpl();

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
        String weIdPrivKey = KeyUtil
            .getPrivateKeyByWeId(KeyUtil.SDK_PRIVKEY_PATH, weId);
        System.out.println("Private Key is: " + weIdPrivKey);

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
        funcArgMap.put("name",
            "id" + Math.round(Math.random() * 1000) + Math.round(Math.random() * 1000));
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
            .assertTrue(
                (resp3.getRespBody().toString()).equalsIgnoreCase(Boolean.FALSE.toString()));

        // register authority Issuer (use SDK privkey, should succeed)
        funcArgMap = new LinkedHashMap<>();
        funcArgMap.put("weId", weId);
        funcArgMap.put("name", "id" + System.currentTimeMillis());
        txnArgMap = new LinkedHashMap<>();
        String adminPrivKey = KeyUtil.getPrivateKeyByWeId(KeyUtil.SDK_PRIVKEY_PATH,
            PropertiesUtil.getProperty("default.passphrase"));
        KeyUtil.savePrivateKey(KeyUtil.SDK_PRIVKEY_PATH, "0xffffffff", adminPrivKey);
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
        Assert
            .assertTrue((resp4.getRespBody().toString()).equalsIgnoreCase(Boolean.TRUE.toString()));

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
        cptJsonSchemaMap.put("weid", "0x11111111");
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
        funcArgMap.put("expirationDate", "2021-01-01T21:12:33Z");
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
        Assert.assertTrue((Boolean) resp9.getRespBody());

        // create credentialPojo using the same cpt id
        txnArgMap = new LinkedHashMap<>();
        txnArgMap.put(WeIdentityParamKeyConstant.KEY_INDEX, weId);
        inputParamMap = new LinkedHashMap<>();
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_ARG, funcArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.TRANSACTION_ARG, txnArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.API_VERSION,
            WeIdentityParamKeyConstant.DEFAULT_API_VERSION);
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_NAME,
            WeIdentityFunctionNames.FUNCNAME_CREATE_CREDENTIALPOJO);
        HttpResponseData<Object> resp10 =
            transactionService.invokeFunction(JsonUtil.objToJsonStr(inputParamMap));
        System.out.println(JsonUtil.objToJsonStr(resp10));
        Assert.assertNotNull(resp10.getRespBody());

        // verify credentialPojo
        credJsonMap = (Map<String, Object>) resp10.getRespBody();
        txnArgMap = new LinkedHashMap<>();
        inputParamMap = new LinkedHashMap<>();
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_ARG, credJsonMap);
        inputParamMap.put(WeIdentityParamKeyConstant.TRANSACTION_ARG, txnArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.API_VERSION,
            WeIdentityParamKeyConstant.DEFAULT_API_VERSION);
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_NAME,
            WeIdentityFunctionNames.FUNCNAME_VERIFY_CREDENTIALPOJO);
        HttpResponseData<Object> resp11 =
            transactionService.invokeFunction(JsonUtil.objToJsonStr(inputParamMap));
        System.out.println(JsonUtil.objToJsonStr(resp11));
        Assert.assertTrue((Boolean) resp11.getRespBody());

        txnArgMap = new LinkedHashMap<>();
        txnArgMap.put(WeIdentityParamKeyConstant.KEY_INDEX, weId);
        inputParamMap = new LinkedHashMap<>();
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_ARG, funcArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.TRANSACTION_ARG, txnArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.API_VERSION,
            WeIdentityParamKeyConstant.DEFAULT_API_VERSION);
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_NAME,
            WeIdentityFunctionNames.FUNCNAME_ECCENCRYPT_CREDENTIAL);
        HttpResponseData<Object> resp12 =
            transactionService.invokeFunction(JsonUtil.objToJsonStr(inputParamMap));
        System.out.println(JsonUtil.objToJsonStr(resp12));
        Assert.assertNotNull(resp12.getRespBody());

        String data = (String) resp12.getRespBody();
        txnArgMap = new LinkedHashMap<>();
        txnArgMap.put(WeIdentityParamKeyConstant.KEY_INDEX, weId);
        funcArgMap = new LinkedHashMap<>();
        funcArgMap.put("data", data);
        inputParamMap = new LinkedHashMap<>();
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_ARG, funcArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.TRANSACTION_ARG, txnArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.API_VERSION,
            WeIdentityParamKeyConstant.DEFAULT_API_VERSION);
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_NAME,
            WeIdentityFunctionNames.FUNCNAME_ECCDECRYPT);
        HttpResponseData<Object> resp13 =
            transactionService.invokeFunction(JsonUtil.objToJsonStr(inputParamMap));
        System.out.println(JsonUtil.objToJsonStr(resp13));
        Assert.assertNotNull(resp13.getRespBody());

        // test pure encrypt and decrypt
        data = "12345";
        txnArgMap = new LinkedHashMap<>();
        txnArgMap.put(WeIdentityParamKeyConstant.KEY_INDEX, weId);
        funcArgMap = new LinkedHashMap<>();
        funcArgMap.put("data", data);
        inputParamMap = new LinkedHashMap<>();
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_ARG, funcArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.TRANSACTION_ARG, txnArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.API_VERSION,
            WeIdentityParamKeyConstant.DEFAULT_API_VERSION);
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_NAME,
            WeIdentityFunctionNames.FUNCNAME_ECCENCRYPT);
        HttpResponseData<Object> resp14 =
            transactionService.invokeFunction(JsonUtil.objToJsonStr(inputParamMap));
        System.out.println(JsonUtil.objToJsonStr(resp14));
        Assert.assertNotNull(resp14.getRespBody());

        data = (String) resp14.getRespBody();
        txnArgMap = new LinkedHashMap<>();
        txnArgMap.put(WeIdentityParamKeyConstant.KEY_INDEX, weId);
        funcArgMap = new LinkedHashMap<>();
        funcArgMap.put("data", data);
        inputParamMap = new LinkedHashMap<>();
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_ARG, funcArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.TRANSACTION_ARG, txnArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.API_VERSION,
            WeIdentityParamKeyConstant.DEFAULT_API_VERSION);
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_NAME,
            WeIdentityFunctionNames.FUNCNAME_ECCDECRYPT);
        HttpResponseData<Object> resp15 =
            transactionService.invokeFunction(JsonUtil.objToJsonStr(inputParamMap));
        System.out.println(JsonUtil.objToJsonStr(resp15));
        Assert.assertNotNull(resp15.getRespBody());
    }

    @Test
    public void testInvokeEncodeIntegration() throws Exception {
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
        String weIdPrivKey = KeyUtil
            .getPrivateKeyByWeId(KeyUtil.SDK_PRIVKEY_PATH, weId);
        System.out.println("Private Key is: " + weIdPrivKey);

        // register cpt
        funcArgMap = new LinkedHashMap<>();
        funcArgMap.put("weId", weId);
        Map<String, Object> cptJsonSchemaMap = new LinkedHashMap<>();
        cptJsonSchemaMap.put("title", "a CPT schema");
        cptJsonSchemaMap.put("weid", "0x11111111");
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
        funcArgMap.put("expirationDate", "2021-01-01T21:12:33Z");
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
        Assert.assertTrue((Boolean) resp9.getRespBody());

        // create credentialPojo using the same cpt id
        txnArgMap = new LinkedHashMap<>();
        txnArgMap.put(WeIdentityParamKeyConstant.KEY_INDEX, weId);
        inputParamMap = new LinkedHashMap<>();
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_ARG, funcArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.TRANSACTION_ARG, txnArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.API_VERSION,
            WeIdentityParamKeyConstant.DEFAULT_API_VERSION);
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_NAME,
            WeIdentityFunctionNames.FUNCNAME_CREATE_CREDENTIALPOJO);
        HttpResponseData<Object> resp10 =
            transactionService.invokeFunction(JsonUtil.objToJsonStr(inputParamMap));
        System.out.println(JsonUtil.objToJsonStr(resp10));
        Assert.assertNotNull(resp10.getRespBody());

        // verify credentialPojo
        credJsonMap = (Map<String, Object>) resp10.getRespBody();
        CredentialPojo tempCred = DataToolUtils.mapToObj(credJsonMap, CredentialPojo.class);
        String rawDataStr = CredentialPojoUtils
            .getCredentialThumbprintWithoutSig(tempCred, tempCred.getSalt(), null);
        byte[] rawDataBytes = DataToolUtils.base64Decode(DataToolUtils.base64Encode(rawDataStr.getBytes(StandardCharsets.UTF_8)));
        ECKeyPair ecKeyPair = ECKeyPair.create(new BigInteger(weIdPrivKey));
        String signedSig2 = new String(DataToolUtils.base64Encode(DataToolUtils
            .simpleSignatureSerialization(Sign.signMessage(DataToolUtils.sha3(rawDataBytes), ecKeyPair))), StandardCharsets.UTF_8);
        System.out.println(signedSig2);
        txnArgMap = new LinkedHashMap<>();
        inputParamMap = new LinkedHashMap<>();
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_ARG, credJsonMap);
        inputParamMap.put(WeIdentityParamKeyConstant.TRANSACTION_ARG, txnArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.API_VERSION,
            WeIdentityParamKeyConstant.DEFAULT_API_VERSION);
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_NAME,
            WeIdentityFunctionNames.FUNCNAME_VERIFY_CREDENTIALPOJO);
        HttpResponseData<Object> resp11 =
            transactionService.invokeFunction(JsonUtil.objToJsonStr(inputParamMap));
        System.out.println(JsonUtil.objToJsonStr(resp11));
        Assert.assertTrue((Boolean) resp11.getRespBody());

        // test encode credential and verify
        txnArgMap = new LinkedHashMap<>();
        inputParamMap = new LinkedHashMap<>();
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_ARG, funcArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.TRANSACTION_ARG, txnArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.API_VERSION,
            WeIdentityParamKeyConstant.DEFAULT_API_VERSION);
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_NAME,
            WeIdentityFunctionNames.FUNCNAME_CREATE_CREDENTIALPOJO);
        HttpResponseData<Object> resp12 =
            transactionService.encodeTransaction(JsonUtil.objToJsonStr(inputParamMap));
        System.out.println(JsonUtil.objToJsonStr(resp12));
        Assert.assertNotNull(resp12.getRespBody());

        credJsonMap = (Map<String, Object>) resp12.getRespBody();
        Map<String, Object> proofMap = (HashMap<String, Object>) credJsonMap.get("proof");
        String sig = (String) proofMap.get("signatureValue");
        // step 1: client do base64 decode
        byte[] rawData = DataToolUtils.base64Decode(sig.getBytes(StandardCharsets.UTF_8));
        // step 2: client do an extra sha3
        byte[] hashedRawData = DataToolUtils.sha3(rawData);
        // step 3: client do sign
        org.fisco.bcos.web3j.crypto.ECKeyPair ecKeyPair2 =
            org.fisco.bcos.web3j.crypto.ECKeyPair.create(new BigInteger(weIdPrivKey));
        org.fisco.bcos.web3j.crypto.Sign.SignatureData sigData =
            org.fisco.bcos.web3j.crypto.Sign.getSignInterface().signMessage(hashedRawData, ecKeyPair2);
        // step 4: client do go-style serialization
        String signedSig = new String(DataToolUtils.base64Encode(TransactionEncoderUtilV2
            .goSignatureSerialization(sigData)), StandardCharsets.UTF_8);
        System.out.println(signedSig);
        proofMap.put("signatureValue", signedSig);
        credJsonMap.put("proof", proofMap);
        txnArgMap = new LinkedHashMap<>();
        inputParamMap = new LinkedHashMap<>();
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_ARG, credJsonMap);
        inputParamMap.put(WeIdentityParamKeyConstant.TRANSACTION_ARG, txnArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.API_VERSION,
            WeIdentityParamKeyConstant.DEFAULT_API_VERSION);
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_NAME,
            WeIdentityFunctionNames.FUNCNAME_VERIFY_CREDENTIALPOJO);
        HttpResponseData<Object> resp13 =
            transactionService.invokeFunction(JsonUtil.objToJsonStr(inputParamMap));
        System.out.println(JsonUtil.objToJsonStr(resp13));
        Assert.assertTrue((Boolean) resp13.getRespBody());
    }
}
