package com.webank.weid.http.service;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import org.apache.commons.lang3.StringUtils;
import org.bcos.web3j.abi.FunctionEncoder;
import org.bcos.web3j.abi.datatypes.Address;
import org.bcos.web3j.abi.datatypes.Function;
import org.bcos.web3j.abi.datatypes.StaticArray;
import org.bcos.web3j.abi.datatypes.Type;
import org.bcos.web3j.abi.datatypes.generated.Bytes32;
import org.bcos.web3j.abi.datatypes.generated.Int256;
import org.bcos.web3j.abi.datatypes.generated.Uint8;
import org.bcos.web3j.crypto.Sign.SignatureData;
import org.bcos.web3j.crypto.TransactionEncoder;
import org.bcos.web3j.protocol.core.methods.request.RawTransaction;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.webank.weid.constant.ErrorCode;
import com.webank.weid.constant.JsonSchemaConstant;
import com.webank.weid.constant.ParamKeyConstant;
import com.webank.weid.constant.WeIdConstant;
import com.webank.weid.http.constant.HttpReturnCode;
import com.webank.weid.http.exception.BizException;
import com.webank.weid.http.protocol.request.ReqRegisterCptMapArgs;
import com.webank.weid.http.protocol.request.ReqRegisterCptStringArgs;
import com.webank.weid.http.protocol.request.ReqRegisterSignCptMapArgs;
import com.webank.weid.http.protocol.request.ReqUpdateCptArgs;
import com.webank.weid.http.protocol.request.ReqUpdateCptStringArgs;
import com.webank.weid.http.protocol.response.HttpResponseData;
import com.webank.weid.http.util.InputUtil;
import com.webank.weid.http.util.TransactionEncoderUtil;
import com.webank.weid.protocol.base.Cpt;
import com.webank.weid.protocol.base.CptBaseInfo;
import com.webank.weid.protocol.base.WeIdAuthentication;
import com.webank.weid.protocol.base.WeIdPrivateKey;
import com.webank.weid.protocol.request.CptMapArgs;
import com.webank.weid.protocol.request.CptStringArgs;
import com.webank.weid.protocol.response.ResponseData;
import com.webank.weid.protocol.response.RsvSignature;
import com.webank.weid.rpc.CptService;
import com.webank.weid.service.impl.CptServiceImpl;
import com.webank.weid.util.DataTypetUtils;
import com.webank.weid.util.JsonUtil;
import com.webank.weid.util.WeIdUtils;

@Service
public class InvokerCptService extends BaseService {

    private Logger logger = LoggerFactory.getLogger(InvokerCptService.class);

    private CptService cptService = new CptServiceImpl();

    /**
     * This is used to register a new CPT to the blockchain.
     *
     * @param reqRegisterCptMapArgs the args
     * @return the response data
     */
    public ResponseData<CptBaseInfo> registerCpt(
        ReqRegisterCptMapArgs reqRegisterCptMapArgs) {

        ResponseData<CptBaseInfo> response = new ResponseData<CptBaseInfo>();
        try {
            WeIdPrivateKey weIdPrivateKey = new WeIdPrivateKey();
            weIdPrivateKey.setPrivateKey(reqRegisterCptMapArgs.getWeIdPrivateKey());

            WeIdAuthentication weIdAuthentication = this.buildWeIdAuthority(
                reqRegisterCptMapArgs.getWeIdPrivateKey(),
                reqRegisterCptMapArgs.getWeId());

            CptMapArgs cptMapArgs = new CptMapArgs();
            cptMapArgs.setWeIdAuthentication(weIdAuthentication);
            cptMapArgs.setCptJsonSchema(reqRegisterCptMapArgs.getCptJsonSchema());

            response = cptService.registerCpt(cptMapArgs);
        } catch (Exception e) {
            logger.error("[registerCpt]: unknow error. reqRegisterCptArgs:{}",
                reqRegisterCptMapArgs,
                e);
            return new ResponseData<>(null, ErrorCode.BASE_ERROR);
        }
        return response;
    }

    public ResponseData<byte[]> getEncodedTransaction(
        ReqRegisterSignCptMapArgs reqRegisterSignCptMapArgs) {

        try {
            RawTransaction rawTransaction = this.getRawTransaction(
                reqRegisterSignCptMapArgs.getDataJson(),
                reqRegisterSignCptMapArgs.getSignatureData());

            byte[] result = TransactionEncoder.encode(rawTransaction);

            ResponseData<byte[]> response = new ResponseData<byte[]>();
            response.setResult(result);
            return response;
        } catch (Exception e) {
            throw new BizException("[getEncodedTransaction] error", e);
        }
    }

