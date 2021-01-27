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

import com.webank.weid.constant.WeIdConstant.PublicKeyType;
import com.webank.weid.exception.InitWeb3jException;
import com.webank.weid.exception.LoadContractException;
import com.webank.weid.http.constant.WeIdentityParamKeyConstant;
import com.webank.weid.http.protocol.response.WeIdListRsp;
import com.webank.weid.protocol.base.PublicKeyProperty;
import com.webank.weid.protocol.base.WeIdAuthentication;
import com.webank.weid.protocol.base.WeIdDocument;
import com.webank.weid.protocol.base.WeIdPublicKey;
import com.webank.weid.protocol.request.PublicKeyArgs;
import com.webank.weid.util.DataToolUtils;
import com.webank.weid.util.WeIdUtils;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.fisco.bcos.web3j.utils.Numeric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.webank.weid.constant.ErrorCode;
import com.webank.weid.constant.ParamKeyConstant;
import com.webank.weid.http.constant.HttpReturnCode;
import com.webank.weid.http.protocol.request.InputArg;
import com.webank.weid.http.protocol.response.HttpResponseData;
import com.webank.weid.http.service.BaseService;
import com.webank.weid.http.service.InvokerWeIdService;
import com.webank.weid.http.util.JsonUtil;
import com.webank.weid.http.util.KeyUtil;
import com.webank.weid.protocol.base.WeIdPrivateKey;
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
        } catch (LoadContractException e) {
            return new ResponseData<>(null, HttpReturnCode.CONTRACT_ERROR.getCode(), HttpReturnCode.CONTRACT_ERROR.getCodeDesc());
        } catch (InitWeb3jException e) {
            return new ResponseData<>(null, HttpReturnCode.WEB3J_ERROR.getCode(), HttpReturnCode.WEB3J_ERROR.getCodeDesc());
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
        } catch (LoadContractException e) {
            return new ResponseData<>(false, HttpReturnCode.CONTRACT_ERROR.getCode(), HttpReturnCode.CONTRACT_ERROR.getCodeDesc());
        } catch (InitWeb3jException e) {
            return new ResponseData<>(false, HttpReturnCode.WEB3J_ERROR.getCode(), HttpReturnCode.WEB3J_ERROR.getCodeDesc());
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
        } catch (LoadContractException e) {
            return new HttpResponseData<>(null, HttpReturnCode.CONTRACT_ERROR.getCode(), HttpReturnCode.CONTRACT_ERROR.getCodeDesc());
        } catch (InitWeb3jException e) {
            return new HttpResponseData<>(null, HttpReturnCode.WEB3J_ERROR.getCode(), HttpReturnCode.WEB3J_ERROR.getCodeDesc());
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
            return getWeIdDocumentJsonInvoke(weIdNode.textValue());
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
     * Get a WeIdentity DID Document Json via the InvokeFunction API.
     *
     * @param weId the WeIdentity DID
     * @return the WeIdentity DID document json
     */
    private HttpResponseData<Object> getWeIdDocumentJsonInvoke(String weId) {
        try {
            ResponseData<WeIdDocument> response = weIdService.getWeIdDocument(weId);
            if (response.getResult() == null) {
                return new HttpResponseData<>(null, response.getErrorCode(), response.getErrorMessage());
            }
            WeIdDocument weIdDocument = response.getResult();
            List<PublicKeyProperty> publicKeyProperties = new ArrayList<>();
            for (PublicKeyProperty publicKeyProperty : weIdDocument.getPublicKey()) {
                if (publicKeyProperty.getType().equalsIgnoreCase(PublicKeyType.SECP256K1.getTypeName())) {
                    publicKeyProperty.setPublicKey(Base64.encodeBase64String(Numeric.
                        hexStringToByteArray(new BigInteger(publicKeyProperty
                        .getPublicKey(), 10).toString(16))));
                    publicKeyProperties.add(publicKeyProperty);
                }
                if (publicKeyProperty.getType().equalsIgnoreCase(PublicKeyType.RSA.getTypeName())) {
                    publicKeyProperty.setPublicKey(Base64.encodeBase64String(Numeric
                        .hexStringToByteArray(publicKeyProperty.getPublicKey())));
                    publicKeyProperties.add(publicKeyProperty);
                }
            }
            weIdDocument.setPublicKey(publicKeyProperties);
            String weIdDocumentStr;
            try {
                weIdDocumentStr = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(weIdDocument);
            } catch (Exception var7) {
                logger.error("write object to String fail.", var7);
                return new HttpResponseData<>(null, HttpReturnCode.UNKNOWN_ERROR);
            }
            weIdDocumentStr = (new StringBuffer()).append(weIdDocumentStr)
                .insert(1, "\"@context\" : \"https://github.com/WeBankFinTech/WeIdentity/blob/master/context/v1\",").toString();
            return new HttpResponseData<>(
                JsonUtil.convertJsonToSortedMap(weIdDocumentStr),
                response.getErrorCode(),
                response.getErrorMessage());
        } catch (LoadContractException e) {
            return new HttpResponseData<>(null, HttpReturnCode.CONTRACT_ERROR.getCode(), HttpReturnCode.CONTRACT_ERROR.getCodeDesc());
        } catch (InitWeb3jException e) {
            return new HttpResponseData<>(null, HttpReturnCode.WEB3J_ERROR.getCode(), HttpReturnCode.WEB3J_ERROR.getCodeDesc());
        } catch (Exception e) {
            logger.error(
                "[getWeIdDocument]: unknow error. weId:{}.",
                weId,
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
                    // host the weId which just got crekated
                    KeyUtil.savePrivateKey(KeyUtil.SDK_PRIVKEY_PATH,
                        createWeIdDataResult.getWeId(),
                        createWeIdDataResult.getUserWeIdPrivateKey().getPrivateKey());
                } catch (Exception e) {
                    return new HttpResponseData<>(null, HttpReturnCode.INVOKER_ILLEGAL);
                }

                return new HttpResponseData<>(createWeIdDataResult.getWeId(),
                		response.getErrorCode(), response.getErrorMessage());
            } else {
                return new HttpResponseData<>(null, response.getErrorCode(),
                    response.getErrorMessage());
            }
        } catch (LoadContractException e) {
            return new HttpResponseData<>(null, HttpReturnCode.CONTRACT_ERROR.getCode(), HttpReturnCode.CONTRACT_ERROR.getCodeDesc());
        } catch (InitWeb3jException e) {
            return new HttpResponseData<>(null, HttpReturnCode.WEB3J_ERROR.getCode(), HttpReturnCode.WEB3J_ERROR.getCodeDesc());
        } catch (Exception e) {
            logger.error(
                "[CreateWeID]: unknow error. weId:{}.",
                createWeIdJsonArgs,
                e);
            return new HttpResponseData<>(new HashMap<>(), HttpReturnCode.WEID_SDK_ERROR.getCode(),
                HttpReturnCode.WEID_SDK_ERROR.getCodeDesc().concat(e.getMessage()));
        }
    }

    /**
     * Create WeId and get the Document via the InvokeFunction API.
     *
     * @param createWeIdJsonArgs the input args, should be almost null
     * @return tthe WeIdentity DID Document
     */
    public HttpResponseData<Object> createWeIdInvoke2(InputArg createWeIdJsonArgs) {
        HttpResponseData<Object> createWeIdInvoke = createWeIdInvoke(createWeIdJsonArgs);
        if (createWeIdInvoke.getRespBody() == null || StringUtils.isBlank(createWeIdInvoke.getRespBody().toString())) {
            return createWeIdInvoke;
        }
        return getWeIdDocumentJsonInvoke(createWeIdInvoke.getRespBody().toString());
    }

    @Override
    public HttpResponseData<Object> createWeIdWithPubKey(InputArg arg) {
        try {
            JsonNode publicKeySecpNode = new ObjectMapper()
                .readTree(arg.getFunctionArg())
                .get(WeIdentityParamKeyConstant.PUBKEY_SECP);
            JsonNode txnArgNode = new ObjectMapper()
                .readTree(arg.getTransactionArg());
            JsonNode keyIndexNode = txnArgNode.get(WeIdentityParamKeyConstant.KEY_INDEX);
            if (publicKeySecpNode == null || StringUtils.isEmpty(publicKeySecpNode.textValue())
                || keyIndexNode == null || StringUtils.isEmpty(keyIndexNode.textValue())) {
                return new HttpResponseData<>(null, HttpReturnCode.INPUT_NULL);
            }
            if (!DataToolUtils.isValidBase64String(publicKeySecpNode.textValue())) {
                logger.error("Public key secp256k1 format illegal: not Base64 encoded.");
                return new HttpResponseData<>(null, HttpReturnCode.INPUT_ILLEGAL.getCode(),
                    HttpReturnCode.INPUT_ILLEGAL.getCodeDesc() + ": not Base64");
            }
//            if (!KeyUtil.isPubkeyBytesValid(Base64.decodeBase64(publicKeySecpNode.textValue()))) {
//                return new HttpResponseData<>(null, HttpReturnCode.INPUT_ILLEGAL.getCode(),
//                    HttpReturnCode.INPUT_ILLEGAL.getCodeDesc() + ": public key security risk");
//            }
            String publicKeySecp = Numeric.toBigInt(Base64.decodeBase64(publicKeySecpNode
                .textValue())).toString(10);
            String weId = WeIdUtils.convertPublicKeyToWeId(publicKeySecp);
            WeIdPublicKey weIdPublicKey = new WeIdPublicKey();
            weIdPublicKey.setPublicKey(publicKeySecp);
            WeIdAuthentication weIdAuthentication = new WeIdAuthentication();
            weIdAuthentication.setWeId(weId);
            String privateKey = KeyUtil
                .getPrivateKeyByWeId(KeyUtil.SDK_PRIVKEY_PATH, keyIndexNode.textValue());
            if (!KeyUtil.isPrivateKeyLengthValid(privateKey)) {
                return new HttpResponseData<>(null, HttpReturnCode.INVOKER_ILLEGAL);
            }
            WeIdPrivateKey weIdPrivateKey = new WeIdPrivateKey();
            weIdPrivateKey.setPrivateKey(privateKey);
            weIdAuthentication.setWeIdPrivateKey(weIdPrivateKey);
            ResponseData<String> response = weIdService.delegateCreateWeId(weIdPublicKey, weIdAuthentication);
            // after success:
            if (!StringUtils.isEmpty(response.getResult())) {
                KeyUtil.savePrivateKey(KeyUtil.SDK_PRIVKEY_PATH,
                    weId,
                    StringUtils.EMPTY);
            } else {
                return new HttpResponseData<>(StringUtils.EMPTY, response.getErrorCode(), response.getErrorMessage());
            }

            // Proceed on RSA public key
            JsonNode publicKeyRsaNode;
            String publicKeyRsa;
            try {
                publicKeyRsaNode = new ObjectMapper()
                    .readTree(arg.getFunctionArg())
                    .get(WeIdentityParamKeyConstant.PUBKEY_RSA);
                publicKeyRsa = publicKeyRsaNode.textValue();
                if (!DataToolUtils.isValidBase64String(publicKeyRsa)) {
                    logger.info("Public key RSA secp256k1 format illegal: not Base64 encoded.");
                    return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.INPUT_ILLEGAL.getCode(),
                        HttpReturnCode.INPUT_ILLEGAL.getCodeDesc() + ": RSA not base64");
                }
            } catch (Exception e) {
                logger.info("Cannot find RSA public key, skipping..");
                return new HttpResponseData<>(weId, HttpReturnCode.SUCCESS);
            }
            PublicKeyArgs publicKeyArgs = new PublicKeyArgs();
            publicKeyArgs.setOwner(weId);
            publicKeyArgs.setPublicKey(Numeric.toHexStringNoPrefix(Base64.decodeBase64(publicKeyRsa)));
            publicKeyArgs.setType(PublicKeyType.RSA);
            ResponseData<Integer> resp = weIdService
                .delegateAddPublicKey(weId, publicKeyArgs, weIdAuthentication.getWeIdPrivateKey());
            if (resp.getResult() < 0) {
                return new HttpResponseData<>(StringUtils.EMPTY, resp.getErrorCode(), resp.getErrorMessage());
            }
            return new HttpResponseData<>(weId, HttpReturnCode.SUCCESS);
        } catch (LoadContractException e) {
            return new HttpResponseData<>(null, HttpReturnCode.CONTRACT_ERROR.getCode(), HttpReturnCode.CONTRACT_ERROR.getCodeDesc());
        } catch (InitWeb3jException e) {
            return new HttpResponseData<>(null, HttpReturnCode.WEB3J_ERROR.getCode(), HttpReturnCode.WEB3J_ERROR.getCodeDesc());
        } catch (Exception e) {
            logger.error(
                "[CreateWeID]: unknow error. weId:{}.",
                arg,
                e);
            return new HttpResponseData<>(null, HttpReturnCode.WEID_SDK_ERROR.getCode(),
                HttpReturnCode.WEID_SDK_ERROR.getCodeDesc().concat(e.getMessage()));
        }
    }
    
    /**
     * Create WeId and get the Document via the InvokeFunction API.
     *
     * @param  arg the input args, should be almost null
     * @return the WeIdentity DID Document
     */
    public HttpResponseData<Object> createWeIdWithPubKey2(InputArg arg) {
        HttpResponseData<Object> createWeIdInvoke = createWeIdWithPubKey(arg);
        if (createWeIdInvoke.getRespBody() == null || StringUtils.isBlank(createWeIdInvoke.getRespBody().toString())) {
            return createWeIdInvoke;
        }
        return getWeIdDocumentJsonInvoke(createWeIdInvoke.getRespBody().toString());
    }

    /**
     * GET WeIdList by PublicKeyList.
     *
     * @param  arg the input args, should be almost null
     * @return the WeIdentity DID List
     * @throws Exception exception
     */
    public HttpResponseData<Object> getWeIdListByPubKeyList(InputArg arg) throws Exception {
        WeIdListRsp weIdListRsp = new WeIdListRsp();
        weIdListRsp.setWeIdList(new ArrayList<>());
        weIdListRsp.setErrorCodeList(new ArrayList<>());

        JsonNode functionArgNode = new ObjectMapper().readTree(arg.getFunctionArg());
        JsonNode pubKeyNode = functionArgNode.get(WeIdentityParamKeyConstant.PUBKEY_LIST);
        if (!pubKeyNode.isArray()) {
            logger.error("input format illegal: not Array.");
            return new HttpResponseData<>(weIdListRsp, HttpReturnCode.INPUT_ILLEGAL.getCode(),
                HttpReturnCode.INPUT_ILLEGAL.getCodeDesc() + ": not Array");
        }
        List<WeIdPublicKey> pubKeyList = new ArrayList<>();
        for (JsonNode jsonNode : pubKeyNode) {
            if (StringUtils.isBlank(jsonNode.asText())) {
                logger.error("public key is null.");
                return new HttpResponseData<>(weIdListRsp, HttpReturnCode.INPUT_ILLEGAL.getCode(),
                        HttpReturnCode.INPUT_ILLEGAL.getCodeDesc() + ": public key is null");
            }
            if (!DataToolUtils.isValidBase64String(jsonNode.asText())) {
                logger.error("Public key secp256k1 format illegal: not Base64 encoded.");
                return new HttpResponseData<>(weIdListRsp, HttpReturnCode.INPUT_ILLEGAL.getCode(),
                    HttpReturnCode.INPUT_ILLEGAL.getCodeDesc() + ": not Base64");
            }
            String publicKeySecp = Numeric.toBigInt(Base64.decodeBase64(jsonNode.asText())).toString(10);
            WeIdPublicKey publicKey = new WeIdPublicKey();
            publicKey.setPublicKey(publicKeySecp);
            pubKeyList.add(publicKey);
        }
        if (pubKeyList.isEmpty()) {
            return new HttpResponseData<>(weIdListRsp, HttpReturnCode.INPUT_ILLEGAL);
        }
        return this.getWeIdListAndErrorCodeList(pubKeyList, weIdListRsp);
    }

    private HttpResponseData<Object> getWeIdListAndErrorCodeList(List<WeIdPublicKey> pubKeyList, WeIdListRsp weIdListRsp) {
        HttpResponseData<Object> responseData = new HttpResponseData<>();
        pubKeyList.forEach(weIdPublicKey -> {
            String weId = WeIdUtils.convertPublicKeyToWeId(weIdPublicKey.getPublicKey());
            if (StringUtils.isBlank(weId)) {
                weIdListRsp.getWeIdList().add(null);
                weIdListRsp.getErrorCodeList().add(HttpReturnCode.CONVERT_PUBKEY_TO_WEID_ERROR.getCode());
            } else {
                if (weIdService.isWeIdExist(weId).getResult()) {
                    weIdListRsp.getWeIdList().add(weId);
                    weIdListRsp.getErrorCodeList().add(ErrorCode.SUCCESS.getCode());
                } else {
                    weIdListRsp.getWeIdList().add(null);
                    weIdListRsp.getErrorCodeList().add(ErrorCode.WEID_PUBLIC_KEY_NOT_EXIST.getCode());
                }
            }
        });

        responseData.setRespBody(weIdListRsp);

        if (weIdListRsp.getWeIdList().contains(null)) {
            responseData.setErrorCode(HttpReturnCode.GET_WEID_LIST_BY_PUBKEY_LIST_ERROR);
        }
        return responseData;
    }


}
