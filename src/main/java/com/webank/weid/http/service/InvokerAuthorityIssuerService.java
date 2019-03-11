package com.webank.weid.http.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.webank.weid.constant.ErrorCode;
import com.webank.weid.constant.ParamKeyConstant;
import com.webank.weid.http.constant.HttpReturnCode;
import com.webank.weid.http.protocol.request.ReqRegisterAuthorityIssuerArgs;
import com.webank.weid.http.protocol.request.ReqRemoveAuthorityIssuerArgs;
import com.webank.weid.http.protocol.response.HttpResponseData;
import com.webank.weid.protocol.base.AuthorityIssuer;
import com.webank.weid.protocol.base.WeIdPrivateKey;
import com.webank.weid.protocol.request.RegisterAuthorityIssuerArgs;
import com.webank.weid.protocol.request.RemoveAuthorityIssuerArgs;
import com.webank.weid.protocol.response.ResponseData;
import com.webank.weid.rpc.AuthorityIssuerService;
import com.webank.weid.service.impl.AuthorityIssuerServiceImpl;
import com.webank.weid.util.JsonUtil;

@Service
public class InvokerAuthorityIssuerService extends BaseService {

    private Logger logger = LoggerFactory.getLogger(InvokerAuthorityIssuerService.class);

    private AuthorityIssuerService authorityIssuerService = new AuthorityIssuerServiceImpl();

    /**
     * Register a new Authority Issuer on Chain.
     *
     * @param reqRegisterAuthorityIssuerArgs the args
     * @return the Boolean response data
     */
    public ResponseData<Boolean> registerAuthorityIssuer(
        ReqRegisterAuthorityIssuerArgs reqRegisterAuthorityIssuerArgs) {

        ResponseData<Boolean> response = new ResponseData<Boolean>();
        try {
            WeIdPrivateKey weIdPrivateKey = new WeIdPrivateKey();
            weIdPrivateKey.setPrivateKey(reqRegisterAuthorityIssuerArgs.getWeIdPrivateKey());

            AuthorityIssuer authorityIssuer = new AuthorityIssuer();
            authorityIssuer.setAccValue(reqRegisterAuthorityIssuerArgs.getAccValue());
            authorityIssuer.setCreated(reqRegisterAuthorityIssuerArgs.getCreated());
            authorityIssuer.setName(reqRegisterAuthorityIssuerArgs.getName());
            authorityIssuer.setWeId(reqRegisterAuthorityIssuerArgs.getWeId());

            RegisterAuthorityIssuerArgs registerAuthorityIssuerArgs = new RegisterAuthorityIssuerArgs();
            registerAuthorityIssuerArgs.setWeIdPrivateKey(weIdPrivateKey);
            registerAuthorityIssuerArgs.setAuthorityIssuer(authorityIssuer);
            response = authorityIssuerService.registerAuthorityIssuer(registerAuthorityIssuerArgs);

        } catch (Exception e) {
            logger.error("[registerAuthorityIssuer]: unknow error. registerAuthorityIssuerArgs:{}",
                reqRegisterAuthorityIssuerArgs,
                e);
            return new ResponseData<>(false, ErrorCode.BASE_ERROR);
        }
        return response;
    }

