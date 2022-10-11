package com.webank.weid.http.service;

import java.util.HashMap;
import java.util.Map;

import com.webank.weid.util.DataToolUtils;
import org.fisco.bcos.sdk.crypto.keypair.CryptoKeyPair;
import org.junit.Assert;
import org.junit.Test;

import com.webank.weid.http.BaseTest;

public class EnCodeAndTranscatTest extends BaseTest {
  
    @Test
    public void testCreateWeId() throws Exception {
        CryptoKeyPair createEcKeyPair = DataToolUtils.cryptoSuite.createKeyPair();
        String nonce = String.valueOf(System.currentTimeMillis());
        String functionName = "createWeId";
        Map<String, Object> buildEncode = buildEncode(functionName, nonce);
        // build function arg
        Map<String, Object> funcMap = new HashMap<String, Object>();
        funcMap.put("publicKey", createEcKeyPair.getHexPublicKey());
        buildEncode.put("functionArg", funcMap);
        Map<String, Object> respBodyMap = encode("weid", buildEncode);
        String base64SignedMsg = sign(DataToolUtils.hexStr2DecStr(createEcKeyPair.getHexPrivateKey()), respBodyMap);
        System.out.println(functionName + " - sign: " + base64SignedMsg);
        Map<String, Object> buildSend = buildSend(
            functionName, 
            base64SignedMsg, 
            respBodyMap.get("data").toString(), 
            nonce, 
            respBodyMap.get("blockLimit").toString()
        );
        Integer code = send("weid", buildSend).getErrorCode();
        Assert.assertEquals(0, code.intValue());
    }
}
