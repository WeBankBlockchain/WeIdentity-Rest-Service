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

import com.webank.weid.constant.WeIdConstant;
import com.webank.weid.contract.v2.WeIdContract.WeIdAttributeChangedEventResponse;
import com.webank.weid.protocol.response.ResponseData;
import com.webank.weid.protocol.response.TransactionInfo;
import com.webank.weid.rpc.RawTransactionService;
import com.webank.weid.service.BaseService;
import com.webank.weid.service.impl.RawTransactionServiceImpl;
import com.webank.weid.util.DateUtils;
import com.webank.weid.util.TransactionUtils;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.bcos.web3j.abi.FunctionEncoder;
import org.bcos.web3j.crypto.Credentials;
import org.bcos.web3j.crypto.ECKeyPair;
import org.bcos.web3j.crypto.GenCredential;
import org.bcos.web3j.crypto.Keys;
import org.bcos.web3j.crypto.Sign;
import org.bcos.web3j.crypto.Sign.SignatureData;
import org.bcos.web3j.utils.Numeric;
import org.fisco.bcos.web3j.abi.TypeReference;
import org.fisco.bcos.web3j.abi.datatypes.DynamicBytes;
import org.fisco.bcos.web3j.abi.datatypes.Type;
import org.fisco.bcos.web3j.abi.datatypes.generated.Bytes32;
import org.fisco.bcos.web3j.crypto.ExtendedRawTransaction;
import org.fisco.bcos.web3j.crypto.ExtendedTransactionEncoder;
import org.fisco.bcos.web3j.crypto.RawTransaction;
import org.fisco.bcos.web3j.crypto.SignedRawTransaction;
import org.fisco.bcos.web3j.crypto.TransactionDecoder;
import org.fisco.bcos.web3j.protocol.Web3j;
import org.fisco.bcos.web3j.protocol.core.methods.response.SendTransaction;
import org.fisco.bcos.web3j.protocol.core.methods.response.TransactionReceipt;
import org.fisco.bcos.web3j.solidity.Abi.Function;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.webank.weid.config.FiscoConfig;
import com.webank.weid.http.BaseTest;
import com.webank.weid.http.constant.WeIdentityFunctionNames;
import com.webank.weid.http.constant.WeIdentityParamKeyConstant;
import com.webank.weid.http.protocol.response.HttpResponseData;
import com.webank.weid.http.util.JsonUtil;
import com.webank.weid.http.util.KeyUtil;
import com.webank.weid.http.util.PropertiesUtil;
import com.webank.weid.http.util.TransactionEncoderUtil;
import com.webank.weid.util.DataToolUtils;
import com.webank.weid.util.WeIdUtils;

@Component
public class TransactionTest extends BaseTest {

    @Autowired
    TransactionService transactionService;
    @Autowired
    InvokerWeIdService invokerWeIdService;

    @Test
    public void testCreateWeIdV2All() throws Exception //{ExtendedRawTransaction rawTransaction, Credentials credentials,
    //String address, String data) {
    {
        FiscoConfig fiscoConfig = new FiscoConfig();
        fiscoConfig.load();
        String to = fiscoConfig.getWeIdAddress();
        // all steps:
        org.fisco.bcos.web3j.crypto.ECKeyPair ecKeyPair = org.fisco.bcos.web3j.crypto.Keys.createEcKeyPair();
        String newPublicKey = ecKeyPair.getPublicKey().toString();
        String weId = WeIdUtils.convertPublicKeyToWeId(newPublicKey);
        String addr = WeIdUtils.convertWeIdToAddress(weId);
        // 0. client generate nonce and send to server
        String nonce = TransactionEncoderUtil.getNonce().toString();
        // 1. server generate data
        // 下面为组装inputParameter
        byte[] byteValue = new byte[32];
        byte[] sourceByte = WeIdConstant.WEID_DOC_CREATED.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(sourceByte, 0, byteValue, 0, sourceByte.length);
        org.fisco.bcos.web3j.abi.datatypes.generated.Bytes32 bytes32val =
            new org.fisco.bcos.web3j.abi.datatypes.generated.Bytes32(byteValue);
        org.fisco.bcos.web3j.abi.datatypes.Function function = new org.fisco.bcos.web3j.abi.datatypes.Function(
            WeIdentityFunctionNames.FUNCCALL_SET_ATTRIBUTE,
            Arrays.<Type>asList(new org.fisco.bcos.web3j.abi.datatypes.Address(addr),
                bytes32val,
                new org.fisco.bcos.web3j.abi.datatypes.DynamicBytes(
                    "sampleType".getBytes(StandardCharsets.UTF_8)),
                new org.fisco.bcos.web3j.abi.datatypes.generated.Int256(new BigInteger("111111111"))),
            Collections.<TypeReference<?>>emptyList());
        String data = org.fisco.bcos.web3j.abi.FunctionEncoder.encode(function);
        // 2. server generate encodedTransaction
        Web3j web3j = (Web3j) BaseService.getWeb3j();
        ExtendedRawTransaction rawTransaction = TransactionEncoderUtil.buildRawTransactionV2(nonce,
            fiscoConfig.getGroupId(), web3j.getBlockNumberCache(), data, to);
        byte[] encodedTransaction = TransactionEncoderUtil.encodeV2(rawTransaction);
        // 3. server sends everything back to client in encoded base64 manner
        // 这一步先忽略
        // 4. client signs and sends back to send raw txn
        org.fisco.bcos.web3j.crypto.Sign.SignatureData clientSignedMsg = org.fisco.bcos.web3j.crypto.Sign
            .getSignInterface().signMessage(encodedTransaction, ecKeyPair);
        byte[] encodedSignedMsg = TransactionEncoderUtil.encode(rawTransaction, clientSignedMsg);
        //byte[] signedMessage = ExtendedTransactionEncoder.signMessage(rawTransaction, credentials);
        String txnHex = Numeric.toHexString(encodedSignedMsg);
        SendTransaction sendTransaction = web3j.sendRawTransaction(txnHex).sendAsync()
            .get(WeIdConstant.TRANSACTION_RECEIPT_TIMEOUT, TimeUnit.SECONDS);
        Optional<TransactionReceipt> receiptOptional =
            getTransactionReceiptRequest(sendTransaction.getTransactionHash());
        TransactionReceipt receipt = receiptOptional.get();
        System.out.println(receipt.getTransactionHash());
    }

