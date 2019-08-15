/*
 *       Copyright© (2019) WeBank Co., Ltd.
 *
 *       This file is part of weidentity-http-service.
 *
 *       weidentity-http-service is free software: you can redistribute it and/or modify
 *       it under the terms of the GNU Lesser General Public License as published by
 *       the Free Software Foundation, either version 3 of the License, or
 *       (at your option) any later version.
 *
 *       weidentity-http-service is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU Lesser General Public License for more details.
 *
 *       You should have received a copy of the GNU Lesser General Public License
 *       along with weidentity-http-service.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.webank.weid.http.util;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.bcos.web3j.abi.FunctionEncoder;
import org.bcos.web3j.abi.datatypes.Function;
import org.bcos.web3j.abi.datatypes.Type;
import org.bcos.web3j.crypto.EncryptType;
import org.bcos.web3j.crypto.Sign.SignatureData;
import org.bcos.web3j.crypto.TransactionEncoder;
import org.bcos.web3j.protocol.core.methods.request.RawTransaction;
import org.bcos.web3j.rlp.RlpEncoder;
import org.bcos.web3j.rlp.RlpList;
import org.bcos.web3j.rlp.RlpString;
import org.bcos.web3j.rlp.RlpType;
import org.bcos.web3j.utils.Bytes;
import org.bcos.web3j.utils.Numeric;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.webank.weid.http.constant.HttpReturnCode;
import com.webank.weid.http.constant.WeIdentityFunctionNames;
import com.webank.weid.http.constant.WeIdentityParamKeyConstant;
import com.webank.weid.http.protocol.request.InputArg;
import com.webank.weid.http.protocol.response.EncodedTransactionWrapper;
import com.webank.weid.http.protocol.response.HttpResponseData;
import com.webank.weid.protocol.response.ResponseData;
import com.webank.weid.util.DataToolUtils;
import com.webank.weid.util.TransactionUtils;

/**
 * Handling all tasks related to encoding and sending transactions.
 *
 * @author chaoxinhu
 */
public class TransactionEncoderUtil {

    private static Logger logger = LoggerFactory.getLogger(TransactionEncoderUtil.class);

