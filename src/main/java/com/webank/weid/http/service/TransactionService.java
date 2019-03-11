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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.bcos.web3j.protocol.core.methods.request.RawTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Service;

import com.webank.weid.config.ContractConfig;
import com.webank.weid.http.constant.HttpReturnCode;
import com.webank.weid.http.constant.WeIdentityFunctionNames;
import com.webank.weid.http.constant.WeIdentityParamKeyConstant;
import com.webank.weid.http.protocol.request.InputArg;
import com.webank.weid.http.protocol.response.HttpResponseData;
import com.webank.weid.http.util.InputUtil;
import com.webank.weid.http.util.TransactionEncoderUtil;

/**
 * Handling Transaction related services.
 *
 * @author chaoxinhu and darwindu
 **/
@Service
public class TransactionService extends BaseService {

    private Logger logger = LoggerFactory.getLogger(TransactionService.class);

    @Autowired
    private InvokerAuthorityIssuerService invokerAuthorityIssuerService;
    @Autowired
    private InvokerWeIdService invokerWeIdService;
    @Autowired
    private InvokerCptService invokerCptService;
    @Autowired
    private InvokerCredentialService invokerCredentialService;

    /**
     * Create an Encoded Transaction.
     *
     * @param encodeTransactionJsonArgs json format args. It should contain 4 keys: functionArgs
     * (including all business related params), transactionArgs, functionName and apiVersion.
     * Hereafter, functionName will decide which WeID SDK method to engage, and assemble all input
     * params into SDK readable format to send there; apiVersion is for extensibility purpose.
     * @return encoded transaction in Base64 format, and the data segment in RawTransaction.
     */
    public HttpResponseData<String> encodeTransaction(
        String encodeTransactionJsonArgs) {
        try {
            InputArg inputArg = TransactionEncoderUtil.buildInputArg(encodeTransactionJsonArgs);
            if (inputArg == null) {
                logger.error("Failed to build input argument: {}", encodeTransactionJsonArgs);
                return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.INPUT_NULL);
            }
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode txnArgNode = objectMapper.readTree(inputArg.getTransactionArg());
            JsonNode nonceNode = txnArgNode.get(WeIdentityParamKeyConstant.NONCE);
            if (nonceNode == null || StringUtils.isEmpty(nonceNode.textValue())) {
                logger.error("Null input within: {}", txnArgNode.toString());
                return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.INPUT_NULL);
            }
            String nonce = nonceNode.textValue();

            // Load WeIdentity related contract addresses
            ApplicationContext context = new ClassPathXmlApplicationContext(
                "applicationContext.xml");
            ContractConfig config = context.getBean(ContractConfig.class);

