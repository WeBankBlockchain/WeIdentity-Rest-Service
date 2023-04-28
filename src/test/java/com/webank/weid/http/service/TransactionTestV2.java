package com.webank.weid.http.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webank.weid.blockchain.config.FiscoConfig;
import com.webank.weid.blockchain.service.fisco.CryptoFisco;
import com.webank.weid.constant.WeIdConstant;
import com.webank.weid.contract.v2.AuthorityIssuerController;
import com.webank.weid.contract.v2.AuthorityIssuerController.AuthorityIssuerRetLogEventResponse;
import com.webank.weid.contract.v2.WeIdContract;
import com.webank.weid.http.constant.SignType;
import com.webank.weid.http.constant.WeIdentityFunctionNames;
import com.webank.weid.http.constant.WeIdentityParamKeyConstant;
import com.webank.weid.http.protocol.response.HttpResponseData;
import com.webank.weid.http.service.impl.InvokerWeIdServiceImpl;
import com.webank.weid.http.service.impl.TransactionServiceImpl;
import com.webank.weid.http.util.JsonUtil;
import com.webank.weid.http.util.KeyUtil;
import com.webank.weid.http.util.PropertiesUtil;
import com.webank.weid.http.util.TransactionEncoderUtil;
import com.webank.weid.http.util.TransactionEncoderUtilV2;
import com.webank.weid.protocol.response.RsvSignature;
import com.webank.weid.blockchain.service.fisco.BaseServiceFisco;
import com.webank.weid.util.DataToolUtils;
import com.webank.weid.util.DateUtils;
import com.webank.weid.util.WeIdUtils;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.bouncycastle.util.encoders.Base64;
import org.fisco.bcos.sdk.abi.FunctionEncoder;
import org.fisco.bcos.sdk.abi.TypeReference;
import org.fisco.bcos.sdk.abi.Utils;
import org.fisco.bcos.sdk.abi.datatypes.*;
import org.fisco.bcos.sdk.abi.datatypes.generated.Bytes32;
import org.fisco.bcos.sdk.abi.datatypes.generated.Int256;
import org.fisco.bcos.sdk.client.Client;
import org.fisco.bcos.sdk.client.protocol.response.SendTransaction;
import org.fisco.bcos.sdk.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.model.TransactionReceipt;
import org.fisco.bcos.sdk.transaction.codec.encode.TransactionEncoderService;
import org.fisco.bcos.sdk.transaction.model.po.RawTransaction;
import org.fisco.bcos.sdk.utils.Numeric;
import org.junit.Assert;
import org.junit.Test;

public class TransactionTestV2 {

    TransactionService transactionService = new TransactionServiceImpl();
    InvokerWeIdService invokerWeIdService = new InvokerWeIdServiceImpl();

