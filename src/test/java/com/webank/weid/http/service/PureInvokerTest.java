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

import com.webank.weid.blockchain.service.fisco.CryptoFisco;
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
import com.webank.weid.protocol.response.RsvSignature;
import com.webank.weid.util.CredentialPojoUtils;
import com.webank.weid.util.DataToolUtils;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.fisco.bcos.sdk.abi.datatypes.generated.Bytes32;
import org.fisco.bcos.sdk.abi.datatypes.generated.Uint8;
import org.fisco.bcos.sdk.crypto.CryptoSuite;
import org.fisco.bcos.sdk.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.crypto.signature.SignatureResult;
import org.fisco.bcos.sdk.model.CryptoType;
import org.fisco.bcos.sdk.utils.Hex;
import org.fisco.bcos.sdk.utils.Numeric;
import org.junit.Assert;
import org.junit.Test;


import javax.xml.crypto.Data;

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

        // register authority issuer, should success
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
                (resp3.getRespBody().toString()).equalsIgnoreCase(Boolean.TRUE.toString()));

        //repeat register authority Issuer (use SDK privkey, should failed)
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
            .assertTrue((resp4.getRespBody().toString()).equalsIgnoreCase(Boolean.FALSE.toString()));

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
        funcArgMap.put("expirationDate", "2023-01-01T21:12:33Z");
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
        txnArgMap.put(WeIdentityParamKeyConstant.KEY_INDEX, "private_key");
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
        txnArgMap.put(WeIdentityParamKeyConstant.KEY_INDEX, "private_key");
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
        funcArgMap.put("expirationDate", "2023-01-01T21:12:33Z");
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
        BigInteger db = new BigInteger(weIdPrivKey, 10);
        CryptoKeyPair ecKeyPair = CryptoFisco.cryptoSuite.createKeyPair(db.toString(16));
        String signedSig2 = new String(DataToolUtils.base64Encode(DataToolUtils
            .SigBase64Serialization(DataToolUtils.signToRsvSignature(DataToolUtils.hash(rawDataBytes).toString(), DataToolUtils.hexStr2DecStr(ecKeyPair.getHexPrivateKey()))).getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
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
        CryptoKeyPair ecKeyPair2 = CryptoFisco.cryptoSuite.createKeyPair(db.toString(16));
        RsvSignature sigData = DataToolUtils
            .signToRsvSignature(rawData.toString(), DataToolUtils.hexStr2DecStr(ecKeyPair2.getHexPrivateKey()));
        String signedSig = DataToolUtils.SigBase64Serialization(sigData);
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
        //替换了proof中的签名，由于上面的签名方法不对，所以credential检验不通过
        Assert.assertFalse((Boolean) resp13.getRespBody());
    }

    @Test
    public void testPubKeyValidity() {
        CryptoKeyPair ecKeyPair;
        int failed = 0;
        int totalRuns = 1000;
        for (int i = 0; i < totalRuns; i++) {
            ecKeyPair = CryptoFisco.cryptoSuite.createKeyPair();
            int times = 0;
            while (!KeyUtil.isKeyPairValid(Numeric.hexStringToByteArray(ecKeyPair.getHexPrivateKey()), Numeric.hexStringToByteArray(ecKeyPair.getHexPublicKey()))) {
                ecKeyPair = CryptoFisco.cryptoSuite.createKeyPair();
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
        CryptoKeyPair ecKeyPair = CryptoFisco.cryptoSuite.createKeyPair();
        while (!KeyUtil.isKeyPairValid(Numeric.hexStringToByteArray(ecKeyPair.getHexPrivateKey()), Numeric.hexStringToByteArray(ecKeyPair.getHexPublicKey()))) {
            ecKeyPair = CryptoFisco.cryptoSuite.createKeyPair();
            System.out.println("Re-generating key pair..");
        }
    }

    @Test
    public void testSpecialInvokeIntegration() throws Exception {
        TransactionService transactionService = new TransactionServiceImpl();
        CryptoKeyPair ecKeyPair;
        byte[] pubkeybytes = new byte[64];
        while (!KeyUtil.isPubkeyBytesValid(pubkeybytes)) {
            ecKeyPair = CryptoFisco.cryptoSuite.createKeyPair();
            pubkeybytes = Numeric.hexStringToByteArray(ecKeyPair.getHexPublicKey());
            System.out.println("Re-generating public key..");
        }
        Map<String, Object> funcArgMap = new LinkedHashMap<>();
        Map<String, Object> txnArgMap = new LinkedHashMap<>();
        Map<String, Object> inputParamMap = new LinkedHashMap<>();
        String adminPrivKey = KeyUtil.getPrivateKeyByWeId(KeyUtil.SDK_PRIVKEY_PATH,
            PropertiesUtil.getProperty("default.passphrase"));
        KeyUtil.savePrivateKey(KeyUtil.SDK_PRIVKEY_PATH, "0xffffffff", adminPrivKey);
        txnArgMap.put(WeIdentityParamKeyConstant.KEY_INDEX, "0xffffffff");
        String pubkeyBase64Str = new String(DataToolUtils.base64Encode(pubkeybytes));
        System.out.println("Original pubkey base64: " + pubkeyBase64Str);
        funcArgMap.put(WeIdentityParamKeyConstant.PUBKEY_ECDSA, pubkeyBase64Str);
        String pubkeySM2Str = new String(DataToolUtils.base64Encode(Numeric.hexStringToByteArray(
                "da99f21026f0b214e03ec2ed61473621fd634507c62d9ddea6f0a2e474adf22914f4564eaaecfffb54e866cf9ab1bfba11e58a7cd8b09ddc22cf8da503211695")));
        funcArgMap.put(WeIdentityParamKeyConstant.PUBKEY_SM2, pubkeySM2Str);
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
        String hash = DataToolUtils.hash(credId);
        String sig = DataToolUtils.SigBase64Serialization(DataToolUtils.signToRsvSignature(hash, adminPrivKey));
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
        hash = DataToolUtils.hash(credId);
        sig = DataToolUtils.SigBase64Serialization(DataToolUtils.signToRsvSignature(hash, adminPrivKey));
        log = "temp";
        funcArgMap.put(WeIdentityParamKeyConstant.CREDENTIAL_ID, credId);
        funcArgMap.put(WeIdentityParamKeyConstant.HASH, hash);
        funcArgMap.put(WeIdentityParamKeyConstant.LOG, log);
        funcArgMap.put(WeIdentityParamKeyConstant.PROOF, sig);
        txnArgMap.put(WeIdentityParamKeyConstant.GROUP_ID, 1);
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
        String txHashECDSA = "64e604787cbf194841e7b68d7cd28786f6c9a0a3ab9f8b0a0e87cb4387ab0107";
        String txHashSM2 = "6e0f9e14344c5406a0cf5a3b4dfb665f87f4a771a31f7edbb5c72874a32b2957";
        String msg = "123";
        String FIXED_PUBKEY_HEX_HEADER = "04";
        String txSigECDSA = "284b99fde19ca4a2135a6c32e1b05cd8b12cbb732948bf8f6c1a6ffc729a3d1f38d32733f07e5b87eff194c53295a7679713ba3e10bf47c445fe5f26e7156d5a00";
        String txSigSM2 = "693efb73380304e30918bcbec1aaff80d5baf0d01676f5cb90d38cdb8608d3ad826d0633d8bc08d2d403ca5e997c08dfb57ac010e95f213d19c8242dbfd0171c00";

        // check hash and key
        byte[] hashBytes = DataToolUtils.hash(msg.getBytes());
        String hash = Numeric.toHexString(hashBytes);
        System.out.println("Converted hash: " + hash);
        if(CryptoFisco.cryptoSuite.getCryptoTypeConfig() == CryptoType.SM_TYPE){
        Assert.assertTrue(hash.equals(WeIdConstant.HEX_PREFIX + txHashSM2));}else {
            Assert.assertTrue(hash.equals(WeIdConstant.HEX_PREFIX + txHashECDSA));
        }

        // recover txsign
        byte[] txSigByte = Hex.decode(txSigECDSA);
        if(CryptoFisco.cryptoSuite.getCryptoTypeConfig() == CryptoType.SM_TYPE){
            txSigByte = Hex.decode(txSigSM2);}
        RsvSignature txSigData = new RsvSignature();
        Assert.assertEquals(txSigByte.length, 65);
        byte[] r = new byte[32];
        byte[] s = new byte[32];
        System.arraycopy(txSigByte, 0, r, 0, 32);
        System.arraycopy(txSigByte, 32, s, 0, 32);
        txSigData.setR(new Bytes32(r));
        txSigData.setS(new Bytes32(s));
        txSigData.setV(new Uint8(txSigByte[64]));
        //String trunctedTxPubkey = txHexPubKey.substring(2);
        CryptoKeyPair keyPair = CryptoFisco.cryptoSuite.createKeyPair(txHexPrivKey);
        //BigInteger txPubKeyBi = new BigInteger(trunctedTxPubkey, 16);
        BigInteger txPubKeyBi = new BigInteger(keyPair.getHexPublicKey(), 16);
        RsvSignature rsvSignature = DataToolUtils.signToRsvSignature(msg, DataToolUtils.hexStr2DecStr(txHexPrivKey));
        RsvSignature rsvSignature1 = DataToolUtils.signToRsvSignature(msg, DataToolUtils.hexStr2DecStr(txHexPrivKey));
        RsvSignature rsvSignature2 = DataToolUtils.signToRsvSignature(msg, DataToolUtils.hexStr2DecStr(txHexPrivKey));
        SignatureResult signatureResult = new CryptoSuite(0)
                .sign(new CryptoSuite(0).hash(msg), new CryptoSuite(0).getKeyPairFactory().createKeyPair(new BigInteger(DataToolUtils.hexStr2DecStr(txHexPrivKey))));
        SignatureResult signatureResult2 = new CryptoSuite(0)
                .sign(new CryptoSuite(0).hash(msg), new CryptoSuite(0).getKeyPairFactory().createKeyPair(new BigInteger(DataToolUtils.hexStr2DecStr(txHexPrivKey))));
        boolean result = DataToolUtils.verifySignature(msg, DataToolUtils.SigBase64Serialization(rsvSignature), txPubKeyBi);
        Assert.assertTrue(result);

        // send to txsign to verify
        CryptoKeyPair keyPair2 = CryptoFisco.cryptoSuite.createKeyPair(txHexPrivKey);
        RsvSignature sigData = DataToolUtils.signToRsvSignature(msg, DataToolUtils.hexStr2DecStr(keyPair2.getHexPrivateKey()));
        byte[] serializedSignatureData = new byte[65];
        serializedSignatureData[64] = sigData.getV().getValue().byteValue();
        System.arraycopy(sigData.getR().getValue(), 0, serializedSignatureData, 0, 32);
        System.arraycopy(sigData.getS().getValue(), 0, serializedSignatureData, 32, 32);
        String toHexStr = Hex.toHexString(serializedSignatureData);
        //由于国密的签名每次都不一样，所以这里先不比较
        //Assert.assertEquals(toHexStr, txSigSM2);

        // integration test
        String privKey = keyPair2.getHexPrivateKey();
        String pubKey = keyPair2.getHexPublicKey();
        System.out.println("privKey: " + privKey + ", pubkey: " + pubKey);

        String priv = "109133513592087805746587031475659996081883766162039886922465775418059633608266";
        BigInteger bi = new BigInteger(priv, 10);
        CryptoKeyPair keyPair3 = CryptoFisco.cryptoSuite.createKeyPair(bi.toString(16));
        System.out.println(DataToolUtils.hexStr2DecStr(keyPair3.getHexPublicKey()));
        System.out.println(DataToolUtils.hexStr2DecStr(keyPair3.getHexPrivateKey()));
    }

    @Test
    public void testHexBase64BigInt() throws Exception {
        CryptoKeyPair keyPair2 = CryptoFisco.cryptoSuite.createKeyPair();
        byte[] correctEncodedBase64Str = DataToolUtils.base64Encode(Hex.decode(keyPair2.getHexPublicKey()));
        System.out.println("直接获取hex " + keyPair2.getHexPublicKey());
        System.out.println("转换decima " + DataToolUtils.hexStr2DecStr(keyPair2.getHexPublicKey()));
        System.out.println("biginteger的base64 " + correctEncodedBase64Str);
        byte[] pubkey = DataToolUtils.base64Decode(correctEncodedBase64Str);
        //System.out.println(new String(pubkey));
        //Assert.assertEquals(new String(pubkey), keyPair2.getHexPublicKey());
        BigInteger bi2 = Numeric.toBigInt(pubkey);
        System.out.println("十六进制 " + bi2.toString(16));
        String dex = bi2.toString(10);
        System.out.println("十进制 " + dex);
        BigInteger db = new BigInteger(dex, 10);
        System.out.println("十进制转成的hex " + db.toString(16));
        Assert.assertEquals(db.toString(16), bi2.toString(16));

        String txHexPubKey = "dfa0a3c55931f26ced064a8f6f79770b44e8a04d183d26b1ff71bbf68fa26cfc6601f17fc9fe25a7179206294d9201ea46b435814bc96c9c80b71b17534d55a9";
        //String txBase64 = "APoqbCpDbA9zQANLVHR7IUn2CplkltRCydFdBkGzpoj8WCy+oo0fNF6FH950CygRQ/1anhkOYdC0RLIk4qhpruI=";
        String txBase64 = "9CkBtkl29d9vmWenOConzsUAJr4Q6pc21cDdlTLU2aZsqbgG8eSVfXs9rFV+tCe4mbEu1INjwGCHtiSayHzmhQ==";
        pubkey = DataToolUtils.base64Decode(txBase64.getBytes(StandardCharsets.UTF_8));
        System.out.println("new pubkey " + new String(pubkey));
        bi2 = Numeric.toBigInt(pubkey);
        System.out.println("new hex值 " + bi2.toString(16));
        //Assert.assertEquals(bi2.toString(16), txHexPubKey);
        System.out.println("十进制 " + bi2.toString(10));
        // Base64 <> hex conversion
        String hexFrom = Numeric.toHexStringNoPrefix(DataToolUtils.base64Decode(txBase64.getBytes(StandardCharsets.UTF_8)));
        System.out.println(hexFrom);
        String base64To = new String(DataToolUtils.base64Encode(Hex.decode(hexFrom)));
        System.out.println(base64To);

    }
}