    /**
     * Call to WeID SDK with direct transaction hex String, to register AuthorityIssuer.
     *
     * @param transactionHex the transactionHex value
     * @return String in ResponseData
     */
    public HttpResponseData<String> registerAuthorityIssuerWithTransactionHex(
        String transactionHex) {
        try {
            ResponseData<String> responseData = authorityIssuerService
                .registerAuthorityIssuer(transactionHex);
            if (responseData.getErrorCode() != ErrorCode.SUCCESS.getCode()) {
                logger.error("[registerCpt]: error occurred: {}, {}", responseData.getErrorCode(),
                    responseData.getErrorMessage());
            }
            return new HttpResponseData<>(responseData.getResult(), responseData.getErrorCode(),
                responseData.getErrorMessage());
        } catch (Exception e) {
            logger.error("[registerAuthorityIssuer]: unknown error, input arguments:{}",
                transactionHex,
                e);
            return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.UNKNOWN_ERROR);
        }
    }

    /**
     * Remove a new Authority Issuer on Chain.
     *
     * @param reqRemoveAuthorityIssuerArgs the args
     * @return the Boolean response data
     */
    public ResponseData<Boolean> removeAuthorityIssuer(
        ReqRemoveAuthorityIssuerArgs reqRemoveAuthorityIssuerArgs) {

        ResponseData<Boolean> response = new ResponseData<Boolean>();
        try {
            WeIdPrivateKey weIdPrivateKey = new WeIdPrivateKey();
            weIdPrivateKey.setPrivateKey(reqRemoveAuthorityIssuerArgs.getWeIdPrivateKey());

            RemoveAuthorityIssuerArgs removeAuthorityIssuerArgs = new RemoveAuthorityIssuerArgs();
            removeAuthorityIssuerArgs.setWeId(reqRemoveAuthorityIssuerArgs.getWeId());
            removeAuthorityIssuerArgs.setWeIdPrivateKey(weIdPrivateKey);

            response = authorityIssuerService.removeAuthorityIssuer(removeAuthorityIssuerArgs);
        } catch (Exception e) {
            logger.error("[removeAuthorityIssuer]: unknow error. reqRemoveAuthorityIssuerArgs:{}",
                reqRemoveAuthorityIssuerArgs,
                e);
            return new ResponseData<>(false, ErrorCode.BASE_ERROR);
        }
        return response;
    }

    /**
     * Check whether the given weId is an authority issuer.
     *
     * @param weId the WeIdentity DID
     * @return the Boolean response data
     */
    public ResponseData<Boolean> isAuthorityIssuer(String weId) {

        ResponseData<Boolean> response = new ResponseData<Boolean>();
        try {
            response = authorityIssuerService.isAuthorityIssuer(weId);
        } catch (Exception e) {
            logger.error("[isAuthorityIssuer]: unknow error. weId:{}",
                weId,
                e);
            return new ResponseData<>(false, ErrorCode.BASE_ERROR);
        }
        return response;
    }

    /**
     * Query the authority issuer information given weId.
     *
     * @param weId the WeIdentity DID
     * @return the AuthorityIssuer response data
     */
    public ResponseData<AuthorityIssuer> queryAuthorityIssuerInfo(String weId) {

        ResponseData<AuthorityIssuer> response = new ResponseData<AuthorityIssuer>();
        try {
            response = authorityIssuerService.queryAuthorityIssuerInfo(weId);
        } catch (Exception e) {
            logger.error("[queryAuthorityIssuerInfo]: unknow error. weId:{}",
                weId,
                e);
            return new ResponseData<>(null, ErrorCode.BASE_ERROR);
        }
        return response;
    }

    /**
     * Query Authority Issuer via the InvokeFunction API.
     *
     * @param queryArgs the query WeID
     * @return the authorityIssuer
     */
    public HttpResponseData<String> queryAuthorityIssuerInfoInvoke(String queryArgs) {
        try {
            JsonNode weIdNode = new ObjectMapper().readTree(queryArgs)
                .get(ParamKeyConstant.WEID);
            if (weIdNode == null || StringUtils.isEmpty(weIdNode.textValue())) {
                return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.INPUT_NULL);
            }
            ResponseData response = authorityIssuerService
                .queryAuthorityIssuerInfo(weIdNode.textValue());
            return new HttpResponseData<>(
                JsonUtil.objToJsonStr(response.getResult()),
                response.getErrorCode(),
                response.getErrorMessage());
        } catch (Exception e) {
            logger.error(
                "[queryAuthorityIssuer]: unknow error. weId:{}.",
                queryArgs,
                e);
            return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.UNKNOWN_ERROR);
        }
    }
}
