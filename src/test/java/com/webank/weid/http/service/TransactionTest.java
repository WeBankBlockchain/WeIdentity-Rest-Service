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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webank.weid.http.BaseTest;
import com.webank.weid.http.constant.WeIdentityFunctionNames;
import com.webank.weid.http.constant.WeIdentityParamKeyConstant;
import com.webank.weid.http.protocol.response.HttpResponseData;
import com.webank.weid.http.service.impl.InvokerWeIdServiceImpl;
import com.webank.weid.http.service.impl.TransactionServiceImpl;
import com.webank.weid.http.util.JsonUtil;
import com.webank.weid.http.util.KeyUtil;
import com.webank.weid.http.util.PropertiesUtil;
import com.webank.weid.http.util.TransactionEncoderUtil;
import com.webank.weid.util.DataToolUtils;
import com.webank.weid.util.WeIdUtils;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;

import org.fisco.bcos.web3j.crypto.Credentials;
import org.fisco.bcos.web3j.crypto.ECKeyPair;
import org.fisco.bcos.web3j.crypto.Keys;
import org.fisco.bcos.web3j.crypto.Sign;
import org.fisco.bcos.web3j.crypto.Sign.SignatureData;
import org.fisco.bcos.web3j.crypto.gm.GenCredential;
import org.junit.Assert;
import org.junit.Test;

public class TransactionTest extends BaseTest {

    TransactionService transactionService = new TransactionServiceImpl();
    InvokerWeIdService invokerWeIdService = new InvokerWeIdServiceImpl();

