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

import com.webank.weid.constant.CredentialConstant.CredentialProofType;
import com.webank.weid.constant.ParamKeyConstant;
import com.webank.weid.http.BaseTest;
import com.webank.weid.http.constant.WeIdentityFunctionNames;
import com.webank.weid.http.constant.WeIdentityParamKeyConstant;
import com.webank.weid.http.protocol.response.HttpResponseData;
import com.webank.weid.http.service.impl.InvokerWeIdServiceImpl;
import com.webank.weid.http.service.impl.TransactionServiceImpl;
import com.webank.weid.http.util.JsonUtil;
import com.webank.weid.http.util.TransactionEncoderUtilV2;
import com.webank.weid.protocol.base.Credential;
import com.webank.weid.protocol.base.CredentialPojo;
import com.webank.weid.protocol.response.CreateWeIdDataResult;
import com.webank.weid.protocol.response.ResponseData;
import com.webank.weid.rpc.CredentialPojoService;
import com.webank.weid.service.impl.CredentialPojoServiceImpl;
import com.webank.weid.util.DataToolUtils;
import com.webank.weid.util.DateUtils;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bcos.web3j.crypto.ECKeyPair;
import org.bcos.web3j.crypto.Sign;
import org.junit.Assert;
import org.junit.Test;

public class CredentialTest extends BaseTest {

    TransactionService transactionService = new TransactionServiceImpl();
    InvokerWeIdService invokerWeIdService = new InvokerWeIdServiceImpl();
    CredentialPojoService credentialPojoService = new CredentialPojoServiceImpl();

