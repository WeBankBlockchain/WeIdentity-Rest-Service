package com.webank.weid.http.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.bcos.web3j.crypto.ECKeyPair;
import org.bcos.web3j.crypto.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.webank.weid.constant.ErrorCode;
import com.webank.weid.constant.ParamKeyConstant;
import com.webank.weid.http.constant.HttpReturnCode;
import com.webank.weid.http.protocol.request.ReqCreateWeIdArgs;
import com.webank.weid.http.protocol.request.ReqSetAuthenticationArgs;
import com.webank.weid.http.protocol.request.ReqSetPublicKeyArgs;
import com.webank.weid.http.protocol.request.ReqSetServiceArgs;
import com.webank.weid.http.protocol.response.HttpResponseData;
import com.webank.weid.protocol.base.WeIdDocument;
import com.webank.weid.protocol.base.WeIdPrivateKey;
import com.webank.weid.protocol.request.CreateWeIdArgs;
import com.webank.weid.protocol.request.SetAuthenticationArgs;
import com.webank.weid.protocol.request.SetPublicKeyArgs;
import com.webank.weid.protocol.request.SetServiceArgs;
import com.webank.weid.protocol.response.CreateWeIdDataResult;
import com.webank.weid.protocol.response.ResponseData;
import com.webank.weid.rpc.WeIdService;
import com.webank.weid.service.impl.WeIdServiceImpl;

@Service
public class InvokerWeIdService extends BaseService {

    private Logger logger = LoggerFactory.getLogger(InvokerWeIdService.class);

    private WeIdService weIdService = new WeIdServiceImpl();

    /**
     * createEcKeyPair.
     *
     * @return publicKey and privateKey
     */
    public ResponseData<CreateWeIdArgs> createEcKeyPair() {

        ResponseData<CreateWeIdArgs> response = new ResponseData<CreateWeIdArgs>();
        try {

            ECKeyPair keyPair = Keys.createEcKeyPair();
            String publicKey = String.valueOf(keyPair.getPublicKey());
            String privateKey = String.valueOf(keyPair.getPrivateKey());

            CreateWeIdArgs createWeIdArgs = new CreateWeIdArgs();
            createWeIdArgs.setPublicKey(publicKey);
            WeIdPrivateKey weIdPrivateKey = new WeIdPrivateKey();
            weIdPrivateKey.setPrivateKey(privateKey);
            createWeIdArgs.setWeIdPrivateKey(weIdPrivateKey);

            response.setResult(createWeIdArgs);
        } catch (Exception e) {
            logger.error("[createEcKeyPair]: unknow error, please check the error log.", e);
            return new ResponseData<>(null, ErrorCode.BASE_ERROR);
        }
        return response;
    }

    /**
     * Create WeIdentity DID
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
     * Create a WeIdentity DID.
     *
     * @param reqCreateWeIdArgs the create WeIdentity DID args
     * @return the response data
     */
    public ResponseData<String> createWeId(ReqCreateWeIdArgs reqCreateWeIdArgs) {

        ResponseData<String> response = new ResponseData<String>();
        try {
            WeIdPrivateKey weIdPrivateKey = new WeIdPrivateKey();
            weIdPrivateKey.setPrivateKey(reqCreateWeIdArgs.getWeIdPrivateKey());

            CreateWeIdArgs createWeIdArgs = new CreateWeIdArgs();
            createWeIdArgs.setPublicKey(reqCreateWeIdArgs.getPublicKey());
            createWeIdArgs.setWeIdPrivateKey(weIdPrivateKey);

            response = weIdService.createWeId(createWeIdArgs);
        } catch (Exception e) {
            logger.error(
                "[weIdService]: unknow error. reqCreateWeIdArgs:{}.",
                reqCreateWeIdArgs,
                e);
            return new ResponseData<>(StringUtils.EMPTY, ErrorCode.BASE_ERROR);
        }
        return response;
    }

    /**
     * Get a WeIdentity DID Document.
     *
     * @param weId the WeIdentity DID
     * @return the WeIdentity DID document
     */
    public ResponseData<WeIdDocument> getWeIdDocument(String weId) {

        ResponseData<WeIdDocument> response = new ResponseData<WeIdDocument>();
        try {
            response = weIdService.getWeIdDocument(weId);
        } catch (Exception e) {
            logger.error(
                "[getWeIdDocument]: unknow error. weId:{}.",
                weId,
                e);
            return new ResponseData<>(null, ErrorCode.BASE_ERROR);
        }
        return response;
    }

    /**
     * Get a WeIdentity DID Document Json.
     *
     * @param weId the WeIdentity DID
     * @return the WeIdentity DID document json
     */
    public ResponseData<String> getWeIdDocumentJson(String weId) {

        ResponseData<String> response = new ResponseData<String>();
        try {
            response = weIdService.getWeIdDocumentJson(weId);
        } catch (Exception e) {
            logger.error(
                "[getWeIdDocumentJson]: unknow error. weId:{}.",
                weId,
                e);
            return new ResponseData<>(StringUtils.EMPTY, ErrorCode.BASE_ERROR);
        }
        return response;
    }

