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

package com.webank.weid.http.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webank.weid.config.FiscoConfig;
import com.webank.weid.http.constant.HttpReturnCode;
import com.webank.weid.http.constant.WeIdentityFunctionNames;
import com.webank.weid.http.constant.WeIdentityParamKeyConstant;
import com.webank.weid.http.protocol.request.InputArg;
import com.webank.weid.http.protocol.response.EncodedTransactionWrapper;
import com.webank.weid.http.protocol.response.HttpResponseData;
import com.webank.weid.protocol.response.ResponseData;
import com.webank.weid.service.BaseService;
import com.webank.weid.util.DataToolUtils;
import com.webank.weid.util.TransactionUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handling all tasks related to encoding and sending transactions.
 *
 * @author chaoxinhu
 */
public class TransactionEncoderUtil {

    private static Logger logger = LoggerFactory.getLogger(TransactionEncoderUtil.class);

    public static boolean isFiscoBcosV1() {
        FiscoConfig fiscoConfig = new FiscoConfig();
        fiscoConfig.load();
        if (fiscoConfig.getVersion().startsWith("1")) {
            return true;
        }
        return false;
    }
    
    /**
     * Get a random Nonce for a transaction.
     *
     * @return nonce in BigInt.
     */
    public static BigInteger getNonce() {
        return TransactionUtils.getNonce();
    }


    /**
     * Get a rawTransaction instance, based on pre-defined parameters.
     *
     * @param nonce the nonce value
     * @param data the data segment
     * @param to contract address
     */
    /*public static RawTransaction buildRawTransaction(String nonce, String data, String to) {
        return RawTransaction.createTransaction(
            new BigInteger(nonce),
            new BigInteger("99999999999"),
            new BigInteger("99999999999"),
            getBlockLimit(),
            to,
            data
        );
    }*/


    /**
     * Extract and build Input arg for all Service APIs.
     *
     * @param inputJson the inputJson String
     * @return An InputArg instance
     */
    public static HttpResponseData<InputArg> buildInputArg(String inputJson) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(inputJson);
            if (jsonNode == null) {
                logger.error("Null input within: {}", inputJson);
                return new HttpResponseData<>(null, HttpReturnCode.INPUT_NULL);
            }
            JsonNode functionNameNode = jsonNode.get(WeIdentityParamKeyConstant.FUNCTION_NAME);
            JsonNode versionNode = jsonNode.get(WeIdentityParamKeyConstant.API_VERSION);
            if (functionNameNode == null || StringUtils.isEmpty(functionNameNode.textValue())) {
                logger.error("Null input within: {}", jsonNode.toString());
                return new HttpResponseData<>(null, HttpReturnCode.FUNCTION_NAME_ILLEGAL);
            }
            if (versionNode == null || StringUtils.isEmpty(versionNode.textValue())) {
                logger.error("Null input within: {}", jsonNode.toString());
                return new HttpResponseData<>(null, HttpReturnCode.VER_ILLEGAL);
            }
            // Need to use toString() for pure Objects and textValue() for pure String
            JsonNode functionArgNode = jsonNode.get(WeIdentityParamKeyConstant.FUNCTION_ARG);
            if (functionArgNode == null || StringUtils.isEmpty(functionArgNode.toString())) {
                logger.error("Null input within: {}", jsonNode.toString());
                return new HttpResponseData<>(null, HttpReturnCode.FUNCARG_ILLEGAL);
            }
            JsonNode txnArgNode = jsonNode.get(WeIdentityParamKeyConstant.TRANSACTION_ARG);
            if (txnArgNode == null || StringUtils.isEmpty(txnArgNode.toString())) {
                logger.error("Null input within: {}", jsonNode.toString());
                return new HttpResponseData<>(null, HttpReturnCode.TXNARG_ILLEGAL);
            }

            String functionArg = functionArgNode.toString();
            String txnArg = txnArgNode.toString();
            InputArg inputArg = new InputArg();
            inputArg.setFunctionArg(functionArg);
            inputArg.setTransactionArg(txnArg);
            inputArg.setFunctionName(functionNameNode.textValue());
            inputArg.setV(versionNode.textValue());
            return new HttpResponseData<>(inputArg, HttpReturnCode.SUCCESS);
        } catch (Exception e) {
            logger.error("Json Extraction error within: {}", inputJson);
            return new HttpResponseData<>(null, HttpReturnCode.UNKNOWN_ERROR.getCode(),
                HttpReturnCode.UNKNOWN_ERROR.getCodeDesc().concat(e.getMessage()));
        }
    }

    /**
     * Get a default blocklimit for a transaction.
     *
     * @return blocklimit in BigInt.
     */
    private static BigInteger getBlockLimit() {
        try {
            return BigInteger.valueOf(BaseService.getBlockNumber());
        } catch (IOException e) {
            logger.error("get BlockNumber error.", e);
        }
        return BigInteger.ZERO;
    }

    /**
     * Get a rawTransaction instance, based on pre-defined parameters and input Function.
     *
     * @param data the input function instance
     * @param nonce the nonce value
     * @param to contract address
     * @return rawTransaction
     */
    /*private static RawTransaction createRawTransactionFromFunction(
        String data,
        String nonce,
        String to) {
        return RawTransaction.createTransaction(
            new BigInteger(nonce),
            new BigInteger("99999999999"),
            new BigInteger("99999999999"),
            getBlockLimit(),
            to,
            data
        );
    }*/

    /**
     * Get the encoded function byte array from a rawTransaction.
     *
     * @param rawTransaction the input rawTransaction
     * @return rawTransaction
     */
    /*private static byte[] encodeRawTransaction(RawTransaction rawTransaction) {
        return TransactionEncoder.encode(rawTransaction);
    }*/

    /**
     * Get the encoded transaction byte array and rawTransaction into a json String as output.
     *
     * @param encodedTransaction the encoded transaction byte array (will be converted to Base64)
     * @param data the input rawTransaction's data
     * @param blockLimit the blockLimit
     * @return Json String, a wrapper including both Base64 encodes, and the rawTransaction
     */
    public static String getEncodeOutput(byte[] encodedTransaction, String data, BigInteger blockLimit) {
        String base64EncodedTransaction = base64Encode(encodedTransaction);
        EncodedTransactionWrapper encodedTransactionWrapper = new EncodedTransactionWrapper();
        encodedTransactionWrapper.setEncodedTransaction(base64EncodedTransaction);
        encodedTransactionWrapper.setData(data);
        encodedTransactionWrapper.setBlockLimit(blockLimit.toString());
        return JsonUtil.objToJsonStr(encodedTransactionWrapper);
    }

    /**
     * Convert a non-Base64 byte array into a Base64 encoded String.
     *
     * @param nonBase64ByteArray byte array
     * @return Base64 string
     */
    private static String base64Encode(byte[] nonBase64ByteArray) {
        return new String(DataToolUtils.base64Encode(nonBase64ByteArray), StandardCharsets.UTF_8);
    }
}
