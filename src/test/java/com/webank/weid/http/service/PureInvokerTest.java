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
import com.webank.weid.constant.WeIdConstant;
import com.webank.weid.http.BaseTest;
import com.webank.weid.http.constant.HttpReturnCode;
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
import java.util.UUID;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.fisco.bcos.web3j.crypto.ECDSASign;
import org.fisco.bcos.web3j.crypto.ECKeyPair;
import org.fisco.bcos.web3j.crypto.Hash;
import org.fisco.bcos.web3j.crypto.Keys;
import org.fisco.bcos.web3j.crypto.Sign;
import org.fisco.bcos.web3j.crypto.gm.GenCredential;
import org.fisco.bcos.web3j.crypto.gm.sm2.util.encoders.Hex;
import org.fisco.bcos.web3j.utils.Numeric;
import org.junit.Assert;
import org.junit.Test;

public class PureInvokerTest extends BaseTest {


    @Test
    public void testInvokeIntegration() throws Exception {
        // create a WeID
        TransactionService transactionService = new TransactionServiceImpl();
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
        funcArgMap.put("expirationDate", "1687971840");
        funcArgMap.put(ParamKeyConstant.PROOF_TYPE, "lite");
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
        //Assert.assertNotNull(resp13.getRespBody());

        // test pure encrypt and decrypt
        data = "{\"name\":\"mark\",\"a\":10}";
        txnArgMap = new LinkedHashMap<>();
        txnArgMap.put(WeIdentityParamKeyConstant.KEY_INDEX, "ecdsa_key");
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
        txnArgMap.put(WeIdentityParamKeyConstant.KEY_INDEX, "ecdsa_key");
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
        TransactionService transactionService = new TransactionServiceImpl();
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
            .simpleSignatureSerialization(Sign.getSignInterface().signMessage(DataToolUtils.sha3(rawDataBytes), ecKeyPair))), StandardCharsets.UTF_8);
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
        // step 2: client do sign
        org.fisco.bcos.web3j.crypto.ECKeyPair ecKeyPair2 =
            org.fisco.bcos.web3j.crypto.ECKeyPair.create(new BigInteger(weIdPrivKey));
        ECDSASign ecdsaSign = new ECDSASign();
        org.fisco.bcos.web3j.crypto.Sign.SignatureData sigData = ecdsaSign
            .secp256SignMessage(rawData, ecKeyPair2);
        String signedSig = DataToolUtils.secp256k1SigBase64Serialization(sigData);
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

    @Test
    public void testPubKeyValidity() {
        org.fisco.bcos.web3j.crypto.ECKeyPair ecKeyPair;
        int failed = 0;
        int totalRuns = 10000;
        for (int i = 0; i < totalRuns; i++) {
            ecKeyPair = GenCredential.createKeyPair();
            int times = 0;
            while (!KeyUtil.isKeyPairValid(ecKeyPair)) {
                ecKeyPair = GenCredential.createKeyPair();
                times++;
            }
            System.out.println("Regenerate a valid one after times: " + times);
            if (times > 0) {
                failed++;
            }
        }
        System.out.println("Total failure rate: " + (double) failed / (double) totalRuns);
    }

    @Test
    public void testKeyValidity() {
        org.fisco.bcos.web3j.crypto.ECKeyPair ecKeyPair = GenCredential.createKeyPair();
        while (!KeyUtil.isKeyPairValid(ecKeyPair)) {
            ecKeyPair = GenCredential.createKeyPair();
            System.out.println("Re-generating key pair..");
        }
    }

    @Test
    public void testSpecialInvokeIntegration() throws Exception {
        TransactionService transactionService = new TransactionServiceImpl();
        org.fisco.bcos.web3j.crypto.ECKeyPair ecKeyPair;
        byte[] pubkeybytes = new byte[64];
        while (!KeyUtil.isPubkeyBytesValid(pubkeybytes)) {
            ecKeyPair = GenCredential.createKeyPair();
            pubkeybytes = ecKeyPair.getPublicKey().toByteArray();
            System.out.println("Re-generating public key..");
        }
        Map<String, Object> funcArgMap = new LinkedHashMap<>();
        Map<String, Object> txnArgMap = new LinkedHashMap<>();
        Map<String, Object> inputParamMap = new LinkedHashMap<>();
        String adminPrivKey = KeyUtil.getPrivateKeyByWeId(KeyUtil.SDK_PRIVKEY_PATH,
            PropertiesUtil.getProperty("default.passphrase"));
        KeyUtil.savePrivateKey(KeyUtil.SDK_PRIVKEY_PATH, "0xffffffff", adminPrivKey);
        txnArgMap.put(WeIdentityParamKeyConstant.KEY_INDEX, "0xffffffff");
        String pubkeyBase64Str = Base64.encodeBase64String(pubkeybytes);
        System.out.println("Original pubkey base64: " + pubkeyBase64Str);
        funcArgMap.put(WeIdentityParamKeyConstant.PUBKEY_SECP, pubkeyBase64Str);
        String pubkeyRsaStr = Base64.encodeBase64String(Numeric.hexStringToByteArray(
            "da99f21026f0b214e03ec2ed61473621fd634507c62d9ddea6f0a2e474adf22914f4564eaaecfffb54e866cf9ab1bfba11e58a7cd8b09ddc22cf8da503211695"));
        funcArgMap.put(WeIdentityParamKeyConstant.PUBKEY_RSA, pubkeyRsaStr);
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_ARG, funcArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.TRANSACTION_ARG, txnArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.API_VERSION,
            WeIdentityParamKeyConstant.DEFAULT_API_VERSION);
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_NAME,
            WeIdentityFunctionNames.FUNCNAME_CREATE_WEID_WITH_PUBKEY);
        HttpResponseData<Object> resp1 = transactionService
            .invokeFunction(JsonUtil.objToJsonStr(inputParamMap));
        System.out.println(JsonUtil.objToJsonStr(resp1));
        String weId = (String) resp1.getRespBody();
        Assert.assertTrue(!StringUtils.isEmpty(weId));
        String weIdPrivKey = KeyUtil
            .getPrivateKeyByWeId(KeyUtil.SDK_PRIVKEY_PATH, weId);
        System.out.println("Stored Private Key is: " + weIdPrivKey + ", should be empty/null");
        Assert.assertTrue(StringUtils.isEmpty(weIdPrivKey));

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
        HttpResponseData<Object> respget =
            transactionService.invokeFunction(JsonUtil.objToJsonStr(inputParamMap));
        System.out.println(JsonUtil.objToJsonStr(respget));
        Assert.assertTrue(respget.getRespBody() != null);

        // We firstly register this weid as an authority issuer:
        inputParamMap = new LinkedHashMap<>();
        funcArgMap = new LinkedHashMap<>();
        txnArgMap = new LinkedHashMap<>();
        funcArgMap.put("weId", weId);
        String orgId = "id" + System.currentTimeMillis();
        funcArgMap.put("name", orgId);
        txnArgMap.put(WeIdentityParamKeyConstant.KEY_INDEX, "0xffffffff");
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_ARG, funcArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.TRANSACTION_ARG, txnArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.API_VERSION,
            WeIdentityParamKeyConstant.DEFAULT_API_VERSION);
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_NAME,
            WeIdentityFunctionNames.FUNCNAME_REGISTER_AUTHORITY_ISSUER);
        HttpResponseData<Object> resp2 =
            transactionService.invokeFunction(JsonUtil.objToJsonStr(inputParamMap));
        System.out.println(JsonUtil.objToJsonStr(resp2));
        Assert
            .assertTrue((resp2.getRespBody().toString()).equalsIgnoreCase(Boolean.TRUE.toString()));

        // Search via orgId
        inputParamMap = new LinkedHashMap<>();
        funcArgMap = new LinkedHashMap<>();
        txnArgMap = new LinkedHashMap<>();
        funcArgMap.put(WeIdentityParamKeyConstant.ORG_ID, orgId);
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_ARG, funcArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.TRANSACTION_ARG, txnArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.API_VERSION,
            WeIdentityParamKeyConstant.DEFAULT_API_VERSION);
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_NAME,
            WeIdentityFunctionNames.FUNCNAME_GET_WEID_DOCUMENT_BY_ORG);
        HttpResponseData<Object> resp3 =
            transactionService.invokeFunction(JsonUtil.objToJsonStr(inputParamMap));
        System.out.println(JsonUtil.objToJsonStr(resp3));
        Assert.assertNotNull(resp3.getRespBody());

        // Create evidence
        funcArgMap = new LinkedHashMap<>();
        txnArgMap = new LinkedHashMap<>();
        String credId = UUID.randomUUID().toString();
        String hash = DataToolUtils.sha3(credId);
        String sig = DataToolUtils.secp256k1Sign(hash, new BigInteger(adminPrivKey));
        String log = "temp";
        funcArgMap.put(WeIdentityParamKeyConstant.CREDENTIAL_ID, credId);
        funcArgMap.put(WeIdentityParamKeyConstant.HASH, hash);
        funcArgMap.put(WeIdentityParamKeyConstant.LOG, log);
        funcArgMap.put(WeIdentityParamKeyConstant.PROOF, sig);
        inputParamMap = new LinkedHashMap<>();
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_ARG, funcArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.TRANSACTION_ARG, txnArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.API_VERSION,
            WeIdentityParamKeyConstant.DEFAULT_API_VERSION);
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_NAME,
            WeIdentityFunctionNames.FUNCNAME_CREATE_EVIDENCE_FOR_LITE_CREDENTIAL);
        HttpResponseData<Object> resp4 =
            transactionService.invokeFunction(JsonUtil.objToJsonStr(inputParamMap));
        System.out.println(JsonUtil.objToJsonStr(resp4));
        Assert.assertNotNull(resp4.getRespBody());

        // verify lite credential
        funcArgMap = new LinkedHashMap<>();
        txnArgMap = new LinkedHashMap<>();
        funcArgMap.put(WeIdentityParamKeyConstant.CREDENTIAL_ID, credId);
        inputParamMap = new LinkedHashMap<>();
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_ARG, funcArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.TRANSACTION_ARG, txnArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.API_VERSION,
            WeIdentityParamKeyConstant.DEFAULT_API_VERSION);
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_NAME,
            WeIdentityFunctionNames.FUNCNAME_VERIFY_LITE_CREDENTIAL);
        HttpResponseData<Object> resp5 =
            transactionService.invokeFunction(JsonUtil.objToJsonStr(inputParamMap));
        System.out.println(JsonUtil.objToJsonStr(resp5));
        Assert.assertNotNull(resp5.getRespBody());

        // Create evidence in another service set
        funcArgMap = new LinkedHashMap<>();
        txnArgMap = new LinkedHashMap<>();
        credId = UUID.randomUUID().toString();
        hash = DataToolUtils.sha3(credId);
        sig = DataToolUtils.secp256k1Sign(hash, new BigInteger(adminPrivKey));
        log = "temp";
        funcArgMap.put(WeIdentityParamKeyConstant.CREDENTIAL_ID, credId);
        funcArgMap.put(WeIdentityParamKeyConstant.HASH, hash);
        funcArgMap.put(WeIdentityParamKeyConstant.LOG, log);
        funcArgMap.put(WeIdentityParamKeyConstant.PROOF, sig);
        txnArgMap.put(WeIdentityParamKeyConstant.GROUP_ID, 3);
        inputParamMap = new LinkedHashMap<>();
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_ARG, funcArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.TRANSACTION_ARG, txnArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.API_VERSION,
            WeIdentityParamKeyConstant.DEFAULT_API_VERSION);
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_NAME,
            WeIdentityFunctionNames.FUNCNAME_CREATE_EVIDENCE_FOR_LITE_CREDENTIAL);
        HttpResponseData<Object> resp6 =
            transactionService.invokeFunction(JsonUtil.objToJsonStr(inputParamMap));
        System.out.println(JsonUtil.objToJsonStr(resp6));
        Assert.assertTrue(resp6.getErrorCode().intValue() == HttpReturnCode.WEB3J_ERROR.getCode() ||
            resp6.getErrorCode().intValue() == HttpReturnCode.CONTRACT_ERROR.getCode() ||
            resp6.getErrorCode().intValue() == HttpReturnCode.SUCCESS.getCode());

        // verify lite credential
        funcArgMap = new LinkedHashMap<>();
        txnArgMap = new LinkedHashMap<>();
        funcArgMap.put(WeIdentityParamKeyConstant.CREDENTIAL_ID, credId);
        txnArgMap.put(WeIdentityParamKeyConstant.GROUP_ID, 1);
        inputParamMap = new LinkedHashMap<>();
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_ARG, funcArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.TRANSACTION_ARG, txnArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.API_VERSION,
            WeIdentityParamKeyConstant.DEFAULT_API_VERSION);
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_NAME,
            WeIdentityFunctionNames.FUNCNAME_VERIFY_LITE_CREDENTIAL);
        HttpResponseData<Object> resp7 =
            transactionService.invokeFunction(JsonUtil.objToJsonStr(inputParamMap));
        System.out.println(JsonUtil.objToJsonStr(resp7));
        Assert.assertTrue(resp7.getErrorCode().intValue() == HttpReturnCode.WEB3J_ERROR.getCode() ||
            resp7.getErrorCode().intValue() == HttpReturnCode.CONTRACT_ERROR.getCode() ||
            resp7.getErrorCode().intValue() == HttpReturnCode.SUCCESS.getCode());
    }

    @Test
    public void testSecpSuite() throws Exception {
        //String txHexPubKey = "0483a7e164ccd8eed1276b95653f3d6a091cb1c4c0e8e9bb097c922f913bd78e4b9fbdf23c1c472a63b0ed24703165ca79e4523805017f8ab2d621e1998ebff0cf";
        String txHexPrivKey = "b774d1cdf59f98fc9ff027597a891a04e42f26965a9a9177d8a023ad91373d48";
        String txHash = "64e604787cbf194841e7b68d7cd28786f6c9a0a3ab9f8b0a0e87cb4387ab0107";
        String msg = "123";
        String FIXED_PUBKEY_HEX_HEADER = "04";
        String txSig = "284b99fde19ca4a2135a6c32e1b05cd8b12cbb732948bf8f6c1a6ffc729a3d1f38d32733f07e5b87eff194c53295a7679713ba3e10bf47c445fe5f26e7156d5a00";

        // check hash and key
        byte[] hashBytes = Hash.sha3(msg.getBytes());
        String hash = Numeric.toHexString(hashBytes);
        System.out.println("Converted hash: " + hash);
        Assert.assertTrue(hash.equals(WeIdConstant.HEX_PREFIX + txHash));

        // check sign
        ECDSASign ecdsaSign = new ECDSASign();

        // recover txsign
        byte[] txSigByte = Hex.decode(txSig);
        org.fisco.bcos.web3j.crypto.Sign.SignatureData txSigData;
        Assert.assertEquals(txSigByte.length, 65);
        byte[] r = new byte[32];
        byte[] s = new byte[32];
        System.arraycopy(txSigByte, 0, r, 0, 32);
        System.arraycopy(txSigByte, 32, s, 0, 32);
        txSigData = new org.fisco.bcos.web3j.crypto.Sign.SignatureData(txSigByte[64], r, s);
        //String trunctedTxPubkey = txHexPubKey.substring(2);
        org.fisco.bcos.web3j.crypto.ECKeyPair keyPair = org.fisco.bcos.web3j.crypto.ECKeyPair.create(Hex.decode(txHexPrivKey));
        //BigInteger txPubKeyBi = new BigInteger(trunctedTxPubkey, 16);
        BigInteger txPubKeyBi = keyPair.getPublicKey();
        boolean result = ecdsaSign.secp256Verify(hashBytes, txPubKeyBi, txSigData);
        Assert.assertTrue(result);

        // send to txsign to verify
        org.fisco.bcos.web3j.crypto.ECKeyPair keyPair2 = org.fisco.bcos.web3j.crypto.ECKeyPair.create(Hex.decode(txHexPrivKey));
        org.fisco.bcos.web3j.crypto.Sign.SignatureData sigData = ecdsaSign.secp256SignMessage(msg.getBytes(), keyPair2);
        byte[] serializedSignatureData = new byte[65];
        serializedSignatureData[64] = sigData.getV();
        System.arraycopy(sigData.getR(), 0, serializedSignatureData, 0, 32);
        System.arraycopy(sigData.getS(), 0, serializedSignatureData, 32, 32);
        String toHexStr = Hex.toHexString(serializedSignatureData);
        Assert.assertEquals(toHexStr, txSig);

        // integration test
        String privKey = keyPair2.getPrivateKey().toString(16);
        String pubKey = keyPair2.getPublicKey().toString(16);
        System.out.println("privKey: " + privKey + ", pubkey: " + pubKey);

        String priv = "109133513592087805746587031475659996081883766162039886922465775418059633608266";
        org.fisco.bcos.web3j.crypto.ECKeyPair keyPair3 = org.fisco.bcos.web3j.crypto.ECKeyPair.create(new BigInteger(priv));
        System.out.println(keyPair3.getPublicKey().toString(10));
        System.out.println(keyPair3.getPrivateKey().toString(10));
    }

    @Test
    public void testHexBase64BigInt() throws Exception {
        org.fisco.bcos.web3j.crypto.ECKeyPair keyPair2 = Keys.createEcKeyPair();
        String correctEncodedBase64Str = org.apache.commons.codec.binary.Base64
            .encodeBase64String(keyPair2.getPublicKey().toByteArray());
        System.out.println("biginteger直接转换toString hex " + keyPair2.getPublicKey().toString(16));
        System.out.println("biginteger的base64 " + correctEncodedBase64Str);
        byte[] pubkey = org.apache.commons.codec.binary.Base64.decodeBase64(correctEncodedBase64Str);
        BigInteger bi2 = Numeric.toBigInt(pubkey);
        System.out.println("base64往返转换 " + bi2.toString(16));
        Assert.assertEquals(bi2.toString(16), keyPair2.getPublicKey().toString(16));
        String dex = bi2.toString(10);
        System.out.println("十进制 " + dex);
        BigInteger db = new BigInteger(dex, 10);
        System.out.println("十进制转成的dex " + db.toString(16));
        Assert.assertEquals(db.toString(16), bi2.toString(16));

        String txHexPubKey = "dfa0a3c55931f26ced064a8f6f79770b44e8a04d183d26b1ff71bbf68fa26cfc6601f17fc9fe25a7179206294d9201ea46b435814bc96c9c80b71b17534d55a9";
        //String txBase64 = "APoqbCpDbA9zQANLVHR7IUn2CplkltRCydFdBkGzpoj8WCy+oo0fNF6FH950CygRQ/1anhkOYdC0RLIk4qhpruI=";
        String txBase64 = "9CkBtkl29d9vmWenOConzsUAJr4Q6pc21cDdlTLU2aZsqbgG8eSVfXs9rFV+tCe4mbEu1INjwGCHtiSayHzmhQ==";
        pubkey = org.apache.commons.codec.binary.Base64.decodeBase64(txBase64);
        bi2 = Numeric.toBigInt(pubkey);
        System.out.println("new hex值 " + bi2.toString(16));
        //Assert.assertEquals(bi2.toString(16), txHexPubKey);
        System.out.println("十进制 " + bi2.toString(10));
        System.out.println(org.apache.commons.codec.binary.Base64
            .encodeBase64String(Numeric.hexStringToByteArray(bi2.toString(16))));

        System.out.println();
        // Base64 <> hex conversion
        String hexFrom = Numeric.toHexStringNoPrefix(Base64.decodeBase64(txBase64));
        System.out.println(hexFrom);
        String base64To = Base64.encodeBase64String(Numeric.hexStringToByteArray(hexFrom));
        System.out.println(base64To);

    }
}
