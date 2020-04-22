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

import com.webank.weid.http.constant.WeIdentityParamKeyConstant;
import com.webank.weid.util.WeIdUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.webank.weid.constant.ErrorCode;
import com.webank.weid.constant.ParamKeyConstant;
import com.webank.weid.http.constant.HttpReturnCode;
import com.webank.weid.http.protocol.request.InputArg;
import com.webank.weid.http.protocol.response.EndpointInfo;
import com.webank.weid.http.protocol.response.HttpResponseData;
import com.webank.weid.http.service.BaseService;
import com.webank.weid.http.service.InvokerWeIdService;
import com.webank.weid.http.util.EndpointDataUtil;
import com.webank.weid.http.util.JsonUtil;
import com.webank.weid.http.util.KeyUtil;
import com.webank.weid.protocol.base.WeIdPrivateKey;
import com.webank.weid.protocol.request.SetAuthenticationArgs;
import com.webank.weid.protocol.request.SetPublicKeyArgs;
import com.webank.weid.protocol.response.CreateWeIdDataResult;
import com.webank.weid.protocol.response.ResponseData;
import com.webank.weid.rpc.RawTransactionService;
import com.webank.weid.rpc.WeIdService;
import com.webank.weid.service.impl.RawTransactionServiceImpl;
import com.webank.weid.service.impl.WeIdServiceImpl;

@Component
public class InvokerWeIdServiceImpl extends BaseService implements InvokerWeIdService {

    private Logger logger = LoggerFactory.getLogger(InvokerWeIdServiceImpl.class);

    private WeIdService weIdService = new WeIdServiceImpl();

    private RawTransactionService rawTransactionService = new RawTransactionServiceImpl();


    /**
     * Create WeIdentity DID - a raw method for test purpose only.
     *
     * @return the response data
     */
    public ResponseData<CreateWeIdDataResult> createWeId() {

        ResponseData<CreateWeIdDataResult> response = new ResponseData<CreateWeIdDataResult>();
        try {
            response = weIdService.createWeId();
        } catch (Exception e) {
            logger.error("[createWeId]: unknow error, please check the error log.", e);
            return new ResponseData<>(null, ErrorCode.BASE_ERROR);
        }
        return response;
    }

    /**
     * Check if WeIdentity DID exists on Chain - for test purpose only.
     *
     * @param weId the WeIdentity DID
     * @return true if exists, false otherwise
     */
    public ResponseData<Boolean> isWeIdExist(String weId) {

        ResponseData<Boolean> response = new ResponseData<Boolean>();
        try {
            response = weIdService.isWeIdExist(weId);
        } catch (Exception e) {
            logger.error(
                "[isWeIdExist]: unknow error. weId:{}.",
                weId,
                e);
            return new ResponseData<>(false, ErrorCode.BASE_ERROR);
        }
        return response;
    }

    /**
     * Call to WeID SDK with direct transaction hex String, to create WeID.
     *
     * @param transactionHex the transactionHex value
     * @return String in ResponseData
     */
    public HttpResponseData<String> createWeIdWithTransactionHex(String transactionHex) {
        try {
            ResponseData<String> responseData = rawTransactionService.createWeId(transactionHex);
            if (responseData.getErrorCode() != ErrorCode.SUCCESS.getCode()) {
                logger.error("[createWeId]: error occurred: {}, {}", responseData.getErrorCode(),
                    responseData.getErrorMessage());
            }
            return new HttpResponseData<>(responseData.getResult(), responseData.getErrorCode(),
                responseData.getErrorMessage());
        } catch (Exception e) {
            logger.error("[createWeId]: unknown error, input arguments:{}",
                transactionHex,
                e);
            return new HttpResponseData<>(StringUtils.EMPTY,
                HttpReturnCode.WEID_SDK_ERROR.getCode(),
                HttpReturnCode.WEID_SDK_ERROR.getCodeDesc().concat(e.getMessage()));
        }
    }

    /**
     * Get a WeIdentity DID Document Json via the InvokeFunction API.
     *
     * @param getWeIdDocumentJsonArgs the WeIdentity DID
     * @return the WeIdentity DID document json
     */
    public HttpResponseData<Object> getWeIdDocumentJsonInvoke(
        InputArg getWeIdDocumentJsonArgs) {
        try {
            JsonNode weIdNode = new ObjectMapper()
                .readTree(getWeIdDocumentJsonArgs.getFunctionArg())
                .get(ParamKeyConstant.WEID);
            if (weIdNode == null || StringUtils.isEmpty(weIdNode.textValue())) {
                return new HttpResponseData<>(null, HttpReturnCode.INPUT_NULL);
            }
            ResponseData<String> response = weIdService.getWeIdDocumentJson(weIdNode.textValue());
            return new HttpResponseData<>(
                JsonUtil.convertJsonToSortedMap(response.getResult()),
                response.getErrorCode(),
                response.getErrorMessage());
        } catch (Exception e) {
            logger.error(
                "[getWeIdDocument]: unknow error. weId:{}.",
                getWeIdDocumentJsonArgs,
                e);
            return new HttpResponseData<>(null, HttpReturnCode.WEID_SDK_ERROR.getCode(),
                HttpReturnCode.WEID_SDK_ERROR.getCodeDesc().concat(e.getMessage()));
        }
    }