    /**
     * Create an Encoded Transaction for registerCpt.
     *
     * @param inputParam the CPT input param which should contain: weId, cptJsonSchema (json
     * String), and cptSignature (in Base64).
     * @param nonce the nonce value to create rawTransaction
     * @param to contract address
     * @return encoded byte array in Base64 format and the rawTransaction.
     */
    public static HttpResponseData<String> registerCptEncoder(
        String inputParam,
        String nonce,
        String to) {
        try {
            ResponseData<List<Type>> responseData = TransactionUtils
                .buildRegisterCptInputParameters(inputParam);
            if (responseData.getResult() == null) {
                logger.error("[RegisterCpt] Error occurred when building input param with: {}",
                    inputParam);
                return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.INPUT_ILLEGAL);
            }
            Function function = new Function(
                WeIdentityFunctionNames.FUNCCALL_REGISTER_CPT,
                responseData.getResult(),
                Collections.emptyList());
            RawTransaction rawTransaction = createRawTransactionFromFunction(function, nonce, to);
            byte[] encodedTransaction = encodeRawTransaction(rawTransaction);
            return new HttpResponseData<>(
                getEncodeOutput(encodedTransaction, rawTransaction),
                HttpReturnCode.SUCCESS);
        } catch (Exception e) {
            logger.error("[RegisterCpt] Failed to get encoder for unknown reason: ", e);
            return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.TXN_ENCODER_ERROR);
        }
    }

    /**
     * Create an Encoded Transaction for createWeIdWithAttributes.
     *
     * @param inputParam the createWeId param should contain publicKey only.
     * @param nonce the nonce value to create rawTransaction
     * @param to contract address
     * @return encoded byte array in Base64 format and the rawTransaction.
     */
    public static HttpResponseData<String> createWeIdEncoder(
        String inputParam,
        String nonce,
        String to) {
        try {
            ResponseData<List<Type>> responseData = TransactionUtils
                .buildCreateWeIdInputParameters(inputParam);
            if (responseData.getResult() == null) {
                logger.error("[CreateWeId] Error occurred when building input param with: {}",
                    inputParam);
                return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.INPUT_ILLEGAL);
            }
            Function function = new Function(
                WeIdentityFunctionNames.FUNCCALL_SET_ATTRIBUTE,
                responseData.getResult(),
                Collections.emptyList());
            RawTransaction rawTransaction = createRawTransactionFromFunction(function, nonce, to);
            byte[] encodedTransaction = encodeRawTransaction(rawTransaction);
            return new HttpResponseData<>(
                getEncodeOutput(encodedTransaction, rawTransaction),
                HttpReturnCode.SUCCESS);
        } catch (Exception e) {
            logger.error("[createWeId] Failed to get encoder for unknown reason:", e);
            return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.TXN_ENCODER_ERROR);
        }
    }

    /**
     * Create an Encoded Transaction for registerAuthorityIssuer
     *
     * @param inputParam the registerAuthorityIssuer param should contain weId and name.
     * @param nonce the nonce value to create rawTransaction
     * @param to contract address
     * @return encoded byte array in Base64 format and the rawTransaction.
     */
    public static HttpResponseData<String> registerAuthorityIssuerEncoder(
        String inputParam,
        String nonce,
        String to) {
        try {
            ResponseData<List<Type>> responseData = TransactionUtils
                .buildAuthorityIssuerInputParameters(inputParam);
            if (responseData.getResult() == null) {
                logger.error(
                    "[RegisterAuthorityIssuer] Error occurred when building input param with: {}",
                    inputParam);
                return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.INPUT_ILLEGAL);
            }
            Function function = new Function(
                WeIdentityFunctionNames.FUNCCALL_ADD_AUTHORITY_ISSUER,
                responseData.getResult(),
                Collections.emptyList());
            RawTransaction rawTransaction = createRawTransactionFromFunction(function, nonce, to);
            byte[] encodedTransaction = encodeRawTransaction(rawTransaction);
            return new HttpResponseData<>(
                getEncodeOutput(encodedTransaction, rawTransaction),
                HttpReturnCode.SUCCESS);
        } catch (Exception e) {
            logger.error("[registerAuthorityIssuer] Failed to get encoder for unknown reason:", e);
            return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.TXN_ENCODER_ERROR);
        }
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
     * Obtain the hexed transaction string such that it can be directly send to blockchain. Requires
     * the previous rawTransaction and the signed Message from client side.
     *
     * @param rawTransaction the input rawTransaction
     * @param signedMessage the base64 signed message from client
     * @return Hex transaction String
     */
    public static String getTransactionHex(RawTransaction rawTransaction, String signedMessage) {
        if (rawTransaction == null || StringUtils.isEmpty(signedMessage)) {
            return StringUtils.EMPTY;
        }
        byte[] encodedSignedMessage = encodeTransactionWithSignature(
            rawTransaction,
            DataToolUtils.simpleSignatureDeserialization(
                DataToolUtils.base64Decode(signedMessage.getBytes(StandardCharsets.UTF_8))));
        return Hex.toHexString(encodedSignedMessage);
    }

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
     * Get a rawTransaction instance, based on pre-defined parameters.
     *
     * @param nonce the nonce value
     * @param data the data segment
     * @param to contract address
     */
    public static RawTransaction buildRawTransaction(String nonce, String data, String to) {
        return RawTransaction.createTransaction(
            new BigInteger(nonce),
            new BigInteger("99999999999"),
            new BigInteger("99999999999"),
            getBlockLimit(),
            to,
            new BigInteger("0"),
            data,
            BigInteger.ZERO,
            false);
    }

    /**
     * Get a default blocklimit for a transaction.
     *
     * @return blocklimit in BigInt.
     */
    private static BigInteger getBlockLimit() {
        return TransactionUtils.getBlockLimit();
    }

    /**
     * Obtain the hexed transaction string such that it can be directly send to blockchain. Requires
     * the previous rawTransaction and the signed Message from client side.
     *
     * @param rawTransaction the input rawTransaction
     * @param signatureData the signatureData
     * @return transaction byte array
     */
    private static byte[] encodeTransactionWithSignature(
        RawTransaction rawTransaction,
        SignatureData signatureData) {
        List<RlpType> values = asRlpValues(rawTransaction, signatureData);
        RlpList rlpList = new RlpList(values);
        return RlpEncoder.encode(rlpList);
    }

    /**
     * Get a rawTransaction instance, based on pre-defined parameters and input Function.
     *
     * @param function the input function instance
     * @param nonce the nonce value
     * @param to contract address
     * @return rawTransaction
     */
    private static RawTransaction createRawTransactionFromFunction(
        Function function,
        String nonce,
        String to) {
        String data = FunctionEncoder.encode(function);
        return RawTransaction.createTransaction(
            new BigInteger(nonce),
            new BigInteger("99999999999"),
            new BigInteger("99999999999"),
            getBlockLimit(),
            to,
            new BigInteger("0"),
            data,
            BigInteger.ZERO,
            false);
    }

    /**
     * Get the encoded function byte array from a rawTransaction.
     *
     * @param rawTransaction the input rawTransaction
     * @return rawTransaction
     */
    private static byte[] encodeRawTransaction(RawTransaction rawTransaction) {
        return TransactionEncoder.encode(rawTransaction);
    }

    /**
     * Get the encoded transaction byte array and rawTransaction into a json String as output.
     *
     * @param encodedTransaction the encoded transaction byte array (will be converted to Base64)
     * @param rawTransaction the input rawTransaction
     * @return Json String, a wrapper including both Base64 encodes, and the rawTransaction
     */
    private static String getEncodeOutput(byte[] encodedTransaction,
        RawTransaction rawTransaction) {
        String base64EncodedTransaction = base64Encode(encodedTransaction);
        EncodedTransactionWrapper encodedTransactionWrapper = new EncodedTransactionWrapper();
        encodedTransactionWrapper.setEncodedTransaction(base64EncodedTransaction);
        encodedTransactionWrapper.setData(rawTransaction.getData());
        return JsonUtil.objToJsonStr(encodedTransactionWrapper);
    }

    /**
     * Create the RLP list.
     *
     * @param rawTransaction the raw transaction
     * @param signatureData the signature data
     * @return List
     */
    private static List<RlpType> asRlpValues(
        RawTransaction rawTransaction,
        SignatureData signatureData) {
        List<RlpType> result = new ArrayList();
        result.add(RlpString.create(rawTransaction.getRandomid()));
        result.add(RlpString.create(rawTransaction.getGasPrice()));
        result.add(RlpString.create(rawTransaction.getGasLimit()));
        result.add(RlpString.create(rawTransaction.getBlockLimit()));
        String to = rawTransaction.getTo();
        if (to != null && to.length() > 0) {
            result.add(RlpString.create(Numeric.hexStringToByteArray(to)));
        } else {
            result.add(RlpString.create(""));
        }

        result.add(RlpString.create(rawTransaction.getValue()));
        byte[] data = Numeric.hexStringToByteArray(rawTransaction.getData());
        result.add(RlpString.create(data));
        String contractName = rawTransaction.getContractName();
        if (contractName != null && contractName.length() > 0) {
            result.add(RlpString.create(rawTransaction.getContractName()));
            result.add(RlpString.create(rawTransaction.getVersion()));
            result.add(RlpString.create(rawTransaction.getType()));
        }

        if (signatureData != null) {
            if (EncryptType.encryptType == 1) {
                result.add(RlpString.create(Bytes.trimLeadingZeroes(signatureData.getPub())));
                result.add(RlpString.create(Bytes.trimLeadingZeroes(signatureData.getR())));
                result.add(RlpString.create(Bytes.trimLeadingZeroes(signatureData.getS())));
            } else {
                result.add(RlpString.create(signatureData.getV()));
                result.add(RlpString.create(Bytes.trimLeadingZeroes(signatureData.getR())));
                result.add(RlpString.create(Bytes.trimLeadingZeroes(signatureData.getS())));
            }
        }
        return result;
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
