/*
 *       Copyright© (2019-2020) WeBank Co., Ltd.
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

package com.webank.weid.http.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webank.weid.config.ContractConfig;
import com.webank.weid.config.FiscoConfig;
import com.webank.weid.constant.ParamKeyConstant;
import com.webank.weid.http.constant.HttpReturnCode;
import com.webank.weid.http.constant.WeIdentityFunctionNames;
import com.webank.weid.http.constant.WeIdentityParamKeyConstant;
import com.webank.weid.http.protocol.request.InputArg;
import com.webank.weid.http.protocol.response.HttpResponseData;
import com.webank.weid.http.service.BaseService;
import com.webank.weid.http.service.InvokerAuthorityIssuerService;
import com.webank.weid.http.service.InvokerCptService;
import com.webank.weid.http.service.InvokerCredentialService;
import com.webank.weid.http.service.InvokerEvidenceService;
import com.webank.weid.http.service.InvokerWeIdService;
import com.webank.weid.http.service.TransactionService;
import com.webank.weid.http.util.JsonUtil;
import com.webank.weid.http.util.PropertiesUtil;
import com.webank.weid.http.util.TransactionEncoderUtil;
import com.webank.weid.http.util.TransactionEncoderUtilV2;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Handling Transaction related services.
 *
 * @author chaoxinhu and darwindu
 **/
@Component
public class TransactionServiceImpl extends BaseService implements TransactionService {

    private Logger logger = LoggerFactory.getLogger(TransactionServiceImpl.class);

    private InvokerAuthorityIssuerService invokerAuthorityIssuerService =
        new InvokerAuthorityIssuerServiceImpl();
    private InvokerWeIdService invokerWeIdService = new InvokerWeIdServiceImpl();
    private InvokerCptService invokerCptService = new InvokerCptServiceImpl();
    private InvokerCredentialService invokerCredentialService = new InvokerCredentialServiceImpl();
    private InvokerEvidenceService invokerEvidenceService = new InvokerEvidenceServiceImpl();