    /**
     * Create WeId via the InvokeFunction API.
     *
     * @param createWeIdJsonArgs the input args, should be almost null
     * @return the WeIdentity DID
     */
    public HttpResponseData<Object> createWeIdInvoke(InputArg createWeIdJsonArgs) {
        try {
            ResponseData<CreateWeIdDataResult> response = weIdService.createWeId();
            CreateWeIdDataResult createWeIdDataResult = response.getResult();
            if (createWeIdDataResult != null) {
                try {
                    // host the weId which just got created
                    KeyUtil.savePrivateKey(KeyUtil.SDK_PRIVKEY_PATH,
                        createWeIdDataResult.getWeId(),
                        createWeIdDataResult.getUserWeIdPrivateKey().getPrivateKey());
                } catch (Exception e) {
                    return new HttpResponseData<>(null, HttpReturnCode.INVOKER_ILLEGAL);
                }

                // set publicKey
                SetPublicKeyArgs setPublicKeyArgs = new SetPublicKeyArgs();
                setPublicKeyArgs.setWeId(createWeIdDataResult.getWeId());
                setPublicKeyArgs
                    .setPublicKey(createWeIdDataResult.getUserWeIdPublicKey().getPublicKey());
                setPublicKeyArgs.setType("secp256k1");
                WeIdPrivateKey weIdPrivateKey = new WeIdPrivateKey();
                weIdPrivateKey
                    .setPrivateKey(createWeIdDataResult.getUserWeIdPrivateKey().getPrivateKey());
                setPublicKeyArgs.setUserWeIdPrivateKey(weIdPrivateKey);
                ResponseData<Boolean> responseSetPub = weIdService.setPublicKey(setPublicKeyArgs);

                // set authentication
                SetAuthenticationArgs setAuthenticationArgs = new SetAuthenticationArgs();
                setAuthenticationArgs.setWeId(createWeIdDataResult.getWeId());
                setAuthenticationArgs
                    .setPublicKey(createWeIdDataResult.getUserWeIdPublicKey().getPublicKey());
                setAuthenticationArgs.setUserWeIdPrivateKey(weIdPrivateKey);
                ResponseData<Boolean> responseSetAuth = weIdService
                    .setAuthentication(setAuthenticationArgs);

                return new HttpResponseData<>(createWeIdDataResult.getWeId(),
                    responseSetAuth.getErrorCode(), responseSetAuth.getErrorMessage());
            } else {
                return new HttpResponseData<>(null, response.getErrorCode(),
                    response.getErrorMessage());
            }
        } catch (Exception e) {
            logger.error(
                "[getWeIdDocumentJson]: unknow error. weId:{}.",
                createWeIdJsonArgs,
                e);
            return new HttpResponseData<>(new HashMap<>(), HttpReturnCode.WEID_SDK_ERROR.getCode(),
                HttpReturnCode.WEID_SDK_ERROR.getCodeDesc().concat(e.getMessage()));
        }
    }

    @Override
    public HttpResponseData<Object> createWeIdWithPubKey(InputArg arg) {
        try {
            JsonNode publicKeyNode = new ObjectMapper()
                .readTree(arg.getFunctionArg())
                .get(ParamKeyConstant.PUBLIC_KEY);
            JsonNode txnArgNode = new ObjectMapper()
                .readTree(arg.getTransactionArg());
            JsonNode keyIndexNode = txnArgNode.get(WeIdentityParamKeyConstant.KEY_INDEX);
            if (publicKeyNode == null || StringUtils.isEmpty(publicKeyNode.textValue())
                || keyIndexNode == null || StringUtils.isEmpty(keyIndexNode.textValue())) {
                return new HttpResponseData<>(null, HttpReturnCode.INPUT_NULL);
            }
            String weId = WeIdUtils.convertPublicKeyToWeId(publicKeyNode.textValue());
            // todo
            // ResponseData<String> response = weIdService.getWeIdDocumentJson(publicKeyNode.textValue());
            return new HttpResponseData<>(weId, HttpReturnCode.SUCCESS);
        } catch (Exception e) {
            logger.error(
                "[getWeIdDocument]: unknow error. weId:{}.",
                arg,
                e);
            return new HttpResponseData<>(null, HttpReturnCode.WEID_SDK_ERROR.getCode(),
                HttpReturnCode.WEID_SDK_ERROR.getCodeDesc().concat(e.getMessage()));
        }
    }
}
