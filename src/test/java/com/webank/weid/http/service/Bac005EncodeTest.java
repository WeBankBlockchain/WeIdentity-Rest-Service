package com.webank.weid.http.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

public class Bac005EncodeTest extends BaseTest {

    private static ECKeyPair createEcKeyPair;
    private static WeIdService weidService = new WeIdServiceImpl();
    private static String bac005Address;
    
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
        bac005Address = response.getRespBody().toString();
    }
    
    @Test
    public void testIssue() throws Exception {
        String recipient = WeIdUtils.convertPublicKeyToWeId(createEcKeyPair.getPublicKey().toString());
        String functionName = "issue";
        Map<String, Object> funcMap = new HashMap<String, Object>();
        funcMap.put("assetAddress", bac005Address);
        funcMap.put("recipient", recipient);
        funcMap.put("data", "this is issue asset");
        funcMap.put("assetId", 10001);
        funcMap.put("assetUri", "/BAC005/issueAsset/10001");
        Map<String, Object> transMap = new HashMap<String, Object>();
        transMap.put("toAddress", bac005Address);
        exectue(functionName, funcMap, transMap, createEcKeyPair);
    }
    
    @Test
    public void testBatchIssue() throws Exception {
        String recipient = WeIdUtils.convertPublicKeyToWeId(createEcKeyPair.getPublicKey().toString());
        String functionName = "batchIssue";
        Map<String, Object> funcMap = new HashMap<String, Object>();
        funcMap.put("assetAddress", bac005Address);
        List<Map<String, Object>> objectList = new ArrayList<>();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("recipient", recipient);
        map.put("data", "this is issue asset");
        map.put("assetId", 10002);
        map.put("assetUri", "/BAC005/issueAsset/10002");
        objectList.add(map);
        
        Map<String, Object> map1 = new HashMap<String, Object>();
        map1.put("recipient", recipient);
        map1.put("data", "this is issue asset");
        map1.put("assetId", 10003);
        map1.put("assetUri", "/BAC005/issueAsset/10003");
        objectList.add(map1);
        
        funcMap.put("objectList", objectList);
        Map<String, Object> transMap = new HashMap<String, Object>();
        transMap.put("toAddress", bac005Address);
        exectue(functionName, funcMap, transMap, createEcKeyPair);
    }
    
    @Test
    public void testSend() throws Exception {
        String invokerWeId = WeIdUtils.convertPublicKeyToWeId(createEcKeyPair.getPublicKey().toString());
        String recipient = weidService.createWeId().getResult().getWeId();
        String functionName = "send";
        Map<String, Object> funcMap = new HashMap<String, Object>();
        funcMap.put("assetAddress", bac005Address);
        funcMap.put("recipient", recipient);
        funcMap.put("data", "this is issue asset");
        funcMap.put("assetId", 10001);
        Map<String, Object> transMap = new HashMap<String, Object>();
        transMap.put("toAddress", bac005Address);
        transMap.put("invokerWeId", invokerWeId);
        exectue(functionName, funcMap, transMap, createEcKeyPair);
    }
    
    @Test
    public void testBatchSend() throws Exception {
        String invokerWeId = WeIdUtils.convertPublicKeyToWeId(createEcKeyPair.getPublicKey().toString());
        String recipient = weidService.createWeId().getResult().getWeId();
        String functionName = "batchSend";
        Map<String, Object> funcMap = new HashMap<String, Object>();
        funcMap.put("assetAddress", bac005Address);
        List<Map<String, Object>> objectList = new ArrayList<>();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("recipient", recipient);
        map.put("data", "this is issue asset");
        map.put("assetId", 10002);
        objectList.add(map);
        funcMap.put("objectList", objectList);
        Map<String, Object> transMap = new HashMap<String, Object>();
        transMap.put("toAddress", bac005Address);
        transMap.put("invokerWeId", invokerWeId);
        exectue(functionName, funcMap, transMap, createEcKeyPair);
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
        Map<String, Object> respBodyMap = encode("payment/bac005", buildEncode);
        String base64SignedMsg = sign(createEcKeyPair, respBodyMap);
        System.out.println(functionName + " - sign: " + base64SignedMsg);
        Map<String, Object> buildSend = buildSend(
            functionName, base64SignedMsg, respBodyMap.get("data").toString(), nonce);
        Map<String, Object> transactionArg = (HashMap<String, Object>)buildSend.get("transactionArg");
        transactionArg.putAll(transMap);
        HttpResponseData<?> response = send("payment/bac005",buildSend);
        Assert.assertEquals(0, response.getErrorCode().intValue());
        return response;
    }
}