    //@Test
    public void testCreateWeIdAll() throws Exception {
        if (TransactionEncoderUtil.isFiscoBcosV1()) {
            return;
        }

        // simulate client key-pair creation
        CryptoKeyPair ecKeyPair = CryptoFisco.cryptoSuite.createKeyPair();
        String newPublicKey = DataToolUtils.hexStr2DecStr(ecKeyPair.getHexPublicKey());
        String weId = WeIdUtils.convertPublicKeyToWeId(newPublicKey);
        String nonce = TransactionEncoderUtilV2.getNonce().toString();

        // simulate encode call
        Map<String, Object> funcArgMap = new LinkedHashMap<>();
        funcArgMap.put("publicKey", newPublicKey);
        Map<String, Object> txnArgMap = new LinkedHashMap<>();
        txnArgMap.put(WeIdentityParamKeyConstant.NONCE, nonce);
        Map<String, Object> inputParamMap = new LinkedHashMap<>();
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_ARG, funcArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.TRANSACTION_ARG, txnArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.API_VERSION,
            WeIdentityParamKeyConstant.DEFAULT_API_VERSION);
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_NAME,
            WeIdentityFunctionNames.FUNCNAME_CREATE_WEID);
        HttpResponseData<Object> resp1 =
            transactionService.encodeTransaction(JsonUtil.objToJsonStr(inputParamMap));

        // simulate client sign
        JsonNode encodeResult = new ObjectMapper()
            .readTree(JsonUtil.objToJsonStr(resp1.getRespBody()));
        String data = encodeResult.get("data").textValue();
        byte[] encodedTransactionClient = DataToolUtils
            .base64Decode(encodeResult.get("encodedTransaction").textValue().getBytes());
        RsvSignature clientSignedData = DataToolUtils.signToRsvSignature(encodedTransactionClient.toString(), DataToolUtils.hexStr2DecStr(ecKeyPair.getHexPrivateKey()));
        String base64SignedMsg = new String(
            DataToolUtils.base64Encode(TransactionEncoderUtilV2.goSignatureSerialization(clientSignedData)));

        // simulate transact call
        funcArgMap = new LinkedHashMap<>();
        txnArgMap = new LinkedHashMap<>();
        txnArgMap.put(WeIdentityParamKeyConstant.NONCE, nonce);
        txnArgMap.put(WeIdentityParamKeyConstant.TRANSACTION_DATA, data);
        txnArgMap.put(WeIdentityParamKeyConstant.SIGNED_MESSAGE, base64SignedMsg);
        txnArgMap.put(WeIdentityParamKeyConstant.SIGN_TYPE, SignType.VSR);
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

        // check WeID existence
        Assert.assertTrue(invokerWeIdService.isWeIdExist(weId).getResult());
    }

    //@Test
    public void testClientSign() {
        CryptoKeyPair ecKeyPair = CryptoFisco.cryptoSuite.createKeyPair("1113");
        String rawData = "+QGYiAoqvdSrq50MhRdIduf/hRdIduf/ggQglFl9kvCEDRPm7TPTzjYkiy3r5p5JgLkBZGvzCg0AAAAAAAAAAAAAAAASqNtWmYMdG8x4pGCkn3Z0a0trLAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAASAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAXkT+MQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB5MTA4MTkzNTI4NjAyODUxMzgxMzc3NjA4NDQ0MjE3NDI4ODEwMjAzOTIxODc4OTEyMjk2ODE1MDI0MDQ5MjA3NDUwMjYwNzA1NTA0OTM4LzB4MTJhOGRiNTY5OTgzMWQxYmNjNzhhNDYwYTQ5Zjc2NzQ2YjRiNmIyYwAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACjE1ODE1Nzk4MjUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAQGA";
        byte[] encodedTransactionClient = Base64.decode(rawData.getBytes());
        RsvSignature clientSignedData = DataToolUtils.signToRsvSignature(encodedTransactionClient.toString(), DataToolUtils.hexStr2DecStr(ecKeyPair.getHexPrivateKey()));
        String base64SignedMsg = new String(
            Base64.encode(TransactionEncoderUtilV2.goSignatureSerialization(clientSignedData)));
        System.out.println(base64SignedMsg);
    }

    //@Test
    public void TestAuthorityIssuerAll() throws Exception {
        if (TransactionEncoderUtil.isFiscoBcosV1()) {
            return;
        }
        //step 1: create a WeID as the issuer
        String issuerWeId = invokerWeIdService.createWeId().getResult().getWeId();

        // step 2: prepare param
        String nonceVal = TransactionEncoderUtilV2.getNonce().toString();
        Map<String, Object> funcArgMap = new LinkedHashMap<>();
        funcArgMap.put("weId", issuerWeId);
        funcArgMap.put("name", "ID" + DateUtils.getNoMillisecondTimeStampString());
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
        // note that the authorityIssuer creation can only be done by deployer
        String adminPrivKey = KeyUtil.getPrivateKeyByWeId(KeyUtil.SDK_PRIVKEY_PATH,
            PropertiesUtil.getProperty("default.passphrase"));
        CryptoKeyPair ecKeyPair = CryptoFisco.cryptoSuite.createKeyPair(adminPrivKey);
        JsonNode encodeResult = new ObjectMapper()
            .readTree(JsonUtil.objToJsonStr(resp1.getRespBody()));
        String data = encodeResult.get("data").textValue();
        byte[] encodedTransaction = DataToolUtils
            .base64Decode(encodeResult.get("encodedTransaction").textValue().getBytes());
        RsvSignature bodySigned = DataToolUtils.signToRsvSignature(encodedTransaction.toString(), DataToolUtils.hexStr2DecStr(ecKeyPair.getHexPrivateKey()));
        String base64SignedMsg = new String(
            DataToolUtils.base64Encode(TransactionEncoderUtilV2.goSignatureSerialization(bodySigned)));
        System.out.println("step 2 done, sig: " + base64SignedMsg);

        // step 4: send
        funcArgMap = new LinkedHashMap<>();
        txnArgMap = new LinkedHashMap<>();
        txnArgMap.put(WeIdentityParamKeyConstant.NONCE, nonceVal);
        txnArgMap.put(WeIdentityParamKeyConstant.TRANSACTION_DATA, data);
        txnArgMap.put(WeIdentityParamKeyConstant.SIGNED_MESSAGE, base64SignedMsg);
        txnArgMap.put(WeIdentityParamKeyConstant.SIGN_TYPE, SignType.VSR);
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


    //@Test
    public void TestCptAll() throws Exception {
        if (TransactionEncoderUtil.isFiscoBcosV1()) {
            return;
        }
        // step 0: create a WeID as the cpt creator (we let alone the authority issuer business)
        String issuerWeId = invokerWeIdService.createWeId().getResult().getWeId();

        // step 1: prepare param
        // note that the authorityIssuer creation can only be done by deployer
        String nonceVal = TransactionEncoderUtil.getNonce().toString();
        Map<String, Object> funcArgMap = new LinkedHashMap<>();
        funcArgMap.put("weId", issuerWeId);
        String cptSignature = //"AbCRWWOb4HoCZY5HPHeY9D87I4Y/HrcAXpIGTO7eS9d/359J9FCAaTtio5Pae6xy6TxPpsd/fHipTm8fXYV3EAA=";
            "HFH5xSCN4APHfL1SVWrlHEVRhZbmHvdXKeZtIoMlIOLrSJ1PpMoRTWAZbhcVYjbM0lhtpX9wQv3oF58Tte4YFPU=";
        funcArgMap.put("cptSignature", cptSignature);
        Map<String, Object> cptJsonSchemaMap = new LinkedHashMap<>();
        cptJsonSchemaMap.put("title", "a CPT schema");
        funcArgMap.put("cptJsonSchema", cptJsonSchemaMap);
        Map<String, Object> txnArgMap = new LinkedHashMap<>();
        txnArgMap.put(WeIdentityParamKeyConstant.NONCE, nonceVal);
        txnArgMap.put(WeIdentityParamKeyConstant.SIGN_TYPE, SignType.VSR);
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

        // step 2: sign via SDK privKey
        // let us use god account for low-bit cptId for good
        String adminPrivKey = KeyUtil.getPrivateKeyByWeId(KeyUtil.SDK_PRIVKEY_PATH,
            PropertiesUtil.getProperty("default.passphrase"));
        CryptoKeyPair ecKeyPair = CryptoFisco.cryptoSuite.createKeyPair(adminPrivKey);
        JsonNode encodeResult = new ObjectMapper()
            .readTree(JsonUtil.objToJsonStr(resp1.getRespBody()));
        String data = encodeResult.get("data").textValue();
        byte[] encodedTransaction = DataToolUtils
            .base64Decode(encodeResult.get("encodedTransaction").textValue().getBytes());
        RsvSignature bodySigned = DataToolUtils.signToRsvSignature(encodedTransaction.toString(), DataToolUtils.hexStr2DecStr(ecKeyPair.getHexPrivateKey()));
        String base64SignedMsg = new String(
            DataToolUtils.base64Encode(TransactionEncoderUtilV2.goSignatureSerialization(bodySigned)));
        System.out.println("step 2 done, sig: " + base64SignedMsg);

        // step 3: send
        funcArgMap = new LinkedHashMap<>();
        txnArgMap = new LinkedHashMap<>();
        txnArgMap.put(WeIdentityParamKeyConstant.NONCE, nonceVal);
        txnArgMap.put(WeIdentityParamKeyConstant.TRANSACTION_DATA, data);
        txnArgMap.put(WeIdentityParamKeyConstant.SIGNED_MESSAGE, base64SignedMsg);
        txnArgMap.put(WeIdentityParamKeyConstant.SIGN_TYPE, SignType.VSR);
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

        // step 3: queryCpt
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
    }

    //@Test
    public void testUpdateWeId() throws Exception {
        if (TransactionEncoderUtil.isFiscoBcosV1()) {
            return;
        }
        FiscoConfig fiscoConfig = new FiscoConfig();
        fiscoConfig.load();
        String to = fiscoConfig.getWeIdAddress();
        // all steps:
        CryptoKeyPair ecKeyPair = CryptoFisco.cryptoSuite.createKeyPair();
        String weId = WeIdUtils.convertPublicKeyToWeId(DataToolUtils.hexStr2DecStr(ecKeyPair.getHexPublicKey()));
        String addr = WeIdUtils.convertWeIdToAddress(weId);
        // 0. client generate nonce and send to server
        String nonce = TransactionEncoderUtilV2.getNonce().toString();
        // 1. server generate data
        // 下面为组装inputParameter
        byte[] byteValue = new byte[32];
        byte[] sourceByte = WeIdConstant.WEID_DOC_CREATED.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(sourceByte, 0, byteValue, 0, sourceByte.length);
        Bytes32 bytes32val =
            new Bytes32(byteValue);
        String updated = String.valueOf(System.currentTimeMillis());
        //Note that: the authentication and service must conform to the format specified by the weid document
        //only for test here, Please refer to the weid-java-sdk
        List<String> authentication = new ArrayList<>();
        authentication.add("ddddddd");
        List<String> service = new ArrayList<>();
        service.add("ggggg");
        Function function = new Function(
            WeIdentityFunctionNames.FUNCCALL_UPDATE_WEID,
                Arrays.<Type>asList(new Address(addr),
                        new Utf8String(updated),
                        new DynamicArray<Utf8String>(
                                Utils.typeMap(authentication, Utf8String.class)),
                        new DynamicArray<Utf8String>(
                                Utils.typeMap(service, org.fisco.bcos.sdk.abi.datatypes.Utf8String.class))),
                Collections.<TypeReference<?>>emptyList());
        FunctionEncoder functionEncoder = new FunctionEncoder(CryptoFisco.cryptoSuite);
        String data = functionEncoder.encode(function);
        // 2. server generate encodedTransaction
        Client web3j = (Client) com.webank.weid.blockchain.service.fisco.BaseServiceFisco.getClient();
        BigInteger blocklimit = TransactionEncoderUtilV2.getBlocklimitV2();
        RawTransaction rawTransaction = TransactionEncoderUtilV2.buildRawTransaction(nonce,
            fiscoConfig.getGroupId(), data, to, blocklimit);
        byte[] encodedTransaction = TransactionEncoderUtilV2.encode(rawTransaction);
        // 3. server sends encodeTransaction (in base64) and data back to client
        String encodedOutputToClient = TransactionEncoderUtil.getEncodeOutput(encodedTransaction, data, blocklimit);
        // 4. client signs and sends back to send raw txn
        JsonNode encodeResult = new ObjectMapper().readTree(encodedOutputToClient);
        byte[] encodedTransactionClient = DataToolUtils
            .base64Decode(encodeResult.get("encodedTransaction").textValue().getBytes());
        RsvSignature clientSignedData = DataToolUtils.signToRsvSignature(encodedTransactionClient.toString(), DataToolUtils.hexStr2DecStr(ecKeyPair.getHexPrivateKey()));
        String base64SignedMsg = new String(
            DataToolUtils.base64Encode(TransactionEncoderUtilV2.goSignatureSerialization(clientSignedData)));
        // 5. server receives the signed data
        RsvSignature clientSignedData2 = TransactionEncoderUtilV2
            .simpleSignatureDeserialization(DataToolUtils.base64Decode(base64SignedMsg.getBytes()), SignType.RSV);
        byte[] encodedSignedMsg = TransactionEncoderUtilV2.encode(rawTransaction, clientSignedData2);
        String txnHex = Numeric.toHexString(encodedSignedMsg);
        SendTransaction sendTransaction = web3j.sendRawTransaction(txnHex);
        /*SendTransaction sendTransaction = web3j.sendRawTransaction(txnHex).sendAsync()
            .get(WeIdConstant.TRANSACTION_RECEIPT_TIMEOUT, TimeUnit.SECONDS);*/
        Optional<TransactionReceipt> receiptOptional =
            TransactionEncoderUtilV2.getTransactionReceiptRequest(sendTransaction.getTransactionHash());
        if (receiptOptional.isPresent()) {
            TransactionReceipt receipt = receiptOptional.get();
            WeIdContract weIdContract = WeIdContract.load(to, web3j,
                    CryptoFisco.cryptoSuite.getCryptoKeyPair());
            List<WeIdContract.UpdateWeIdEventResponse> response =
                weIdContract.getUpdateWeIdEvents(receipt);
            Assert.assertNotNull(response);
            Assert.assertNotNull(response.get(0));
        }
    }

    //@Test
    public void testRemoveAuthIssuer() throws Exception {
        if (TransactionEncoderUtil.isFiscoBcosV1()) {
            return;
        }
        FiscoConfig fiscoConfig = new FiscoConfig();
        fiscoConfig.load();
        String to = fiscoConfig.getIssuerAddress();
        // all steps:
        CryptoKeyPair ecKeyPair = CryptoFisco.cryptoSuite.createKeyPair();
        String weId = WeIdUtils.convertPublicKeyToWeId(DataToolUtils.hexStr2DecStr(ecKeyPair.getHexPublicKey()));
        String addr = WeIdUtils.convertWeIdToAddress(weId);
        // 0. client generate nonce and send to server
        String nonce = TransactionEncoderUtilV2.getNonce().toString();
        // 1. server generate data
        // 下面为组装inputParameter
        Function function = new Function(
            "removeAuthorityIssuer",
            Arrays.<Type>asList(new Address(addr)),
            Collections.<TypeReference<?>>emptyList());
        FunctionEncoder functionEncoder = new FunctionEncoder(CryptoFisco.cryptoSuite);
        String data = functionEncoder.encode(function);
        // 2. server generate encodedTransaction
        Client web3j = (Client) com.webank.weid.blockchain.service.fisco.BaseServiceFisco.getClient();
        BigInteger blocklimit = TransactionEncoderUtilV2.getBlocklimitV2();
        RawTransaction rawTransaction = TransactionEncoderUtilV2.buildRawTransaction(nonce,
            fiscoConfig.getGroupId(), data, to, blocklimit);
        // 3. server sends everything back to client in encoded base64 manner
        // 这一步先忽略
        // 4. client signs and sends back to send raw txn
        TransactionEncoderService transactionEncoder = new TransactionEncoderService(CryptoFisco.cryptoSuite);
        String txnHex = transactionEncoder.encodeAndSign(rawTransaction, CryptoFisco.cryptoSuite.getCryptoKeyPair());
        //String txnHex = Numeric.toHexString(signedMessage);
        /*SendTransaction sendTransaction = web3j.sendRawTransaction(txnHex).sendAsync()
            .get(WeIdConstant.TRANSACTION_RECEIPT_TIMEOUT, TimeUnit.SECONDS);*/
        SendTransaction sendTransaction = web3j.sendRawTransaction(txnHex);
        Optional<TransactionReceipt> receiptOptional =
            TransactionEncoderUtilV2.getTransactionReceiptRequest(sendTransaction.getTransactionHash());
        TransactionReceipt receipt = receiptOptional.get();
        AuthorityIssuerController authorityIssuerController = AuthorityIssuerController.load(to, web3j,
                CryptoFisco.cryptoSuite.getCryptoKeyPair());
        List<AuthorityIssuerRetLogEventResponse> response =
            authorityIssuerController.getAuthorityIssuerRetLogEvents(receipt);
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.get(0));
    }
}