    /**
     * Create an Encoded Transaction.
     *
     * @param encodeTransactionJsonArgs json format args. It should contain 4 keys: functionArgs (including all business related params),
     * transactionArgs, functionName and apiVersion. Hereafter, functionName will decide which WeID SDK method to engage, and assemble all input
     * params into SDK readable format to send there; apiVersion is for extensibility purpose.
     * @return encoded transaction in Base64 format, and the data segment in RawTransaction.
     */
    @Override
    public HttpResponseData<Object> encodeTransaction(
        String encodeTransactionJsonArgs) {
        try {
            HttpResponseData<InputArg> resp = TransactionEncoderUtil
                .buildInputArg(encodeTransactionJsonArgs);
            InputArg inputArg = resp.getRespBody();
            if (inputArg == null) {
                logger.error("Failed to build input argument: {}", encodeTransactionJsonArgs);
                return new HttpResponseData<>(null, resp.getErrorCode(), resp.getErrorMessage());
            }

            String functionName = inputArg.getFunctionName();
            String functionArg = inputArg.getFunctionArg();
            HttpResponseData<String> httpResponseData;
            if (functionName.equalsIgnoreCase(WeIdentityFunctionNames.FUNCNAME_CREATE_CREDENTIALPOJO)) {
                HttpResponseData<Object> credResp = TransactionEncoderUtilV2.encodeCredential(inputArg);
                return new HttpResponseData<>(credResp.getRespBody(), credResp.getErrorCode(),
                    credResp.getErrorMessage());
            }

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode txnArgNode = objectMapper.readTree(inputArg.getTransactionArg());
            JsonNode nonceNode = txnArgNode.get(WeIdentityParamKeyConstant.NONCE);
            if (nonceNode == null || StringUtils.isEmpty(nonceNode.textValue())) {
                logger.error("Null input within: {}", txnArgNode.toString());
                return new HttpResponseData<>(null, HttpReturnCode.NONCE_ILLEGAL);
            }
            String nonce = JsonUtil.removeDoubleQuotes(nonceNode.toString());
            // Load WeIdentity related contract addresses
            FiscoConfig fiscoConfig = new FiscoConfig();
            fiscoConfig.load();
            ContractConfig config = PropertiesUtil.buildContractConfig(fiscoConfig);
            if (TransactionEncoderUtil.isFiscoBcosV1()) {
                if (functionName.equalsIgnoreCase(WeIdentityFunctionNames.FUNCNAME_CREATE_WEID)) {
                    httpResponseData = TransactionEncoderUtil
                        .createWeIdEncoder(functionArg, nonce, config.getWeIdAddress());
                    return new HttpResponseData<>(
                        JsonUtil.convertJsonToSortedMap(httpResponseData.getRespBody()),
                        httpResponseData.getErrorCode(),
                        httpResponseData.getErrorMessage());
                }
                if (functionName
                    .equalsIgnoreCase(WeIdentityFunctionNames.FUNCNAME_REGISTER_AUTHORITY_ISSUER)) {
                    httpResponseData = TransactionEncoderUtil
                        .registerAuthorityIssuerEncoder(functionArg, nonce, config.getIssuerAddress());
                    return new HttpResponseData<>(
                        JsonUtil.convertJsonToSortedMap(httpResponseData.getRespBody()),
                        httpResponseData.getErrorCode(),
                        httpResponseData.getErrorMessage());
                }
                if (functionName.equalsIgnoreCase(WeIdentityFunctionNames.FUNCNAME_REGISTER_CPT)) {
                    httpResponseData = TransactionEncoderUtil
                        .registerCptEncoder(functionArg, nonce, config.getCptAddress());
                    return new HttpResponseData<>(
                        JsonUtil.convertJsonToSortedMap(httpResponseData.getRespBody()),
                        httpResponseData.getErrorCode(),
                        httpResponseData.getErrorMessage());
                }
            } else {
                // is FISCO-BCOS v2 blockchain
                httpResponseData = TransactionEncoderUtilV2
                    .createEncoder(functionArg, nonce, functionName);
                return new HttpResponseData<>(
                    JsonUtil.convertJsonToSortedMap(httpResponseData.getRespBody()),
                    httpResponseData.getErrorCode(),
                    httpResponseData.getErrorMessage());
            }
            logger.error("Function name undefined: {}.", functionName);
            return new HttpResponseData<>(null, HttpReturnCode.FUNCTION_NAME_ILLEGAL);
        } catch (Exception e) {
            logger.error("[createEncodingFunction]: unknown error with input argment {}",
                encodeTransactionJsonArgs,
                e);
            return new HttpResponseData<>(null, HttpReturnCode.UNKNOWN_ERROR.getCode(),
                HttpReturnCode.UNKNOWN_ERROR.getCodeDesc().concat(e.getMessage()));
        }
    }