    @Test
    public void testRemoveAuthIssuer() throws Exception
    {
        FiscoConfig fiscoConfig = new FiscoConfig();
        fiscoConfig.load();
        String to = fiscoConfig.getIssuerAddress();
        // all steps:
        org.fisco.bcos.web3j.crypto.ECKeyPair ecKeyPair = org.fisco.bcos.web3j.crypto.Keys.createEcKeyPair();
        org.fisco.bcos.web3j.crypto.Credentials credentials = org.fisco.bcos.web3j.crypto.Credentials.create(ecKeyPair);
        String newPublicKey = ecKeyPair.getPublicKey().toString();
        String weId = WeIdUtils.convertPublicKeyToWeId(newPublicKey);
        String addr = WeIdUtils.convertWeIdToAddress(weId);
        // 0. client generate nonce and send to server
        String nonce = TransactionEncoderUtil.getV2Nonce().toString();
        // 1. server generate data
        // 下面为组装inputParameter
        org.fisco.bcos.web3j.abi.datatypes.Function function = new org.fisco.bcos.web3j.abi.datatypes.Function(
            "removeAuthorityIssuer",
            Arrays.<Type>asList(new org.fisco.bcos.web3j.abi.datatypes.Address(addr)),
            Collections.<TypeReference<?>>emptyList());
        String data = org.fisco.bcos.web3j.abi.FunctionEncoder.encode(function);
        // 2. server generate encodedTransaction
        Web3j web3j = (Web3j) BaseService.getWeb3j();
        ExtendedRawTransaction rawTransaction = TransactionEncoderUtil.buildRawTransactionV2(nonce,
            fiscoConfig.getGroupId(), web3j.getBlockNumberCache(), data, to);
        // 3. server sends everything back to client in encoded base64 manner
        // 这一步先忽略
        // 4. client signs and sends back to send raw txn
        byte[] signedMessage = ExtendedTransactionEncoder.signMessage(rawTransaction, credentials);
        String txnHex = Numeric.toHexString(signedMessage);
        SendTransaction sendTransaction = web3j.sendRawTransaction(txnHex).sendAsync()
            .get(WeIdConstant.TRANSACTION_RECEIPT_TIMEOUT, TimeUnit.SECONDS);
        Optional<TransactionReceipt> receiptOptional =
            getTransactionReceiptRequest(sendTransaction.getTransactionHash());
        TransactionReceipt receipt = receiptOptional.get();
    }

    private Optional<TransactionReceipt> getTransactionReceiptRequest(String transactionHash) {
        Optional<TransactionReceipt> receiptOptional = Optional.empty();
        Web3j web3j = (Web3j) BaseService.getWeb3j();
        try {
            for (int i = 0; i < 5; i++) {
                receiptOptional = web3j.getTransactionReceipt(transactionHash).send().getTransactionReceipt();
                if (!receiptOptional.isPresent()) {
                    Thread.sleep(1000);
                } else {
                    return receiptOptional;
                }
            }
        } catch (IOException | InterruptedException e) {
            System.out.println();
        }
        return receiptOptional;
    }
    @Test
    public void TestWeIdAll() throws Exception {

//        FiscoConfig fiscoConfig = new FiscoConfig();
//        fiscoConfig.load();
//        if (fiscoConfig.getVersion().startsWith("2")) {
//            return;
//        }
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
        SignatureData bodySigned = Sign.signMessage(encodedTransaction, ecKeyPair);
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
//        FiscoConfig fiscoConfig = new FiscoConfig();
//        fiscoConfig.load();
//        if (fiscoConfig.getVersion().startsWith("2")) {
//            return;
//        }
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
        SignatureData bodySigned = Sign.signMessage(encodedTransaction, ecKeyPair);
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
//        FiscoConfig fiscoConfig = new FiscoConfig();
//        fiscoConfig.load();
//        if (fiscoConfig.getVersion().startsWith("2")) {
//            return;
//        }
        //step 1: create a WeID as the cpt creator (we let alone the authority issuer business)
        String issuerWeId = invokerWeIdService.createWeId().getResult().getWeId();
        Assert.assertTrue(invokerWeIdService.isWeIdExist(issuerWeId).getResult());

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
        SignatureData bodySigned = Sign.signMessage(encodedTransaction, ecKeyPair);
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