    public ResponseData<CptBaseInfo> registerSignCpt(
        ReqRegisterSignCptMapArgs reqRegisterSignCptMapArgs) {

        try {
            Map<String, Object> dataJsonMap = reqRegisterSignCptMapArgs.getDataJson();
            Map<String, Object> cptJsonSchema = (HashMap<String, Object>) dataJsonMap
                .get("cptJsonSchema");

            String weId = dataJsonMap.get("weId").toString();
            WeIdAuthentication weIdAuthentication = this.buildWeIdAuthority(null, weId);

            CptMapArgs cptMapArgs = new CptMapArgs();
            cptMapArgs.setCptJsonSchema(cptJsonSchema);
            cptMapArgs.setWeIdAuthentication(weIdAuthentication);

            byte[] signedMessage = TransactionEncoderUtil.encodeTransactionWithSignature(
                this.getRawTransaction(dataJsonMap, reqRegisterSignCptMapArgs.getSignatureData()),
                reqRegisterSignCptMapArgs.getBodySigned());

            String hexValue = Hex.toHexString(signedMessage);
            String result = cptService.registerCpt(hexValue).getResult();
            ObjectMapper mapper = new ObjectMapper();
            CptBaseInfo cptBaseInfo = mapper.readValue(result, CptBaseInfo.class);
            System.out.println("=====response:" + result);
            return new ResponseData<>(cptBaseInfo, ErrorCode.SUCCESS);
        } catch (Exception e) {
            logger.error("[registerCpt]: unknow error. reqRegisterCptArgs:{}",
                reqRegisterSignCptMapArgs,
                e);
            return new ResponseData<>(null, ErrorCode.BASE_ERROR);
        }
    }