    /**
     * Send Transaction to Blockchain.
     *
     * @param sendTransactionJsonArgs the json format args. It should contain 4 keys: functionArgs (including all business related params),
     * transactionArgs, functionName and apiVersion.
     * @return the json string from SDK response.
     */
    @Override
    public HttpResponseData<Object> sendTransaction(String sendTransactionJsonArgs) {
        try {
            HttpResponseData<InputArg> resp = TransactionEncoderUtil
                .buildInputArg(sendTransactionJsonArgs);
            InputArg inputArg = resp.getRespBody();
            if (inputArg == null) {
                logger.error("Failed to build input argument: {}", sendTransactionJsonArgs);
                return new HttpResponseData<>(StringUtils.EMPTY, resp.getErrorCode(),
                    resp.getErrorMessage());
            }
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode txnArgNode = objectMapper.readTree(inputArg.getTransactionArg());
            JsonNode nonceNode = txnArgNode.get(WeIdentityParamKeyConstant.NONCE);
            JsonNode dataNode = txnArgNode.get(WeIdentityParamKeyConstant.TRANSACTION_DATA);
            JsonNode signedMessageNode = txnArgNode
                .get(WeIdentityParamKeyConstant.SIGNED_MESSAGE);
            if (nonceNode == null || StringUtils.isEmpty(nonceNode.textValue())) {
                logger.error("Null input within: {}", txnArgNode.toString());
                return new HttpResponseData<>(null, HttpReturnCode.NONCE_ILLEGAL);
            }
            if (dataNode == null || StringUtils.isEmpty(dataNode.textValue())) {
                logger.error("Null input within: {}", txnArgNode.toString());
                return new HttpResponseData<>(null, HttpReturnCode.DATA_ILLEGAL);
            }
            if (signedMessageNode == null || StringUtils.isEmpty(signedMessageNode.textValue())) {
                logger.error("Null input within: {}", txnArgNode.toString());
                return new HttpResponseData<>(null, HttpReturnCode.SIGNED_MSG_ILLEGAL);
            }

            // Load WeIdentity related contract addresses
            FiscoConfig fiscoConfig = new FiscoConfig();
            fiscoConfig.load();
            ContractConfig config = PropertiesUtil.buildContractConfig(fiscoConfig);

            String functionName = inputArg.getFunctionName();
            String nonce = JsonUtil.removeDoubleQuotes(nonceNode.toString());
            String data = JsonUtil.removeDoubleQuotes(dataNode.toString());
            String signedMessage = signedMessageNode.textValue();
            HttpResponseData<String> httpResponseData =
                new HttpResponseData<>(null, HttpReturnCode.INPUT_NULL);
            String txnHex;
            if (TransactionEncoderUtil.isFiscoBcosV1()) {
                if (functionName.equalsIgnoreCase(WeIdentityFunctionNames.FUNCNAME_CREATE_WEID)) {
                    txnHex = TransactionEncoderUtil
                        .createTxnHex(signedMessage, nonce, config.getWeIdAddress(), data);
                    if (StringUtils.isEmpty(txnHex)) {
                        return new HttpResponseData<>(null, HttpReturnCode.TXN_HEX_ERROR);
                    }
                    httpResponseData = invokerWeIdService.createWeIdWithTransactionHex(txnHex);
                    return new HttpResponseData<>(
                        JsonUtil.convertJsonToSortedMap(httpResponseData.getRespBody()),
                        httpResponseData.getErrorCode(),
                        httpResponseData.getErrorMessage());
                }
                if (functionName
                    .equalsIgnoreCase(WeIdentityFunctionNames.FUNCNAME_REGISTER_AUTHORITY_ISSUER)) {
                    txnHex = TransactionEncoderUtil
                        .createTxnHex(signedMessage, nonce, config.getIssuerAddress(), data);
                    if (StringUtils.isEmpty(txnHex)) {
                        return new HttpResponseData<>(null, HttpReturnCode.TXN_HEX_ERROR);
                    }
                    httpResponseData = invokerAuthorityIssuerService
                        .registerAuthorityIssuerWithTransactionHex(txnHex);
                    return new HttpResponseData<>(
                        JsonUtil.convertJsonToSortedMap(httpResponseData.getRespBody()),
                        httpResponseData.getErrorCode(),
                        httpResponseData.getErrorMessage());
                }
                if (functionName.equalsIgnoreCase(WeIdentityFunctionNames.FUNCNAME_REGISTER_CPT)) {
                    txnHex = TransactionEncoderUtil
                        .createTxnHex(signedMessage, nonce, config.getCptAddress(), data);
                    if (StringUtils.isEmpty(txnHex)) {
                        return new HttpResponseData<>(null, HttpReturnCode.TXN_HEX_ERROR);
                    }
                    httpResponseData = invokerCptService.registerCptWithTransactionHex(txnHex);
                    return new HttpResponseData<>(
                        JsonUtil.convertJsonToSortedMap(httpResponseData.getRespBody()),
                        httpResponseData.getErrorCode(),
                        httpResponseData.getErrorMessage());
                }
            } else { // is FISCO-BCOS v2
                if (functionName.equalsIgnoreCase(WeIdentityFunctionNames.FUNCNAME_CREATE_WEID)) {
                    txnHex = TransactionEncoderUtilV2
                        .createTxnHex(signedMessage, nonce, config.getWeIdAddress(), data);
                    httpResponseData = invokerWeIdService.createWeIdWithTransactionHex(txnHex);
                }
                if (functionName.equalsIgnoreCase(WeIdentityFunctionNames.FUNCNAME_REGISTER_AUTHORITY_ISSUER)) {
                    txnHex = TransactionEncoderUtilV2
                        .createTxnHex(signedMessage, nonce, config.getIssuerAddress(), data);
                    httpResponseData = invokerAuthorityIssuerService
                        .registerAuthorityIssuerWithTransactionHex(txnHex);
                }
                if (functionName.equalsIgnoreCase(WeIdentityFunctionNames.FUNCCALL_REGISTER_CPT)) {
                    txnHex = TransactionEncoderUtilV2
                        .createTxnHex(signedMessage, nonce, config.getCptAddress(), data);
                    httpResponseData = invokerCptService.registerCptWithTransactionHex(txnHex);
                }
                return new HttpResponseData<>(
                    JsonUtil.convertJsonToSortedMap(httpResponseData.getRespBody()),
                    httpResponseData.getErrorCode(),
                    httpResponseData.getErrorMessage());
            }
            logger.error("Function name undefined: {}.", functionName);
            return new HttpResponseData<>(null, HttpReturnCode.FUNCTION_NAME_ILLEGAL);
        } catch (Exception e) {
            logger.error("[sendTransaction]: unknown error with input argument {}",
                sendTransactionJsonArgs,
                e);
            return new HttpResponseData<>(null, HttpReturnCode.UNKNOWN_ERROR.getCode(),
                HttpReturnCode.UNKNOWN_ERROR.getCodeDesc().concat(e.getMessage()));
        }
    }

