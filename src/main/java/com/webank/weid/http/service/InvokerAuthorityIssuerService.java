package com.webank.weid.http.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.webank.weid.http.constant.HttpErrorCode;
import com.webank.weid.http.protocol.request.ReqRegisterAuthorityIssuerArgs;
import com.webank.weid.http.protocol.request.ReqRemoveAuthorityIssuerArgs;
import com.webank.weid.protocol.base.AuthorityIssuer;
import com.webank.weid.protocol.base.WeIdPrivateKey;
import com.webank.weid.protocol.request.RegisterAuthorityIssuerArgs;
import com.webank.weid.protocol.request.RemoveAuthorityIssuerArgs;
import com.webank.weid.protocol.response.ResponseData;
import com.webank.weid.rpc.AuthorityIssuerService;

@Service
public class InvokerAuthorityIssuerService {

    private Logger logger = LoggerFactory.getLogger(InvokerAuthorityIssuerService.class);

    @Autowired
    private AuthorityIssuerService authorityIssuerService;

    /**
     * Register a new Authority Issuer on Chain.
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
            response.setErrorCode(HttpErrorCode.UNKNOW_ERROR.getCode());
            response.setErrorMessage(HttpErrorCode.UNKNOW_ERROR.getCodeDesc());
        }
        return response;
    }

    /**
     * Remove a new Authority Issuer on Chain.
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
            response.setErrorCode(HttpErrorCode.UNKNOW_ERROR.getCode());
            response.setErrorMessage(HttpErrorCode.UNKNOW_ERROR.getCodeDesc());
        }
        return response;
    }

    /**
     * Check whether the given weId is an authority issuer.
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
            response.setErrorCode(HttpErrorCode.UNKNOW_ERROR.getCode());
            response.setErrorMessage(HttpErrorCode.UNKNOW_ERROR.getCodeDesc());
        }
        return response;
    }

    /**
     * Query the authority issuer information given weId.
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
            response.setErrorCode(HttpErrorCode.UNKNOW_ERROR.getCode());
            response.setErrorMessage(HttpErrorCode.UNKNOW_ERROR.getCodeDesc());
        }
        return response;
    }

}
