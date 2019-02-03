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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.google.common.base.Splitter;
import org.bcos.web3j.abi.FunctionEncoder;
import org.bcos.web3j.abi.datatypes.Address;
import org.bcos.web3j.abi.datatypes.Function;
import org.bcos.web3j.abi.datatypes.StaticArray;
import org.bcos.web3j.abi.datatypes.Type;
import org.bcos.web3j.abi.datatypes.generated.Bytes32;
import org.bcos.web3j.abi.datatypes.generated.Int256;
import org.bcos.web3j.abi.datatypes.generated.Uint8;
import org.bcos.web3j.crypto.Credentials;
import org.bcos.web3j.crypto.ECKeyPair;
import org.bcos.web3j.crypto.Sign.SignatureData;
import org.bcos.web3j.crypto.TransactionEncoder;
import org.bcos.web3j.crypto.sm2.util.encoders.Hex;
import org.bcos.web3j.protocol.core.methods.request.RawTransaction;
import org.bcos.web3j.tx.TransactionConstant;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.webank.weid.constant.ErrorCode;
import com.webank.weid.constant.JsonSchemaConstant;
import com.webank.weid.constant.WeIdConstant;
import com.webank.weid.http.BaseTest;
import com.webank.weid.http.protocol.request.ReqRegisterTranCptMapArgs;
import com.webank.weid.protocol.base.CptBaseInfo;
import com.webank.weid.protocol.response.ResponseData;
import com.webank.weid.protocol.response.RsvSignature;
import com.webank.weid.service.BaseService;
import com.webank.weid.util.DataTypetUtils;
import com.webank.weid.util.JsonUtil;
import com.webank.weid.util.SignatureUtils;
import com.webank.weid.util.WeIdUtils;

/**
 * @author darwindu
 **/
@Component
public class InvokerCptServiceTest extends BaseTest {

    @Autowired
    private InvokerWeb3jService invokerWeb3jService;

    @Autowired
    private InvokerCptService invokerCptService;

    @Test
    public void registerTranCpt() throws Exception {

        KeyStore ks = KeyStore.getInstance("JKS");
        InputStream ksInputStream =
            InvokerCptServiceTest.class.getClassLoader().getResourceAsStream("tb.jks");
        ks.load(ksInputStream, "123456".toCharArray());
        Key key = ks.getKey("ec", "123456".toCharArray());
        ECPrivateKey privateKey = (ECPrivateKey) key;
        Credentials credentials = Credentials.create(ECKeyPair.create(privateKey.getS()));

        BigInteger nonce = invokerWeb3jService.getNonce().getResult();
        System.out.println("==nonce:" + nonce);

        JsonNode jsonNode = JsonLoader.fromResource("/cpt.json");
        String jsonSchema = jsonNode.toString();
        Map<String, Object> jsonSchemaMap =
            (Map<String, Object>) JsonUtil.jsonStrToObj(new HashMap<String, Object>(), jsonSchema);
        String data =
            createFuntionRegisterCpt(jsonSchemaMap, credentials.getEcKeyPair().getPrivateKey());

        BigInteger blockLimit = BaseService.getBlockLimit().getResult();
        RawTransaction rawTransaction  = RawTransaction.createTransaction(
            nonce,
            new BigInteger("99999999999"),
            new BigInteger("99999999999"),
            blockLimit,
            //"0xd7a617780dd61be1c599f2462667a26cbb9fd6bf",
            "0x3774efe1552806b12d9a0f3f46e31986a2795955",
            new BigInteger("0"),
            data,
            TransactionConstant.callType,
            false);

        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
        System.out.println("====signedMessage:" + signedMessage);
        String hexValue = Hex.toHexString(signedMessage);
        System.out.println("====hexValue:" + hexValue);

        ReqRegisterTranCptMapArgs reqRegisterTranCptMapArgs = new ReqRegisterTranCptMapArgs();
        reqRegisterTranCptMapArgs.setBodySigned(hexValue);
        reqRegisterTranCptMapArgs.setDataJson(jsonSchema);
        System.out.println("====request:" + JsonUtil.objToJsonStr(reqRegisterTranCptMapArgs));
        ResponseData<CptBaseInfo> responseData = invokerCptService.registerTranCpt(reqRegisterTranCptMapArgs);

        System.out.println("====response:" + JsonUtil.objToJsonStr(responseData));

        Assert.assertEquals(ErrorCode.SUCCESS.getCode(), responseData.getErrorCode().intValue());
        Assert.assertNotNull(responseData.getResult());

        Assert.assertNotNull("hellworld");
    }