    @Test
    public void TestWeIdAll() throws Exception {
        if (!TransactionEncoderUtil.isFiscoBcosV1()) {
            return;
        }
        ECKeyPair ecKeyPair = Keys.createEcKeyPair();
        String newPublicKey = ecKeyPair.getPublicKey().toString();
        String weId = WeIdUtils.convertPublicKeyToWeId(newPublicKey);
        Assert.assertFalse(invokerWeIdService.isWeIdExist(weId).getResult());
        String nonceVal = TransactionEncoderUtil.getNonce().toString();

        Map<String, Object> funcArgMap = new LinkedHashMap<>();
        funcArgMap.put("publicKey", newPublicKey);
        Map<String, Object> txnArgMap = new LinkedHashMap<>();
        txnArgMap.put(WeIdentityParamKeyConstant.NONCE, nonceVal);
        Map<String, Object> inputParamMap = new LinkedHashMap<>();
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_ARG, funcArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.TRANSACTION_ARG, txnArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.API_VERSION,
            WeIdentityParamKeyConstant.DEFAULT_API_VERSION);
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_NAME,
            WeIdentityFunctionNames.FUNCNAME_CREATE_WEID);
        HttpResponseData<Object> resp1 =
            transactionService.encodeTransaction(JsonUtil.objToJsonStr(inputParamMap));
        System.out.println(resp1);

        JsonNode encodeResult = new ObjectMapper()
            .readTree(JsonUtil.objToJsonStr(resp1.getRespBody()));
        String data = encodeResult.get("data").textValue();
        byte[] encodedTransaction = DataToolUtils
            .base64Decode(encodeResult.get("encodedTransaction").textValue().getBytes());
        SignatureData bodySigned = Sign.getSignInterface().signMessage(encodedTransaction, ecKeyPair);
        String signedMsg = new String(
            DataToolUtils.base64Encode(DataToolUtils.simpleSignatureSerialization(bodySigned)));

        funcArgMap = new LinkedHashMap<>();
        txnArgMap = new LinkedHashMap<>();
        txnArgMap.put(WeIdentityParamKeyConstant.NONCE, nonceVal);
        txnArgMap.put(WeIdentityParamKeyConstant.TRANSACTION_DATA, data);
        txnArgMap.put(WeIdentityParamKeyConstant.SIGNED_MESSAGE, signedMsg);
        inputParamMap = new LinkedHashMap<>();
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_ARG, funcArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.TRANSACTION_ARG, txnArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.API_VERSION,
            WeIdentityParamKeyConstant.DEFAULT_API_VERSION);
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_NAME,
            WeIdentityFunctionNames.FUNCNAME_CREATE_WEID);
        HttpResponseData<Object> resp2 =
            transactionService.sendTransaction(JsonUtil.objToJsonStr(inputParamMap));
        System.out.println(resp2);
        Assert.assertTrue(invokerWeIdService.isWeIdExist(weId).getResult());
        System.out.println("txn hex check done, step 2 done");

        //test getweiddocument w/ invoke
        funcArgMap = new LinkedHashMap<>();
        funcArgMap.put("weId", weId);
        txnArgMap = new LinkedHashMap<>();
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_ARG, funcArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.TRANSACTION_ARG, txnArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.API_VERSION,
            WeIdentityParamKeyConstant.DEFAULT_API_VERSION);
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_NAME,
            WeIdentityFunctionNames.FUNCNAME_GET_WEID_DOCUMENT);
        HttpResponseData<Object> resp3 =
            transactionService.invokeFunction(JsonUtil.objToJsonStr(inputParamMap));
        System.out.println(resp3);
        System.out.println("invoke done, step 3 done");
    }

    @Test
    public void TestAuthorityIssuerAll() throws Exception {
        if (!TransactionEncoderUtil.isFiscoBcosV1()) {
            return;
        }
        //step 1: create a WeID as the issuer
        String issuerWeId = invokerWeIdService.createWeId().getResult().getWeId();

        // step 2: prepare param
        String nonceVal = TransactionEncoderUtil.getNonce().toString();
        Map<String, Object> funcArgMap = new LinkedHashMap<>();
        funcArgMap.put("weId", issuerWeId);
        funcArgMap.put("name",
            "id" + Math.round(Math.random() * 1000) + Math.round(Math.random() * 1000));
        Map<String, Object> txnArgMap = new LinkedHashMap<>();
        txnArgMap.put(WeIdentityParamKeyConstant.NONCE, nonceVal);
        Map<String, Object> inputParamMap;
        inputParamMap = new LinkedHashMap<>();
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_ARG, funcArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.TRANSACTION_ARG, txnArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.API_VERSION,
            WeIdentityParamKeyConstant.DEFAULT_API_VERSION);
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_NAME,
            WeIdentityFunctionNames.FUNCNAME_REGISTER_AUTHORITY_ISSUER);
        HttpResponseData<Object> resp1 =
            transactionService.encodeTransaction(JsonUtil.objToJsonStr(inputParamMap));
        System.out.println("step 1 done: " + resp1);

        // step 3: sign via SDK privKey
        // note that the authorityIssuer creation can only be done by god account - for now
        String adminPrivKey = KeyUtil.getPrivateKeyByWeId(KeyUtil.SDK_PRIVKEY_PATH,
            PropertiesUtil.getProperty("default.passphrase"));
        BigInteger decValue = new BigInteger(adminPrivKey, 10);
        Credentials credentials = GenCredential.create(decValue.toString(16));
        ECKeyPair ecKeyPair = credentials.getEcKeyPair();
        JsonNode encodeResult = new ObjectMapper()
            .readTree(JsonUtil.objToJsonStr(resp1.getRespBody()));
        String data = encodeResult.get("data").textValue();
        byte[] encodedTransaction = DataToolUtils
            .base64Decode(encodeResult.get("encodedTransaction").textValue().getBytes());
        SignatureData bodySigned = Sign.getSignInterface().signMessage(encodedTransaction, ecKeyPair);
        String signedMsg = new String(
            DataToolUtils.base64Encode(DataToolUtils.simpleSignatureSerialization(bodySigned)));
        System.out.println("step 2 done, sig: " + signedMsg);

        // step 4: send
        funcArgMap = new LinkedHashMap<>();
        txnArgMap = new LinkedHashMap<>();
        txnArgMap.put(WeIdentityParamKeyConstant.NONCE, nonceVal);
        txnArgMap.put(WeIdentityParamKeyConstant.TRANSACTION_DATA, data);
        txnArgMap.put(WeIdentityParamKeyConstant.SIGNED_MESSAGE, signedMsg);
        inputParamMap = new LinkedHashMap<>();
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_ARG, funcArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.TRANSACTION_ARG, txnArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.API_VERSION,
            WeIdentityParamKeyConstant.DEFAULT_API_VERSION);
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_NAME,
            WeIdentityFunctionNames.FUNCNAME_REGISTER_AUTHORITY_ISSUER);
        HttpResponseData<Object> resp2 =
            transactionService.sendTransaction(JsonUtil.objToJsonStr(inputParamMap));
        System.out.println(resp2);

        // step 5: query
        funcArgMap = new LinkedHashMap<>();
        funcArgMap.put("weId", issuerWeId);
        txnArgMap = new LinkedHashMap<>();
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_ARG, funcArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.TRANSACTION_ARG, txnArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.API_VERSION,
            WeIdentityParamKeyConstant.DEFAULT_API_VERSION);
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_NAME,
            WeIdentityFunctionNames.FUNCNAME_QUERY_AUTHORITY_ISSUER);
        HttpResponseData<Object> resp3 =
            transactionService.invokeFunction(JsonUtil.objToJsonStr(inputParamMap));
        System.out.println(resp3);
        System.out.println("invoke done, step 3 done");
    }

    @Test
    public void TestCptAll() throws Exception {
        if (!TransactionEncoderUtil.isFiscoBcosV1()) {
            return;
        }
        //step 1: create a WeID as the cpt creator (we let alone the authority issuer business)
        String issuerWeId = invokerWeIdService.createWeId().getResult().getWeId();

        // step 2: prepare param
        // note that the authorityIssuer creation can only be done by god account - for now
        String nonceVal = TransactionEncoderUtil.getNonce().toString();
        Map<String, Object> funcArgMap = new LinkedHashMap<>();
        funcArgMap.put("weId", issuerWeId);
        String cptSignature = "HJPbDmoi39xgZBGi/aj1zB6VQL5QLyt4qTV6GOvQwzfgUJEZTazKZXe1dRg5aCt8Q44GwNF2k+l1rfhpY1hc/ls=";
        funcArgMap.put("cptSignature", cptSignature);
        Map<String, Object> cptJsonSchemaMap = new LinkedHashMap<>();
        cptJsonSchemaMap.put("title", "a CPT schema");
        funcArgMap.put("cptJsonSchema", cptJsonSchemaMap);
        Map<String, Object> txnArgMap = new LinkedHashMap<>();
        txnArgMap.put(WeIdentityParamKeyConstant.NONCE, nonceVal);
        Map<String, Object> inputParamMap;
        inputParamMap = new LinkedHashMap<>();
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_ARG, funcArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.TRANSACTION_ARG, txnArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.API_VERSION,
            WeIdentityParamKeyConstant.DEFAULT_API_VERSION);
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_NAME,
            WeIdentityFunctionNames.FUNCNAME_REGISTER_CPT);
        HttpResponseData<Object> resp1 =
            transactionService.encodeTransaction(JsonUtil.objToJsonStr(inputParamMap));
        System.out.println("step 1 done: " + resp1);

        // step 3: sign via SDK privKey
        // let us use god account for low-bit cptId for good
        String adminPrivKey = KeyUtil.getPrivateKeyByWeId(KeyUtil.SDK_PRIVKEY_PATH,
            PropertiesUtil.getProperty("default.passphrase"));
        BigInteger decValue = new BigInteger(adminPrivKey, 10);
        Credentials credentials = GenCredential.create(decValue.toString(16));
        ECKeyPair ecKeyPair = credentials.getEcKeyPair();
        JsonNode encodeResult = new ObjectMapper()
            .readTree(JsonUtil.objToJsonStr(resp1.getRespBody()));
        String data = encodeResult.get("data").textValue();
        byte[] encodedTransaction = DataToolUtils
            .base64Decode(encodeResult.get("encodedTransaction").textValue().getBytes());
        SignatureData bodySigned = Sign.getSignInterface().signMessage(encodedTransaction, ecKeyPair);
        String signedMsg = new String(
            DataToolUtils.base64Encode(DataToolUtils.simpleSignatureSerialization(bodySigned)));
        System.out.println("step 2 done, sig: " + signedMsg);

        // step 4: send
        funcArgMap = new LinkedHashMap<>();
        txnArgMap = new LinkedHashMap<>();
        txnArgMap.put(WeIdentityParamKeyConstant.NONCE, nonceVal);
        txnArgMap.put(WeIdentityParamKeyConstant.TRANSACTION_DATA, data);
        txnArgMap.put(WeIdentityParamKeyConstant.SIGNED_MESSAGE, signedMsg);
        inputParamMap = new LinkedHashMap<>();
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_ARG, funcArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.TRANSACTION_ARG, txnArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.API_VERSION,
            WeIdentityParamKeyConstant.DEFAULT_API_VERSION);
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_NAME,
            WeIdentityFunctionNames.FUNCNAME_REGISTER_CPT);
        HttpResponseData<Object> resp2 =
            transactionService.sendTransaction(JsonUtil.objToJsonStr(inputParamMap));
        System.out.println(resp2);

        // step 5: queryCpt
        /*
        JsonNode jsonNode = new ObjectMapper().readTree(JsonUtil.objToJsonStr(resp2.getRespBody()));
        Integer cptId = Integer.valueOf(jsonNode.get("cptId").toString());
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
        HttpResponseData<Object> resp3 =
            transactionService.invokeFunction(JsonUtil.objToJsonStr(inputParamMap));
        System.out.println(resp3);
        System.out.println("invoke done, step 3 done");
        */
    }
}