    /**
     * Set Public Key.
     *
     * @param reqSetPublicKeyArgs the set public key args
     * @return the response data
     */
    public ResponseData<Boolean> setPublicKey(ReqSetPublicKeyArgs reqSetPublicKeyArgs) {

        ResponseData<Boolean> response = new ResponseData<Boolean>();
        try {
            WeIdPrivateKey weIdPrivateKey = new WeIdPrivateKey();
            weIdPrivateKey.setPrivateKey(reqSetPublicKeyArgs.getUserWeIdPrivateKey());

            SetPublicKeyArgs setPublicKeyArgs = new SetPublicKeyArgs();
            setPublicKeyArgs.setPublicKey(reqSetPublicKeyArgs.getPublicKey());
            setPublicKeyArgs.setOwner(reqSetPublicKeyArgs.getOwner());
            setPublicKeyArgs.setType(reqSetPublicKeyArgs.getType());
            setPublicKeyArgs.setWeId(reqSetPublicKeyArgs.getWeId());
            setPublicKeyArgs.setUserWeIdPrivateKey(weIdPrivateKey);

            response = weIdService.setPublicKey(setPublicKeyArgs);
        } catch (Exception e) {
            logger.error(
                "[setPublicKey]: unknow error. reqSetPublicKeyArgs:{}.",
                reqSetPublicKeyArgs,
                e);
            return new ResponseData<>(false, ErrorCode.BASE_ERROR);
        }
        return response;
    }

    /**
     * Set Service.
     *
     * @param reqSetServiceArgs the set service args
     * @return the response data
     */
    public ResponseData<Boolean> setService(ReqSetServiceArgs reqSetServiceArgs) {

        ResponseData<Boolean> response = new ResponseData<Boolean>();
        try {

            WeIdPrivateKey weIdPrivateKey = new WeIdPrivateKey();
            weIdPrivateKey.setPrivateKey(reqSetServiceArgs.getUserWeIdPrivateKey());

            SetServiceArgs setServiceArgs = new SetServiceArgs();
            setServiceArgs.setType(reqSetServiceArgs.getType());
            setServiceArgs.setWeId(reqSetServiceArgs.getWeId());
            setServiceArgs.setServiceEndpoint(reqSetServiceArgs.getServiceEndpoint());
            setServiceArgs.setUserWeIdPrivateKey(weIdPrivateKey);

            response = weIdService.setService(setServiceArgs);
        } catch (Exception e) {
            logger.error(
                "[setService]: unknow error. reqSetServiceArgs:{}.",
                reqSetServiceArgs,
                e);
            return new ResponseData<>(false, ErrorCode.BASE_ERROR);
        }
        return response;
    }

    /**
     * Set Authentication.
     *
     * @param reqSetAuthenticationArgs the set authentication args
     * @return the response data
     */
    public ResponseData<Boolean> setAuthentication(
        ReqSetAuthenticationArgs reqSetAuthenticationArgs) {

        ResponseData<Boolean> response = new ResponseData<Boolean>();
        try {
            WeIdPrivateKey weIdPrivateKey = new WeIdPrivateKey();
            weIdPrivateKey.setPrivateKey(reqSetAuthenticationArgs.getUserWeIdPrivateKey());

            SetAuthenticationArgs setAuthenticationArgs = new SetAuthenticationArgs();
            setAuthenticationArgs.setWeId(reqSetAuthenticationArgs.getWeId());
            setAuthenticationArgs.setOwner(reqSetAuthenticationArgs.getOwner());
            setAuthenticationArgs.setType(reqSetAuthenticationArgs.getType());
            setAuthenticationArgs.setPublicKey(reqSetAuthenticationArgs.getPublicKey());
            setAuthenticationArgs.setUserWeIdPrivateKey(weIdPrivateKey);

            response = weIdService.setAuthentication(setAuthenticationArgs);
        } catch (Exception e) {
            logger.error(
                "[setAuthentication]: unknow error. reqSetAuthenticationArgs:{}.",
                reqSetAuthenticationArgs,
                e);
            return new ResponseData<>(false, ErrorCode.BASE_ERROR);
        }
        return response;
    }

    /**
     * Check if WeIdentity DID exists on Chain.
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
            ResponseData<String> responseData = weIdService.createWeId(transactionHex);
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
            return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.UNKNOWN_ERROR);
        }
    }

    /**
     * Get a WeIdentity DID Document Json via the InvokeFunction API.
     *
     * @param getWeIdDocumentJsonArgs the WeIdentity DID
     * @return the WeIdentity DID document json
     */
    public HttpResponseData<String> getWeIdDocumentJsonInvoke(String getWeIdDocumentJsonArgs) {
        try {
            JsonNode weIdNode = new ObjectMapper().readTree(getWeIdDocumentJsonArgs)
                .get(ParamKeyConstant.WEID);
            if (weIdNode == null || StringUtils.isEmpty(weIdNode.textValue())) {
                return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.INPUT_NULL);
            }
            ResponseData response = weIdService.getWeIdDocumentJson(weIdNode.textValue());
            return new HttpResponseData<>(response.getResult().toString(), response.getErrorCode(),
                response.getErrorMessage());
        } catch (Exception e) {
            logger.error(
                "[getWeIdDocumentJson]: unknow error. weId:{}.",
                getWeIdDocumentJsonArgs,
                e);
            return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.UNKNOWN_ERROR);
        }
    }
}