    /**
     * Call to WeID SDK with direct transaction hex String, to register CPT.
     *
     * @param transactionHex the transactionHex value
     * @return String in ResponseData
     */
    public HttpResponseData<String> registerCptWithTransactionHex(String transactionHex) {
        try {
            ResponseData<String> responseData = cptService.registerCpt(transactionHex);
            if (responseData.getErrorCode() != ErrorCode.SUCCESS.getCode()) {
                logger.error("[registerCpt]: error occurred: {}, {}", responseData.getErrorCode(),
                    responseData.getErrorMessage());
            }
            return new HttpResponseData<>(responseData.getResult(), responseData.getErrorCode(),
                responseData.getErrorMessage());
        } catch (Exception e) {
            logger.error("[registerCpt]: unknown error, input arguments:{}",
                transactionHex,
                e);
            return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.UNKNOWN_ERROR);
        }
    }

    /**
     * build WeIdAuthority
     *
     * @param weIdPrivateKeyStr this is String
     * @param weId this is String
     */
    private WeIdAuthentication buildWeIdAuthority(String weIdPrivateKeyStr, String weId) {

        WeIdPrivateKey weIdPrivateKey = new WeIdPrivateKey();
        weIdPrivateKey.setPrivateKey(weIdPrivateKeyStr);

        WeIdAuthentication weIdAuthentication = new WeIdAuthentication();
        weIdAuthentication.setWeId(weId);
        weIdAuthentication.setWeIdPrivateKey(weIdPrivateKey);
        return weIdAuthentication;
    }

    /**
     * This is used to register a new CPT to the blockchain.
     *
     * @param reqRegisterCptStringArgs the args
     * @return the response data
     */
    public ResponseData<CptBaseInfo> registerCpt(
        ReqRegisterCptStringArgs reqRegisterCptStringArgs) {

        ResponseData<CptBaseInfo> response = new ResponseData<CptBaseInfo>();
        try {
            WeIdAuthentication weIdAuthentication = this.buildWeIdAuthority(
                reqRegisterCptStringArgs.getWeIdPrivateKey(),
                reqRegisterCptStringArgs.getWeId());

            CptStringArgs cptStringArgs = new CptStringArgs();
            cptStringArgs.setWeIdAuthentication(weIdAuthentication);
            cptStringArgs.setCptJsonSchema(reqRegisterCptStringArgs.getCptJsonSchema());

            response = cptService.registerCpt(cptStringArgs);
        } catch (Exception e) {
            logger.error("[registerCpt]: unknow error. reqRegisterCptArgs:{}",
                reqRegisterCptStringArgs,
                e);
            return new ResponseData<>(null, ErrorCode.BASE_ERROR);
        }
        return response;
    }

    /**
     * this is used to query cpt with the latest version which has been registered.
     *
     * @param cptId the cpt id
     * @return the response data
     */
    public ResponseData<Cpt> queryCpt(Integer cptId) {

        ResponseData<Cpt> response = new ResponseData<Cpt>();
        try {
            response = cptService.queryCpt(cptId);
        } catch (Exception e) {
            logger.error("[queryCpt]: unknow error. cptId:{}", cptId, e);
            return new ResponseData<>(null, ErrorCode.BASE_ERROR);
        }
        return response;
    }

    /**
     * Query CPT via the InvokeFunction API.
     *
     * @param queryArgs the query arg
     * @return the CPT data
     */
    public HttpResponseData<String> queryCptInvoke(String queryArgs) {
        try {
            JsonNode cptIdNode = new ObjectMapper().readTree(queryArgs)
                .get(ParamKeyConstant.CPT_ID);
            if (cptIdNode == null || StringUtils
                .isEmpty(InputUtil.removeDoubleQuotes(cptIdNode.toString()))) {
                return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.INPUT_NULL);
            }
            ResponseData response = cptService
                .queryCpt(Integer.valueOf(InputUtil.removeDoubleQuotes(cptIdNode.toString())));
            return new HttpResponseData<>(
                JsonUtil.objToJsonStr(response.getResult()),
                response.getErrorCode(),
                response.getErrorMessage());
        } catch (Exception e) {
            logger.error(
                "[queryCpt]: unknow error. cptId:{}.",
                queryArgs,
                e);
            return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.UNKNOWN_ERROR);
        }
    }

    /**
     * This is used to update a CPT data which has been register.
     *
     * @param reqUpdateCptArgs the args
     * @return the response data
     */
    public ResponseData<CptBaseInfo> updateCpt(ReqUpdateCptArgs reqUpdateCptArgs) {

        ResponseData<CptBaseInfo> response = new ResponseData<CptBaseInfo>();
        try {
            WeIdAuthentication weIdAuthentication = this.buildWeIdAuthority(
                reqUpdateCptArgs.getWeIdPrivateKey(),
                reqUpdateCptArgs.getWeId());

            CptMapArgs cptMapArgs = new CptMapArgs();
            cptMapArgs.setCptJsonSchema(reqUpdateCptArgs.getCptJsonSchema());
            cptMapArgs.setWeIdAuthentication(weIdAuthentication);

            response = cptService.updateCpt(cptMapArgs, reqUpdateCptArgs.getCptId());
        } catch (Exception e) {
            logger.error("[updateCpt]: unknow error. reqUpdateCptArgs:{}", reqUpdateCptArgs, e);
            return new ResponseData<>(null, ErrorCode.BASE_ERROR);
        }
        return response;
    }

    /**
     * This is used to update a CPT data which has been register.
     *
     * @param reqUpdateCptStringArgs the args
     * @return the response data
     */
    public ResponseData<CptBaseInfo> updateCpt(ReqUpdateCptStringArgs reqUpdateCptStringArgs) {

        ResponseData<CptBaseInfo> response = new ResponseData<CptBaseInfo>();
        try {
            WeIdAuthentication weIdAuthentication = this.buildWeIdAuthority(
                reqUpdateCptStringArgs.getWeIdPrivateKey(),
                reqUpdateCptStringArgs.getWeId());

            CptStringArgs cptStringArgs = new CptStringArgs();
            cptStringArgs.setCptJsonSchema(reqUpdateCptStringArgs.getCptJsonSchema());
            cptStringArgs.setWeIdAuthentication(weIdAuthentication);

            response = cptService.updateCpt(cptStringArgs, reqUpdateCptStringArgs.getCptId());
        } catch (Exception e) {
            logger
                .error("[updateCpt]: unknow error. reqUpdateCptArgs:{}", reqUpdateCptStringArgs, e);
            return new ResponseData<>(null, ErrorCode.BASE_ERROR);
        }
        return response;
    }

    private RawTransaction getRawTransaction(
        Map<String, Object> jsonSchemaMap,
        SignatureData signatureData) throws Exception {

        String data = this.createFuntionRegisterCpt(jsonSchemaMap, signatureData);

        return RawTransaction.createTransaction(
            TransactionEncoderUtil.getNonce(),
            new BigInteger("99999999999"),
            new BigInteger("99999999999"),
            TransactionEncoderUtil.getBlockLimit(),
            "0xfcd14ed03e6d94ca127d557a1883dd042a81ea11",
            new BigInteger("0"),
            data,
            BigInteger.ZERO,
            false);
    }


    private String createFuntionRegisterCpt(
        Map<String, Object> map,
        SignatureData signatureData) throws Exception {

        String weId = map.get("weId").toString();
        Map<String, Object> cptJsonSchema = (Map<String, Object>) map.get("cptJsonSchema");
        String cptJsonSchemaNew = this.cptSchemaToString(cptJsonSchema);
        RsvSignature rsvSignature = sign(
            weId,
            cptJsonSchemaNew,
            signatureData);

        StaticArray<Bytes32> bytes32Array = DataTypetUtils.stringArrayToBytes32StaticArray(
            new String[WeIdConstant.STRING_ARRAY_LENGTH]
        );

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
        SignatureData signatureData) throws Exception {

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
