/*
 *       Copyright© (2018) WeBank Co., Ltd.
 *
 *       This file is part of weidentity-java-sdk.
 *
 *       weidentity-java-sdk is free software: you can redistribute it and/or modify
 *       it under the terms of the GNU Lesser General Public License as published by
 *       the Free Software Foundation, either version 3 of the License, or
 *       (at your option) any later version.
 *
 *       weidentity-java-sdk is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU Lesser General Public License for more details.
 *
 *       You should have received a copy of the GNU Lesser General Public License
 *       along with weidentity-java-sdk.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.webank.weid.http.service;

import java.io.InputStream;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyStore;
import java.security.interfaces.ECPrivateKey;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import org.bcos.web3j.crypto.Credentials;
import org.bcos.web3j.crypto.ECKeyPair;
import org.bcos.web3j.crypto.Sign;
import org.bcos.web3j.crypto.Sign.SignatureData;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.webank.weid.constant.ErrorCode;
import com.webank.weid.http.BaseTest;
import com.webank.weid.http.protocol.request.ReqRegisterSignCptMapArgs;
import com.webank.weid.protocol.base.CptBaseInfo;
import com.webank.weid.protocol.response.ResponseData;
import com.webank.weid.util.JsonUtil;
import com.webank.weid.util.SignatureUtils;

/**
 * @author darwindu
 **/
@Component
public abstract class InvokerCptServiceTest1 extends BaseTest {

    private InvokerCptService invokerCptService = new InvokerCptService();

    //@Test
    public void registerTranCpt() throws Exception {

        Credentials credentials = Credentials.create(ECKeyPair.create(new BigInteger("1111")));

        JsonNode jsonNode = JsonLoader.fromResource("/cpt.json");
        String jsonSchema = jsonNode.toString();
        Map<String, Object> jsonSchemaMap = (Map<String, Object>) JsonUtil.jsonStrToObj(new HashMap<String, Object>(), jsonSchema);

        String cptPublisher = jsonSchemaMap.get("weId").toString();
        StringBuilder sb = new StringBuilder();
        sb.append(cptPublisher);
        sb.append("|");
        sb.append(jsonSchema);
        SignatureData signatureData = SignatureUtils.signMessage(sb.toString(), credentials.getEcKeyPair().getPrivateKey().toString());

        System.out.println("==signatureData:" + JsonUtil.objToJsonStr(signatureData));

        ReqRegisterSignCptMapArgs reqRegisterSignCptMapArgs = new ReqRegisterSignCptMapArgs();
        reqRegisterSignCptMapArgs.setSignatureData(signatureData);
        reqRegisterSignCptMapArgs.setDataJson(jsonSchemaMap);
        ResponseData<byte[]> responseData =  invokerCptService.getEncodedTransaction(reqRegisterSignCptMapArgs);
        String baseenc = new String(SignatureUtils.base64Encode(responseData.getResult()));
        System.out.println("==responseData:" + JsonUtil.objToJsonStr(responseData) + "; base 64: " +  baseenc);

        byte[] encodedTransaction = responseData.getResult();

        SignatureData bodySigned = Sign.signMessage(encodedTransaction, credentials.getEcKeyPair());
        reqRegisterSignCptMapArgs = new ReqRegisterSignCptMapArgs();
        reqRegisterSignCptMapArgs.setSignatureData(signatureData);
        reqRegisterSignCptMapArgs.setDataJson(jsonSchemaMap);
        reqRegisterSignCptMapArgs.setBodySigned(bodySigned);
        ResponseData<CptBaseInfo> responseData1 = invokerCptService.registerSignCpt(reqRegisterSignCptMapArgs);

        System.out.println("==responseData1:" + JsonUtil.objToJsonStr(responseData1));

        Assert.assertEquals(ErrorCode.SUCCESS.getCode(), responseData1.getErrorCode().intValue());
        Assert.assertNotNull(responseData1.getResult());
        Assert.assertNotNull("hellworld");
    }
}