    private String createFuntionRegisterCpt(
        Map<String, Object> map,
        BigInteger privateKey) throws Exception {

        String weId = map.get("weId").toString();
        Map<String, Object> cptJsonSchema = (Map<String, Object>) map.get("cptJsonSchema");
        String cptJsonSchemaNew = this.cptSchemaToString(cptJsonSchema);
        RsvSignature rsvSignature = sign(
            weId,
            cptJsonSchemaNew,
            privateKey.toString());

        StaticArray<Bytes32> bytes32Array = DataTypetUtils.stringArrayToBytes32StaticArray(
            new String[WeIdConstant.STRING_ARRAY_LENGTH]
        );

        System.out.println("=======weId:" + new Address(WeIdUtils.convertWeIdToAddress(weId)));
        System.out.println("=======sign:" + JsonUtil.objToJsonStr(rsvSignature));
        List<Type> inputParameters = Arrays.<Type>asList(
            new Address(WeIdUtils.convertWeIdToAddress(weId)),
            this.getParamCreated(),
            bytes32Array,
            this.getParamJsonSchema(cptJsonSchemaNew),
            rsvSignature.getV(),
            rsvSignature.getR(),
            rsvSignature.getS());

        Function function = new Function("registerCpt", inputParameters, Collections.emptyList());
        return FunctionEncoder.encode(function);
    }

    private RsvSignature sign(
        String cptPublisher,
        String jsonSchema,
        String privateKey) throws Exception {

        StringBuilder sb = new StringBuilder();
        sb.append(cptPublisher);
        sb.append("|");
        sb.append(jsonSchema);
        SignatureData signatureData =
            SignatureUtils.signMessage(sb.toString(), privateKey);

        Uint8 v = DataTypetUtils.intToUnt8(Integer.valueOf(signatureData.getV()));
        Bytes32 r = DataTypetUtils.bytesArrayToBytes32(signatureData.getR());
        Bytes32 s = DataTypetUtils.bytesArrayToBytes32(signatureData.getS());

        RsvSignature rsvSignature = new RsvSignature();
        rsvSignature.setV(v);
        rsvSignature.setR(r);
        rsvSignature.setS(s);
        return rsvSignature;
    }

    private String cptSchemaToString(Map<String, Object> cptJsonSchema) throws Exception {

        Map<String, Object> cptJsonSchemaNew = new HashMap<String, Object>();
        cptJsonSchemaNew.put(JsonSchemaConstant.SCHEMA_KEY, JsonSchemaConstant.SCHEMA_VALUE);
        cptJsonSchemaNew.put(JsonSchemaConstant.TYPE_KEY, JsonSchemaConstant.DATE_TYPE_OBJECT);
        cptJsonSchemaNew.putAll(cptJsonSchema);
        return JsonUtil.objToJsonStr(cptJsonSchemaNew);
    }

    private StaticArray<Int256> getParamCreated() {

        long[] longArray = new long[WeIdConstant.LONG_ARRAY_LENGTH];
        long created = System.currentTimeMillis();
        longArray[1] = created;
        return DataTypetUtils.longArrayToInt256StaticArray(longArray);
    }

    private StaticArray<Bytes32> getParamJsonSchema(String cptJsonSchemaNew) {

        List<String> stringList = Splitter
            .fixedLength(WeIdConstant.BYTES32_FIXED_LENGTH)
            .splitToList(cptJsonSchemaNew);
        String[] jsonSchemaArray = new String[WeIdConstant.JSON_SCHEMA_ARRAY_LENGTH];
        for (int i = 0; i < stringList.size(); i++) {
            jsonSchemaArray[i] = stringList.get(i);
        }
        return DataTypetUtils.stringArrayToBytes32StaticArray(jsonSchemaArray);
    }
}