            String functionName = inputArg.getFunctionName();
            String functionArg = inputArg.getFunctionArg();
            if (functionName.equalsIgnoreCase(WeIdentityFunctionNames.FUNCNAME_CREATE_WEID)) {
                return TransactionEncoderUtil
                    .createWeIdEncoder(functionArg, nonce, config.getWeIdAddress());
            }
            if (functionName
                .equalsIgnoreCase(WeIdentityFunctionNames.FUNCNAME_REGISTER_AUTHORITY_ISSUER)) {
                return TransactionEncoderUtil
                    .registerAuthorityIssuerEncoder(functionArg, nonce, config.getIssuerAddress());
            }
            if (functionName.equalsIgnoreCase(WeIdentityFunctionNames.FUNCNAME_REGISTER_CPT)) {
                return TransactionEncoderUtil
                    .registerCptEncoder(functionArg, nonce, config.getCptAddress());
            }
            logger.error("Function name undefined: {}.", functionName);
            return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.UNKNOWN_FUNCTION_NAME);
        } catch (Exception e) {
            logger.error("[createEncodingFunction]: unknown error with input argment {}",
                encodeTransactionJsonArgs,
                e);
            return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.UNKNOWN_ERROR);
        }
    }

    /**
     * Send Transaction to Blockchain.
     *
     * @param sendTransactionJsonArgs the json format args. It should contain 4 keys: functionArgs
     * (including all business related params), transactionArgs, functionName and apiVersion.
     * @return the json string from SDK response.
     */
    public HttpResponseData<String> sendTransaction(String sendTransactionJsonArgs) {
        try {
            InputArg inputArg = TransactionEncoderUtil.buildInputArg(sendTransactionJsonArgs);
            if (inputArg == null) {
                logger.error("Failed to build input argument: {}", sendTransactionJsonArgs);
                return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.INPUT_NULL);
            }
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode txnArgNode = objectMapper.readTree(inputArg.getTransactionArg());
            JsonNode nonceNode = txnArgNode.get(WeIdentityParamKeyConstant.NONCE);
            JsonNode dataNode = txnArgNode.get(WeIdentityParamKeyConstant.TRANSACTION_DATA);
            if (nonceNode == null || dataNode == null
                || StringUtils.isEmpty(nonceNode.textValue())
                || StringUtils.isEmpty(dataNode.textValue())) {
                logger.error("Null input within: {}", txnArgNode.toString());
                return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.INPUT_NULL);
            }
            JsonNode functionArgNode = objectMapper.readTree(inputArg.getFunctionArg());
            JsonNode signedMessageNode = functionArgNode
                .get(WeIdentityParamKeyConstant.SIGNED_MESSAGE);
            if (signedMessageNode == null || StringUtils.isEmpty(signedMessageNode.textValue())) {
                logger.error("Null input within: {}", txnArgNode.toString());
                return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.INPUT_NULL);
            }

            // Load WeIdentity related contract addresses
            ApplicationContext context = new ClassPathXmlApplicationContext(
                "applicationContext.xml");
            ContractConfig config = context.getBean(ContractConfig.class);

            String functionName = inputArg.getFunctionName();
            String nonce = InputUtil.removeDoubleQuotes(nonceNode.toString());
            String data = InputUtil.removeDoubleQuotes(dataNode.toString());
            String signedMessage = signedMessageNode.textValue();
            if (functionName.equalsIgnoreCase(WeIdentityFunctionNames.FUNCNAME_CREATE_WEID)) {
                RawTransaction rawTransaction = TransactionEncoderUtil
                    .buildRawTransaction(nonce, data, config.getWeIdAddress());
                String transactionHex = TransactionEncoderUtil
                    .getTransactionHex(rawTransaction, signedMessage);
                if (StringUtils.isEmpty(transactionHex)) {
                    return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.TXN_HEX_ERROR);
                }
                return invokerWeIdService.createWeIdWithTransactionHex(transactionHex);
            }
            if (functionName
                .equalsIgnoreCase(WeIdentityFunctionNames.FUNCNAME_REGISTER_AUTHORITY_ISSUER)) {
                RawTransaction rawTransaction = TransactionEncoderUtil
                    .buildRawTransaction(nonce, data, config.getIssuerAddress());
                String transactionHex = TransactionEncoderUtil
                    .getTransactionHex(rawTransaction, signedMessage);
                if (StringUtils.isEmpty(transactionHex)) {
                    return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.TXN_HEX_ERROR);
                }
                return invokerAuthorityIssuerService
                    .registerAuthorityIssuerWithTransactionHex(transactionHex);
            }
            if (functionName.equalsIgnoreCase(WeIdentityFunctionNames.FUNCNAME_REGISTER_CPT)) {
                RawTransaction rawTransaction = TransactionEncoderUtil
                    .buildRawTransaction(nonce, data, config.getCptAddress());
                String transactionHex = TransactionEncoderUtil
                    .getTransactionHex(rawTransaction, signedMessage);
                if (StringUtils.isEmpty(transactionHex)) {
                    return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.TXN_HEX_ERROR);
                }
                return invokerCptService.registerCptWithTransactionHex(transactionHex);
            }
            logger.error("Function name undefined: {}.", functionName);
            return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.UNKNOWN_FUNCTION_NAME);
        } catch (Exception e) {
            logger.error("[sendTransaction]: unknown error with input argument {}",
                sendTransactionJsonArgs,
                e);
            return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.UNKNOWN_ERROR);
        }
    }

    /**
     * Directly invoke an SDK function. No client-side sign needed.
     *
     * @param invokeFunctionJsonArgs the json format args. It should contain 4 keys: functionArgs,
     * (including all business related params), EMPTY transactionArgs, functionName and apiVersion.
     * @return the json string from SDK response.
     */
    public HttpResponseData<String> invokeFunction(String invokeFunctionJsonArgs) {
        try {
            InputArg inputArg = TransactionEncoderUtil.buildInputArg(invokeFunctionJsonArgs);
            if (inputArg == null) {
                logger.error("Failed to build input argument: {}", invokeFunctionJsonArgs);
                return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.INPUT_NULL);
            }
            String functionName = inputArg.getFunctionName();
            String functionArg = inputArg.getFunctionArg();
            if (functionName.equalsIgnoreCase(WeIdentityFunctionNames.FUNCNAME_CREATE_CREDENTIAL)) {
                return invokerCredentialService.createCredentialInvoke(functionArg);
            }
            if (functionName.equalsIgnoreCase(WeIdentityFunctionNames.FUNCNAME_VERIFY_CREDENTIAL)) {
                return invokerCredentialService.verifyCredentialInvoke(functionArg);
            }
            if (functionName
                .equalsIgnoreCase(WeIdentityFunctionNames.FUNCNAME_GET_CREDENTIAL_JSON)) {
                return invokerCredentialService.getCredentialJsonInvoke(functionArg);
            }
            if (functionName
                .equalsIgnoreCase(WeIdentityFunctionNames.FUNCNAME_QUERY_AUTHORITY_ISSUER)) {
                return invokerAuthorityIssuerService.queryAuthorityIssuerInfoInvoke(functionArg);
            }
            if (functionName.equalsIgnoreCase(WeIdentityFunctionNames.FUNCNAME_GET_WEID_DOCUMENT)
                || functionName
                .equalsIgnoreCase(WeIdentityFunctionNames.FUNCNAME_GET_WEID_DOCUMENT_JSON)) {
                return invokerWeIdService.getWeIdDocumentJsonInvoke(functionArg);
            }
            if (functionName.equalsIgnoreCase(WeIdentityFunctionNames.FUNCNAME_QUERY_CPT)) {
                return invokerCptService.queryCptInvoke(functionArg);
            }
            logger.error("Function name undefined: {}.", functionName);
            return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.UNKNOWN_FUNCTION_NAME);
        } catch (Exception e) {
            logger.error("[invokeFunction]: unknown error with input argument {}",
                invokeFunctionJsonArgs,
                e);
            return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.UNKNOWN_ERROR);
        }
    }
}
