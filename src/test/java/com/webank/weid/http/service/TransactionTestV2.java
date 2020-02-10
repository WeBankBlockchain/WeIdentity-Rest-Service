package com.webank.weid.http.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webank.weid.config.FiscoConfig;
import com.webank.weid.constant.WeIdConstant;
import com.webank.weid.contract.v2.AuthorityIssuerController;
import com.webank.weid.contract.v2.AuthorityIssuerController.AuthorityIssuerRetLogEventResponse;
import com.webank.weid.contract.v2.WeIdContract;
import com.webank.weid.contract.v2.WeIdContract.WeIdAttributeChangedEventResponse;
import com.webank.weid.http.constant.WeIdentityFunctionNames;
import com.webank.weid.http.constant.WeIdentityParamKeyConstant;
import com.webank.weid.http.protocol.response.HttpResponseData;
import com.webank.weid.http.service.impl.InvokerWeIdServiceImpl;
import com.webank.weid.http.service.impl.TransactionServiceImpl;
import com.webank.weid.http.util.JsonUtil;
import com.webank.weid.http.util.TransactionEncoderUtil;
import com.webank.weid.http.util.TransactionEncoderUtilV2;
import com.webank.weid.service.BaseService;
import com.webank.weid.util.DataToolUtils;
import com.webank.weid.util.WeIdUtils;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.bcos.web3j.utils.Numeric;
import org.fisco.bcos.web3j.abi.TypeReference;
import org.fisco.bcos.web3j.abi.datatypes.Type;
import org.fisco.bcos.web3j.crypto.ExtendedRawTransaction;
import org.fisco.bcos.web3j.crypto.ExtendedTransactionEncoder;
import org.fisco.bcos.web3j.crypto.Sign;
import org.fisco.bcos.web3j.crypto.Sign.SignatureData;
import org.fisco.bcos.web3j.protocol.Web3j;
import org.fisco.bcos.web3j.protocol.core.methods.response.SendTransaction;
import org.fisco.bcos.web3j.protocol.core.methods.response.TransactionReceipt;
import org.fisco.bcos.web3j.tx.gas.StaticGasProvider;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TransactionTestV2 {

    TransactionService transactionService = new TransactionServiceImpl();
    InvokerWeIdService invokerWeIdService = new InvokerWeIdServiceImpl();

    @Test
    public void testCreateWeIdAll() throws Exception {
        FiscoConfig fiscoConfig = new FiscoConfig();
        fiscoConfig.load();

        org.fisco.bcos.web3j.crypto.ECKeyPair ecKeyPair = org.fisco.bcos.web3j.crypto.Keys.createEcKeyPair();
        String newPublicKey = ecKeyPair.getPublicKey().toString();
        String weId = WeIdUtils.convertPublicKeyToWeId(newPublicKey);
        String nonce = TransactionEncoderUtilV2.getNonce().toString();

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

        // simulate client side sign
        JsonNode encodeResult = new ObjectMapper()
            .readTree(JsonUtil.objToJsonStr(resp1.getRespBody()));
        String data = encodeResult.get("data").textValue();
        byte[] encodedTransactionClient = DataToolUtils
            .base64Decode(encodeResult.get("encodedTransaction").textValue().getBytes());
        SignatureData clientSignedData = Sign.getSignInterface().signMessage(encodedTransactionClient, ecKeyPair);
        String base64SignedMsg = new String(
            DataToolUtils.base64Encode(TransactionEncoderUtilV2.simpleSignatureSerialization(clientSignedData)));

        funcArgMap = new LinkedHashMap<>();
        txnArgMap = new LinkedHashMap<>();
        txnArgMap.put(WeIdentityParamKeyConstant.NONCE, nonce);
        txnArgMap.put(WeIdentityParamKeyConstant.TRANSACTION_DATA, data);
        txnArgMap.put(WeIdentityParamKeyConstant.SIGNED_MESSAGE, base64SignedMsg);
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
    }

    @Test
    public void testSetAttribute() throws Exception //{ExtendedRawTransaction rawTransaction, Credentials credentials,
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
        String nonce = TransactionEncoderUtilV2.getNonce().toString();
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
        ExtendedRawTransaction rawTransaction = TransactionEncoderUtilV2.buildRawTransaction(nonce,
            fiscoConfig.getGroupId(), data, to);
        byte[] encodedTransaction = TransactionEncoderUtilV2.encode(rawTransaction);
        // 3. server sends encodeTransaction (in base64) and data back to client
        String encodedOutputToClient = TransactionEncoderUtil.getEncodeOutput(encodedTransaction, data);
        // 4. client signs and sends back to send raw txn
        JsonNode encodeResult = new ObjectMapper().readTree(encodedOutputToClient);
        byte[] encodedTransactionClient = DataToolUtils
            .base64Decode(encodeResult.get("encodedTransaction").textValue().getBytes());
        org.fisco.bcos.web3j.crypto.Sign.SignatureData clientSignedData = org.fisco.bcos.web3j.crypto.Sign
            .getSignInterface().signMessage(encodedTransactionClient, ecKeyPair);
        String base64SignedMsg = new String(
            DataToolUtils.base64Encode(TransactionEncoderUtilV2.simpleSignatureSerialization(clientSignedData)));
        // 5. server receives the signed data
        org.fisco.bcos.web3j.crypto.Sign.SignatureData clientSignedData2 = TransactionEncoderUtilV2
            .simpleSignatureDeserialization(DataToolUtils.base64Decode(base64SignedMsg.getBytes()));
        byte[] encodedSignedMsg = TransactionEncoderUtilV2.encode(rawTransaction, clientSignedData2);
        String txnHex = Numeric.toHexString(encodedSignedMsg);
        SendTransaction sendTransaction = web3j.sendRawTransaction(txnHex).sendAsync()
            .get(WeIdConstant.TRANSACTION_RECEIPT_TIMEOUT, TimeUnit.SECONDS);
        Optional<TransactionReceipt> receiptOptional =
            TransactionEncoderUtilV2.getTransactionReceiptRequest(sendTransaction.getTransactionHash());
        if (receiptOptional.isPresent()) {
            TransactionReceipt receipt = receiptOptional.get();
            WeIdContract weIdContract = WeIdContract.load(to, web3j,
                org.fisco.bcos.web3j.crypto.gm.GenCredential.create(),
                new StaticGasProvider(WeIdConstant.GAS_PRICE, WeIdConstant.GAS_LIMIT));
            List<WeIdAttributeChangedEventResponse> response =
                weIdContract.getWeIdAttributeChangedEvents(receipt);
            System.out.println();
        }
    }


    @Test
    public void testRemoveAuthIssuer() throws Exception {
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
        String nonce = TransactionEncoderUtilV2.getNonce().toString();
        // 1. server generate data
        // 下面为组装inputParameter
        org.fisco.bcos.web3j.abi.datatypes.Function function = new org.fisco.bcos.web3j.abi.datatypes.Function(
            "removeAuthorityIssuer",
            Arrays.<Type>asList(new org.fisco.bcos.web3j.abi.datatypes.Address(addr)),
            Collections.<TypeReference<?>>emptyList());
        String data = org.fisco.bcos.web3j.abi.FunctionEncoder.encode(function);
        // 2. server generate encodedTransaction
        Web3j web3j = (Web3j) BaseService.getWeb3j();
        ExtendedRawTransaction rawTransaction = TransactionEncoderUtilV2.buildRawTransaction(nonce,
            fiscoConfig.getGroupId(), data, to);
        // 3. server sends everything back to client in encoded base64 manner
        // 这一步先忽略
        // 4. client signs and sends back to send raw txn
        byte[] signedMessage = ExtendedTransactionEncoder.signMessage(rawTransaction, credentials);
        String txnHex = Numeric.toHexString(signedMessage);
        SendTransaction sendTransaction = web3j.sendRawTransaction(txnHex).sendAsync()
            .get(WeIdConstant.TRANSACTION_RECEIPT_TIMEOUT, TimeUnit.SECONDS);
        Optional<TransactionReceipt> receiptOptional =
            TransactionEncoderUtilV2.getTransactionReceiptRequest(sendTransaction.getTransactionHash());
        TransactionReceipt receipt = receiptOptional.get();
        AuthorityIssuerController authorityIssuerController = AuthorityIssuerController.load(to, web3j,
            org.fisco.bcos.web3j.crypto.gm.GenCredential.create(),
            new StaticGasProvider(WeIdConstant.GAS_PRICE, WeIdConstant.GAS_LIMIT));
        List<AuthorityIssuerRetLogEventResponse> response =
            authorityIssuerController.getAuthorityIssuerRetLogEvents(receipt);
        System.out.println(response);
    }
}