    @Test
    public void TestClientSideCredentialAll() throws Exception {
        // test create
        Map<String, Object> funcArgMap = new LinkedHashMap<>();
        funcArgMap.put("cptId", "10");
        funcArgMap.put("issuer", "did:weid:0x865f29d2407e91a8be0d5811c6156b6f1c845f41");
        funcArgMap.put("expirationDate", "2020-11-18T21:12:33Z");
        Map<String, Object> claimMap = new LinkedHashMap<>();
        claimMap.put("acc", "10001");
        claimMap.put("name", "ppp");
        funcArgMap.put("claim", claimMap);
        Map<String, Object> txnArgMap = new LinkedHashMap<>();
        Map<String, Object> inputParamMap = new LinkedHashMap<>();
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_ARG, funcArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.TRANSACTION_ARG, txnArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.API_VERSION,
            WeIdentityParamKeyConstant.DEFAULT_API_VERSION);
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_NAME,
            WeIdentityFunctionNames.FUNCNAME_CREATE_CREDENTIAL);
        HttpResponseData<Object> resp1 =
            transactionService.invokeFunction(JsonUtil.objToJsonStr(inputParamMap));
        System.out.println("intermediate result: " + JsonUtil.objToJsonStr(resp1));

        // simulate client side sign
        // this result contains both claim and claimhash
        Map<String, Object> credMap = (Map<String, Object>) JsonUtil
            .jsonStrToObj(new HashMap<String, Object>(),
                JsonUtil.mapToCompactJson((Map<String, Object>) resp1.getRespBody()));
        System.out.println(credMap);
        String claimHash = credMap.get("claimHash").toString();
        credMap.remove("claimHash");
        Map<String, Object> credForSigMap = new HashMap<>(credMap);
        credForSigMap.put("claim", claimHash);
        credForSigMap.remove("signature");
        //do sign
        //String sign = ClientUtil.signCredential(credMap, privateKey);
        String sign = "FeB5";
        Map<String, Object> proofMap = new HashMap<>();
        proofMap.put(ParamKeyConstant.PROOF_CREATED, DateUtils.convertUtcDateToTimeStamp(
            DateUtils.convertTimestampToUtc(System.currentTimeMillis())));
        proofMap.put(ParamKeyConstant.PROOF_CREATOR,
            "did:weid:0x865f29d2407e91a8be0d5811c6156b6f1c845f41");
        proofMap.put(ParamKeyConstant.PROOF_TYPE, CredentialProofType.ECDSA.getTypeName());
        proofMap.put(ParamKeyConstant.CREDENTIAL_SIGNATURE, sign);
        credMap.put(ParamKeyConstant.PROOF, proofMap);
        String credentialAfterSign = JsonUtil.mapToCompactJson(credMap);
        Credential credential = (Credential) JsonUtil
            .jsonStrToObj(new Credential(), credentialAfterSign);
        System.out.println("after sign: " + JsonUtil.objToJsonStr(credential));

        //test verify
        Map<String, Object> credJsonMap = JsonUtil.objToMap(credential);
        inputParamMap = new LinkedHashMap<>();
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_ARG, credJsonMap);
        inputParamMap.put(WeIdentityParamKeyConstant.TRANSACTION_ARG, txnArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.API_VERSION,
            WeIdentityParamKeyConstant.DEFAULT_API_VERSION);
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_NAME,
            WeIdentityFunctionNames.FUNCNAME_VERIFY_CREDENTIAL);
        HttpResponseData<Object> resp3 =
            transactionService.invokeFunction(JsonUtil.objToJsonStr(inputParamMap));
        System.out.println(resp3);
    }

    @Test
    public void TestEncodeCredentialPojoAll() throws Exception {
        CreateWeIdDataResult createWeIdDataResult = invokerWeIdService.createWeId().getResult();
        String weId = createWeIdDataResult.getWeId();

        // test createargs
        Map<String, Object> funcArgMap = new LinkedHashMap<>();
        funcArgMap.put("cptId", "2000000");
        funcArgMap.put("issuer", weId);
        funcArgMap.put("expirationDate", "2040-04-18T21:12:33Z");
        Map<String, Object> claimMap = new LinkedHashMap<>();
        claimMap.put("acc", "10001");
        claimMap.put("name", "ppp");
        funcArgMap.put("claim", claimMap);
        Map<String, Object> txnArgMap = new LinkedHashMap<>();
        Map<String, Object> inputParamMap = new LinkedHashMap<>();
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_ARG, funcArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.TRANSACTION_ARG, txnArgMap);
        inputParamMap.put(WeIdentityParamKeyConstant.API_VERSION,
            WeIdentityParamKeyConstant.DEFAULT_API_VERSION);
        inputParamMap.put(WeIdentityParamKeyConstant.FUNCTION_NAME,
            WeIdentityFunctionNames.FUNCNAME_CREATE_CREDENTIALPOJO);
        HttpResponseData<Object> resp1 =
            transactionService.encodeTransaction(JsonUtil.objToJsonStr(inputParamMap));
        System.out.println("intermediate result: " + resp1.getRespBody());

        // simulate client-sign
        Map<String, Object> credMap = (HashMap<String, Object>) resp1.getRespBody();
        Map<String, Object> proofMap = (HashMap<String, Object>) credMap.get("proof");
        String base64EncRawData = (String) proofMap.get("signatureValue");
        System.out.println(base64EncRawData);
        String rawData = new String(DataToolUtils.base64Decode(base64EncRawData.getBytes()));
        System.out.println(rawData);
        String signature = DataToolUtils.sign(rawData,
            createWeIdDataResult.getUserWeIdPrivateKey().getPrivateKey());
        ECKeyPair ecKeyPair = ECKeyPair.create(new BigInteger(createWeIdDataResult.getUserWeIdPrivateKey().getPrivateKey()));
        String sig2 = new String(DataToolUtils.base64Encode(DataToolUtils.simpleSignatureSerialization(
            Sign.signMessage(DataToolUtils.sha3(rawData.getBytes()), ecKeyPair))));
        Assert.assertTrue(sig2.equals(signature));
        // Verify Credential
        proofMap.put("signatureValue", signature);
        credMap.put("proof", proofMap);
        CredentialPojo credentialPojo = DataToolUtils
            .deserialize(DataToolUtils.mapToCompactJson(credMap), CredentialPojo.class);
        ResponseData<Boolean> verifyResp = credentialPojoService.verify(credentialPojo.getIssuer(),
            credentialPojo);
        System.out.println(verifyResp.getErrorCode());
    }
}
