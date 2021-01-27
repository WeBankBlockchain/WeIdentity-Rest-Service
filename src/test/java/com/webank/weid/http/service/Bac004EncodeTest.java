package com.webank.weid.http.service;

import java.util.HashMap;
import java.util.Map;

import org.fisco.bcos.web3j.crypto.ECKeyPair;
import org.fisco.bcos.web3j.crypto.Keys;
import org.junit.Assert;
import org.junit.Test;

import com.webank.weid.exception.WeIdBaseException;
import com.webank.weid.http.BaseTest;
import com.webank.weid.http.protocol.response.HttpResponseData;
import com.webank.weid.protocol.base.WeIdPrivateKey;
import com.webank.weid.protocol.request.CreateWeIdArgs;
import com.webank.weid.rpc.WeIdService;
import com.webank.weid.service.impl.WeIdServiceImpl;
import com.webank.weid.util.WeIdUtils;

public class Bac004EncodeTest extends BaseTest {

    private static ECKeyPair createEcKeyPair;
    private static WeIdService weidService = new WeIdServiceImpl();
    private static String bac004Address;
    
    static {
        try {
            createEcKeyPair = Keys.createEcKeyPair();
            CreateWeIdArgs createWeIdArgs = new CreateWeIdArgs();
            createWeIdArgs.setPublicKey(createEcKeyPair.getPublicKey().toString());
            createWeIdArgs.setWeIdPrivateKey(new WeIdPrivateKey(createEcKeyPair.getPrivateKey().toString()));
            weidService.createWeId(createWeIdArgs);
            testConstruct();
        } catch (Exception e) {
            throw new WeIdBaseException("fail.");
        }
    }
    
    public static void testConstruct() throws Exception {
        String functionName = "construct";
        Map<String, Object> funcMap = new HashMap<String, Object>();
        funcMap.put("shortName", "pan");
        funcMap.put("description", "this is a pan");
        Map<String, Object> transMap = new HashMap<String, Object>();
        HttpResponseData<?> response = exectue(functionName, funcMap, transMap, createEcKeyPair);
        bac004Address = response.getRespBody().toString();
    }
    
    @Test
    public void testIssue() throws Exception {
        String functionName = "issue";
        String recipient = WeIdUtils.convertPublicKeyToWeId(createEcKeyPair.getPublicKey().toString());
        Map<String, Object> funcMap = new HashMap<String, Object>();
        funcMap.put("assetAddress", bac004Address);
        funcMap.put("recipient", recipient);
        funcMap.put("amount", 1000);
        funcMap.put("remark", "this is a pan");
        Map<String, Object> transMap = new HashMap<String, Object>();
        transMap.put("toAddress", bac004Address);
        HttpResponseData<?> response = exectue(functionName, funcMap, transMap, createEcKeyPair);
        bac004Address = response.getRespBody().toString();
    }

    private static HttpResponseData<?> exectue(
        String functionName, 
        Map<String, Object> funcMap, 
        Map<String, Object> transMap,
        ECKeyPair createEcKeyPair
    ) throws Exception {
        String nonce = String.valueOf(System.currentTimeMillis());
        Map<String, Object> buildEncode = buildEncode(functionName, nonce);
        buildEncode.put("functionArg", funcMap);
        Map<String, Object> transactionArg_ = (HashMap<String, Object>)buildEncode.get("transactionArg");
        transactionArg_.putAll(transMap);
        Map<String, Object> respBodyMap = encode("payment/bac004", buildEncode);
        String base64SignedMsg = sign(createEcKeyPair, respBodyMap);
        System.out.println(functionName + " - sign: " + base64SignedMsg);
        Map<String, Object> buildSend = buildSend(
            functionName, base64SignedMsg, respBodyMap.get("data").toString(), nonce, respBodyMap.get("blockLimit").toString());
        Map<String, Object> transactionArg = (HashMap<String, Object>)buildSend.get("transactionArg");
        transactionArg.putAll(transMap);
        HttpResponseData<?> response = send("payment/bac004",buildSend);
        Assert.assertEquals(0, response.getErrorCode().intValue());
        return response;
    }
}