    /**
     * Directly invoke an SDK function. No client-side sign needed.
     *
     * @param invokeFunctionJsonArgs the json format args. It should contain 4 keys: functionArgs, (including all business related params), EMPTY
     * transactionArgs, functionName and apiVersion.
     * @return the json string from SDK response.
     */
    @Override
    public HttpResponseData<Object> invokeFunction(String invokeFunctionJsonArgs) {
        HttpResponseData<InputArg> resp = TransactionEncoderUtil
            .buildInputArg(invokeFunctionJsonArgs);
        InputArg inputArg = resp.getRespBody();
        if (inputArg == null) {
            logger.error("Failed to build input argument: {}", invokeFunctionJsonArgs);
            return new HttpResponseData<>(null, resp.getErrorCode(), resp.getErrorMessage());
        }
        String functionName = inputArg.getFunctionName();
        try {
            if (functionName.equalsIgnoreCase(WeIdentityFunctionNames.FUNCNAME_CREATE_CREDENTIAL)) {
                return invokerCredentialService.createCredentialInvoke(inputArg);
            }
            if (functionName.equalsIgnoreCase(WeIdentityFunctionNames.FUNCNAME_CREATE_CREDENTIALPOJO)) {
                return invokerCredentialService.createCredentialPojoInvoke(inputArg);
            }
            if (functionName.equalsIgnoreCase(WeIdentityFunctionNames.FUNCNAME_VERIFY_CREDENTIAL)) {
                return invokerCredentialService.verifyCredentialInvoke(inputArg);
            }
            if (functionName.equalsIgnoreCase(WeIdentityFunctionNames.FUNCNAME_VERIFY_CREDENTIALPOJO)) {
                HttpResponseData<Boolean> respData = invokerCredentialService.verifyCredentialPojoInvoke(inputArg);
                return new HttpResponseData<>(respData.getRespBody(), respData.getErrorCode(), respData.getErrorMessage());
            }
            if (functionName
                .equalsIgnoreCase(WeIdentityFunctionNames.FUNCNAME_QUERY_AUTHORITY_ISSUER)) {
                return invokerAuthorityIssuerService.queryAuthorityIssuerInfoInvoke(inputArg);
            }
            if (functionName.equalsIgnoreCase(WeIdentityFunctionNames.FUNCNAME_GET_WEID_DOCUMENT)
                || functionName
                .equalsIgnoreCase(WeIdentityFunctionNames.FUNCNAME_GET_WEID_DOCUMENT_JSON)) {
                return invokerWeIdService.getWeIdDocumentJsonInvoke(inputArg);
            }
            if (functionName.equalsIgnoreCase(WeIdentityFunctionNames.FUNCNAME_QUERY_CPT)) {
                return invokerCptService.queryCptInvoke(inputArg);
            }
            if (functionName.equalsIgnoreCase(WeIdentityFunctionNames.FUNCNAME_CREATE_WEID)) {
                return invokerWeIdService.createWeIdInvoke(inputArg);
            }
            if (functionName.equalsIgnoreCase(WeIdentityFunctionNames.FUNCNAME_REGISTER_CPT)) {
                return invokerCptService.registerCptInvoke(inputArg);
            }
            if (functionName
                .equalsIgnoreCase(WeIdentityFunctionNames.FUNCNAME_REGISTER_AUTHORITY_ISSUER)) {
                return invokerAuthorityIssuerService.registerAuthorityIssuerInvoke(inputArg);
            }
            if (functionName
                .equalsIgnoreCase(WeIdentityFunctionNames.FUNCNAME_ADD_WEID_TO_WHITELIST)) {
                return invokerAuthorityIssuerService.addWeIdToWhitelist(inputArg);
            }
            if (functionName
                .equalsIgnoreCase(WeIdentityFunctionNames.FUNCNAME_IS_WEID_IN_WHITELIST)) {
                return invokerAuthorityIssuerService.isWeIdInWhitelist(inputArg);
            }
            if (functionName.equalsIgnoreCase(WeIdentityFunctionNames.FUNCNAME_GET_WEID_DOCUMENT_BY_ORG)) {
                String weId = (String) invokerAuthorityIssuerService.getWeIdByNameInvoke(inputArg).getRespBody();
                // Construct new InputArg
                Map<String, Object> funcArgMap = new LinkedHashMap<>();
                funcArgMap.put(ParamKeyConstant.WEID, weId);
                inputArg.setFunctionArg(JsonUtil.objToJsonStr(funcArgMap));
                return invokerWeIdService.getWeIdDocumentJsonInvoke(inputArg);
            }
            if (functionName.equalsIgnoreCase(WeIdentityFunctionNames.FUNCNAME_CREATE_EVIDENCE_FOR_LITE_CREDENTIAL)) {
                // 1. call createevidencewithcustomkeyandlog
                return invokerEvidenceService.createEvidenceWithExtraInfo(inputArg);
            }
            if (functionName.equalsIgnoreCase(WeIdentityFunctionNames.FUNCNAME_CREATE_WEID_WITH_PUBKEY)) {
                return invokerWeIdService.createWeIdWithPubKey(inputArg);
            }
            if (functionName.equalsIgnoreCase(WeIdentityFunctionNames.FUNCNAME_VERIFY_LITE_CREDENTIAL)) {
                return invokerEvidenceService.getEvidenceByCustomKey(inputArg);
            }
            if (functionName.equalsIgnoreCase(WeIdentityFunctionNames.FUNCNAME_ECCENCRYPT_CREDENTIAL)) {
                return invokerCredentialService.createCredentialPojoAndEncryptInvoke(inputArg);
            }
            if (functionName.equalsIgnoreCase(WeIdentityFunctionNames.FUNCNAME_ECCENCRYPT)) {
                return invokerCredentialService.eccEncrypt(inputArg);
            }
            if (functionName.equalsIgnoreCase(WeIdentityFunctionNames.FUNCNAME_ECCDECRYPT)) {
                return invokerCredentialService.eccDecrypt(inputArg);
            }
            if (functionName.equalsIgnoreCase(WeIdentityFunctionNames.FUNCNAME_GET_EVIDENCE_BY_HASH)) {
                return invokerEvidenceService.getEvidenceByHash(inputArg);
            }
            logger.error("Function name undefined: {}.", functionName);
            return new HttpResponseData<>(null, HttpReturnCode.FUNCTION_NAME_ILLEGAL);
        } catch (Exception e) {
            logger.error("[invokeFunction]: unknown error with input argument {}",
                invokeFunctionJsonArgs,
                e);
            return new HttpResponseData<>(null, HttpReturnCode.UNKNOWN_ERROR.getCode(),
                HttpReturnCode.UNKNOWN_ERROR.getCodeDesc());
        }
    }
}
