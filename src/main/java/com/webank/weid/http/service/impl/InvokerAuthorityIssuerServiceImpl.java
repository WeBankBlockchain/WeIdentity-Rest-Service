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

package com.webank.weid.http.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webank.weid.blockchain.constant.ErrorCode;
import com.webank.weid.blockchain.protocol.response.ResponseData;
import com.webank.weid.constant.ParamKeyConstant;
import com.webank.weid.constant.WeIdConstant;
import com.webank.weid.exception.InitWeb3jException;
import com.webank.weid.exception.LoadContractException;
import com.webank.weid.http.constant.HttpReturnCode;
import com.webank.weid.http.constant.WeIdentityParamKeyConstant;
import com.webank.weid.http.protocol.request.InputArg;
import com.webank.weid.http.protocol.response.HttpResponseData;
import com.webank.weid.http.service.BaseService;
import com.webank.weid.http.service.InvokerAuthorityIssuerService;
import com.webank.weid.http.util.JsonUtil;
import com.webank.weid.http.util.KeyUtil;
import com.webank.weid.protocol.base.AuthorityIssuer;
import com.webank.weid.protocol.base.WeIdAuthentication;
import com.webank.weid.protocol.base.WeIdPrivateKey;
import com.webank.weid.protocol.request.RegisterAuthorityIssuerArgs;
import com.webank.weid.service.impl.AuthorityIssuerServiceImpl;
import com.webank.weid.service.rpc.AuthorityIssuerService;
import com.webank.weid.util.WeIdUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InvokerAuthorityIssuerServiceImpl extends BaseService implements
    InvokerAuthorityIssuerService {

    private Logger logger = LoggerFactory.getLogger(InvokerAuthorityIssuerServiceImpl.class);

    private AuthorityIssuerService authorityIssuerService = new AuthorityIssuerServiceImpl();

    //private RawTransactionService rawTransactionService = new RawTransactionServiceImpl();

    /**
     * Register a new Authority Issuer on Chain via Invoke function.
     *
     * @param registerArgs the args
     * @return the Boolean response data
     */
    @Override
    public HttpResponseData<Object> registerAuthorityIssuerInvoke(
        InputArg registerArgs) {
        try {
            JsonNode functionArgNode = new ObjectMapper().readTree(registerArgs.getFunctionArg());
            JsonNode weIdNode = functionArgNode.get(ParamKeyConstant.WEID);
            JsonNode nameNode = functionArgNode.get(ParamKeyConstant.AUTHORITY_ISSUER_NAME);
            JsonNode txnArgNode = new ObjectMapper().readTree(registerArgs.getTransactionArg());
            JsonNode keyIndexNode = txnArgNode.get(WeIdentityParamKeyConstant.KEY_INDEX);
            if (weIdNode == null || StringUtils.isEmpty(weIdNode.textValue())
                || nameNode == null || StringUtils.isEmpty(nameNode.textValue())
                || keyIndexNode == null || StringUtils.isEmpty(keyIndexNode.textValue())) {
                return new HttpResponseData<>(null, HttpReturnCode.INPUT_NULL);
            }
            // return the name length check in this place
            if (nameNode.textValue().length() > WeIdConstant.MAX_AUTHORITY_ISSUER_NAME_LENGTH) {
                return new HttpResponseData<>(null,
                    ErrorCode.AUTHORITY_ISSUER_NAME_ILLEGAL.getCode(),
                    ErrorCode.AUTHORITY_ISSUER_NAME_ILLEGAL.getCodeDesc());
            }

            String weIdPrivKey = KeyUtil
                .getPrivateKeyByWeId(KeyUtil.SDK_PRIVKEY_PATH, keyIndexNode.textValue());
            if (StringUtils.isEmpty(weIdPrivKey)) {
                return new HttpResponseData<>(null, HttpReturnCode.INVOKER_ILLEGAL);
            }
            WeIdPrivateKey weIdPrivateKey = new WeIdPrivateKey();
            weIdPrivateKey.setPrivateKey(weIdPrivKey);
            AuthorityIssuer authorityIssuer = new AuthorityIssuer();
            authorityIssuer.setAccValue("1");
            authorityIssuer.setCreated(System.currentTimeMillis());
            authorityIssuer.setName(nameNode.textValue());
            authorityIssuer.setWeId(weIdNode.textValue());

            RegisterAuthorityIssuerArgs registerAuthorityIssuerArgs =
                new RegisterAuthorityIssuerArgs();
            registerAuthorityIssuerArgs.setWeIdPrivateKey(weIdPrivateKey);
            registerAuthorityIssuerArgs.setAuthorityIssuer(authorityIssuer);
            ResponseData<Boolean> response = authorityIssuerService
                .registerAuthorityIssuer(registerAuthorityIssuerArgs);

            return new HttpResponseData<>(
                response.getResult(),
                response.getErrorCode(), response.getErrorMessage());
        } catch (LoadContractException e) {
            return new HttpResponseData<>(null, HttpReturnCode.CONTRACT_ERROR.getCode(), HttpReturnCode.CONTRACT_ERROR.getCodeDesc());
        } catch (InitWeb3jException e) {
            return new HttpResponseData<>(null, HttpReturnCode.WEB3J_ERROR.getCode(), HttpReturnCode.WEB3J_ERROR.getCodeDesc());
        } catch (Exception e) {
            logger.error(
                "[registerAuthorityIssuer]: unknow error. weId:{}.",
                registerArgs,
                e);
            return new HttpResponseData<>(null, HttpReturnCode.WEID_SDK_ERROR.getCode(),
                HttpReturnCode.WEID_SDK_ERROR.getCodeDesc().concat(e.getMessage()));
        }
    }

    /**
     * Call to WeID SDK with direct transaction hex String, to register AuthorityIssuer.
     *
     * @param transactionHex the transactionHex value
     * @return String in ResponseData
     */
    @Override
    public HttpResponseData<String> registerAuthorityIssuerWithTransactionHex(
        String transactionHex) {
        try {
            com.webank.weid.blockchain.protocol.response.ResponseData<String> responseData = rawTransactionService
                .registerAuthorityIssuer(transactionHex);
            if (responseData.getErrorCode() != ErrorCode.SUCCESS.getCode()) {
                logger.error("[registerCpt]: error occurred: {}, {}", responseData.getErrorCode(),
                    responseData.getErrorMessage());
            }
            return new HttpResponseData<>(responseData.getResult(), responseData.getErrorCode(),
                responseData.getErrorMessage());
        } catch (LoadContractException e) {
            return new HttpResponseData<>(null, HttpReturnCode.CONTRACT_ERROR.getCode(), HttpReturnCode.CONTRACT_ERROR.getCodeDesc());
        } catch (InitWeb3jException e) {
            return new HttpResponseData<>(null, HttpReturnCode.WEB3J_ERROR.getCode(), HttpReturnCode.WEB3J_ERROR.getCodeDesc());
        } catch (Exception e) {
            logger.error("[registerAuthorityIssuer]: unknown error, input arguments:{}",
                transactionHex,
                e);
            return new HttpResponseData<>(StringUtils.EMPTY,
                HttpReturnCode.WEID_SDK_ERROR.getCode(),
                HttpReturnCode.WEID_SDK_ERROR.getCodeDesc().concat(e.getMessage()));
        }
    }

    /**
     * Query Authority Issuer via the InvokeFunction API.
     *
     * @param queryArgs the query WeID
     * @return the authorityIssuer
     */
    @Override
    public HttpResponseData<Object> queryAuthorityIssuerInfoInvoke(
        InputArg queryArgs) {
        try {
            JsonNode weIdNode = new ObjectMapper()
                .readTree(queryArgs.getFunctionArg())
                .get(ParamKeyConstant.WEID);
            if (weIdNode == null || StringUtils.isEmpty(weIdNode.textValue())) {
                return new HttpResponseData<>(null, HttpReturnCode.INPUT_NULL);
            }
            ResponseData response = authorityIssuerService
                .queryAuthorityIssuerInfo(weIdNode.textValue());
            return new HttpResponseData<>(
                JsonUtil.convertJsonToSortedMap(JsonUtil.objToJsonStr(response.getResult())),
                response.getErrorCode(),
                response.getErrorMessage());
        } catch (LoadContractException e) {
            return new HttpResponseData<>(null, HttpReturnCode.CONTRACT_ERROR.getCode(), HttpReturnCode.CONTRACT_ERROR.getCodeDesc());
        } catch (InitWeb3jException e) {
            return new HttpResponseData<>(null, HttpReturnCode.WEB3J_ERROR.getCode(), HttpReturnCode.WEB3J_ERROR.getCodeDesc());
        } catch (Exception e) {
            logger.error(
                "[queryAuthorityIssuer]: unknow error. weId:{}.",
                queryArgs,
                e);
            return new HttpResponseData<>(null, HttpReturnCode.WEID_SDK_ERROR.getCode(),
                HttpReturnCode.WEID_SDK_ERROR.getCodeDesc().concat(e.getMessage()));
        }
    }

    @Override
    public HttpResponseData<Object> getWeIdByNameInvoke(InputArg arg) {
        try {
            JsonNode nameNode = new ObjectMapper()
                .readTree(arg.getFunctionArg()).get(WeIdentityParamKeyConstant.ORG_ID);
            if (nameNode == null || StringUtils.isEmpty(nameNode.textValue())) {
                return new HttpResponseData<>(null, HttpReturnCode.INPUT_NULL);
            }
            ResponseData<String> addressResp = authorityIssuerService.getWeIdByOrgId(nameNode.textValue());
            if (StringUtils.isEmpty(addressResp.getResult())) {
                return new HttpResponseData<>(StringUtils.EMPTY, addressResp.getErrorCode(),
                    addressResp.getErrorMessage());
            }
            return new HttpResponseData<>(addressResp.getResult(),
                HttpReturnCode.SUCCESS);
        } catch (LoadContractException e) {
            return new HttpResponseData<>(null, HttpReturnCode.CONTRACT_ERROR.getCode(), HttpReturnCode.CONTRACT_ERROR.getCodeDesc());
        } catch (InitWeb3jException e) {
            return new HttpResponseData<>(null, HttpReturnCode.WEB3J_ERROR.getCode(), HttpReturnCode.WEB3J_ERROR.getCodeDesc());
        } catch (Exception e) {
            logger.error(
                "[queryAuthorityIssuer]: unknow error. weId:{}.",
                arg,
                e);
            return new HttpResponseData<>(null, HttpReturnCode.WEID_SDK_ERROR.getCode(),
                HttpReturnCode.WEID_SDK_ERROR.getCodeDesc().concat(e.getMessage()));
        }
    }

    @Override
    public HttpResponseData<Object> addWeIdToWhitelist(InputArg args) {
        try {
            JsonNode functionArgNode = new ObjectMapper().readTree(args.getFunctionArg());
            JsonNode weIdNode = functionArgNode.get(ParamKeyConstant.WEID);
            JsonNode whiteListNameNode = functionArgNode.get(WeIdentityParamKeyConstant.WHITELIST_NAME);
            JsonNode txnArgNode = new ObjectMapper().readTree(args.getTransactionArg());
            JsonNode keyIndexNode = txnArgNode.get(WeIdentityParamKeyConstant.KEY_INDEX);
            if (weIdNode == null || StringUtils.isEmpty(weIdNode.textValue())
                || whiteListNameNode == null || StringUtils.isEmpty(whiteListNameNode.textValue())
                || keyIndexNode == null || StringUtils.isEmpty(keyIndexNode.textValue())) {
                return new HttpResponseData<>(null, HttpReturnCode.INPUT_NULL);
            }
            String weIdPrivKey = KeyUtil
                .getPrivateKeyByWeId(KeyUtil.SDK_PRIVKEY_PATH, keyIndexNode.textValue());
            if (StringUtils.isEmpty(weIdPrivKey)) {
                return new HttpResponseData<>(null, HttpReturnCode.INVOKER_ILLEGAL);
            }
            String issuer = WeIdUtils.getWeIdFromPrivateKey(weIdPrivKey);
            WeIdAuthentication callerAuth = new WeIdAuthentication(issuer, weIdPrivKey);
            ResponseData<Boolean> response = authorityIssuerService.addIssuerIntoIssuerType(callerAuth, whiteListNameNode.textValue(), weIdNode.textValue());
            return new HttpResponseData<>(response.getResult(), response.getErrorCode(),response.getErrorMessage());
        } catch (LoadContractException e) {
            return new HttpResponseData<>(null, HttpReturnCode.CONTRACT_ERROR.getCode(), HttpReturnCode.CONTRACT_ERROR.getCodeDesc());
        } catch (InitWeb3jException e) {
            return new HttpResponseData<>(null, HttpReturnCode.WEB3J_ERROR.getCode(), HttpReturnCode.WEB3J_ERROR.getCodeDesc());
        } catch (Exception e) {
            logger.error(
                "[addWeIdToIssuerTypeInvoke]: unknow error. args:{}.",
                args,
                e);
            return new HttpResponseData<>(null, HttpReturnCode.WEID_SDK_ERROR.getCode(),
                HttpReturnCode.WEID_SDK_ERROR.getCodeDesc().concat(e.getMessage()));
        }
    }

    @Override
    public HttpResponseData<Object> isWeIdInWhitelist(InputArg args) {
        try {
            JsonNode functionArgNode = new ObjectMapper().readTree(args.getFunctionArg());
            JsonNode weIdNode = functionArgNode.get(ParamKeyConstant.WEID);
            JsonNode whiteListNameNode = functionArgNode.get(WeIdentityParamKeyConstant.WHITELIST_NAME);
            if (weIdNode == null || StringUtils.isEmpty(weIdNode.textValue())
                || whiteListNameNode == null || StringUtils.isEmpty(whiteListNameNode.textValue())) {
                return new HttpResponseData<>(null, HttpReturnCode.INPUT_NULL);
            }
            ResponseData<Boolean> response = authorityIssuerService.isSpecificTypeIssuer(whiteListNameNode.textValue(), weIdNode.textValue());
            return new HttpResponseData<>(response.getResult(), response.getErrorCode(),response.getErrorMessage());
        } catch (LoadContractException e) {
            return new HttpResponseData<>(null, HttpReturnCode.CONTRACT_ERROR.getCode(), HttpReturnCode.CONTRACT_ERROR.getCodeDesc());
        } catch (InitWeb3jException e) {
            return new HttpResponseData<>(null, HttpReturnCode.WEB3J_ERROR.getCode(), HttpReturnCode.WEB3J_ERROR.getCodeDesc());
        } catch (Exception e) {
            logger.error(
                "[checkWeIdByIssuerTypeInvoke]: unknow error. args:{}.",
                args,
                e);
            return new HttpResponseData<>(null, HttpReturnCode.WEID_SDK_ERROR.getCode(),
                HttpReturnCode.WEID_SDK_ERROR.getCodeDesc().concat(e.getMessage()));
        }
    }

    @Override
    public HttpResponseData<Object> recognizeAuthorityIssuer(InputArg args) {
        try {
            JsonNode functionArgNode = new ObjectMapper().readTree(args.getFunctionArg());
            JsonNode weIdNode = functionArgNode.get(ParamKeyConstant.WEID);
            JsonNode txnArgNode = new ObjectMapper().readTree(args.getTransactionArg());
            JsonNode keyIndexNode = txnArgNode.get(WeIdentityParamKeyConstant.KEY_INDEX);
            if (weIdNode == null || StringUtils.isEmpty(weIdNode.textValue())) {
                return new HttpResponseData<>(null, HttpReturnCode.INPUT_NULL);
            }
            String weIdPrivKey = KeyUtil
                .getPrivateKeyByWeId(KeyUtil.SDK_PRIVKEY_PATH, keyIndexNode.textValue());
            if (StringUtils.isEmpty(weIdPrivKey)) {
                return new HttpResponseData<>(null, HttpReturnCode.INVOKER_ILLEGAL);
            }
            ResponseData<Boolean> response = authorityIssuerService.recognizeAuthorityIssuer(weIdNode.textValue(), new WeIdPrivateKey(weIdPrivKey));
            return new HttpResponseData<>(response.getResult(), response.getErrorCode(),response.getErrorMessage());
        } catch (LoadContractException e) {
            return new HttpResponseData<>(null, HttpReturnCode.CONTRACT_ERROR.getCode(), HttpReturnCode.CONTRACT_ERROR.getCodeDesc());
        } catch (InitWeb3jException e) {
            return new HttpResponseData<>(null, HttpReturnCode.WEB3J_ERROR.getCode(), HttpReturnCode.WEB3J_ERROR.getCodeDesc());
        } catch (Exception e) {
            logger.error(
                "[RecognizeAuthorityIssuer]: unknow error. args:{}.",
                args,
                e);
            return new HttpResponseData<>(null, HttpReturnCode.WEID_SDK_ERROR.getCode(),
                HttpReturnCode.WEID_SDK_ERROR.getCodeDesc().concat(e.getMessage()));
        }
    }

    @Override
    public HttpResponseData<Object> deRecognizeAuthorityIssuer(InputArg args) {
        try {
            JsonNode functionArgNode = new ObjectMapper().readTree(args.getFunctionArg());
            JsonNode weIdNode = functionArgNode.get(ParamKeyConstant.WEID);
            JsonNode txnArgNode = new ObjectMapper().readTree(args.getTransactionArg());
            JsonNode keyIndexNode = txnArgNode.get(WeIdentityParamKeyConstant.KEY_INDEX);
            if (weIdNode == null || StringUtils.isEmpty(weIdNode.textValue())) {
                return new HttpResponseData<>(null, HttpReturnCode.INPUT_NULL);
            }
            String weIdPrivKey = KeyUtil
                .getPrivateKeyByWeId(KeyUtil.SDK_PRIVKEY_PATH, keyIndexNode.textValue());
            if (StringUtils.isEmpty(weIdPrivKey)) {
                return new HttpResponseData<>(null, HttpReturnCode.INVOKER_ILLEGAL);
            }
            ResponseData<Boolean> response = authorityIssuerService.deRecognizeAuthorityIssuer(weIdNode.textValue(), new WeIdPrivateKey(weIdPrivKey));
            return new HttpResponseData<>(response.getResult(), response.getErrorCode(),response.getErrorMessage());
        } catch (LoadContractException e) {
            return new HttpResponseData<>(null, HttpReturnCode.CONTRACT_ERROR.getCode(), HttpReturnCode.CONTRACT_ERROR.getCodeDesc());
        } catch (InitWeb3jException e) {
            return new HttpResponseData<>(null, HttpReturnCode.WEB3J_ERROR.getCode(), HttpReturnCode.WEB3J_ERROR.getCodeDesc());
        } catch (Exception e) {
            logger.error(
                "[DeRecognizeAuthorityIssuer]: unknow error. args:{}.",
                args,
                e);
            return new HttpResponseData<>(null, HttpReturnCode.WEID_SDK_ERROR.getCode(),
                HttpReturnCode.WEID_SDK_ERROR.getCodeDesc().concat(e.getMessage()));
        }
    }
}
