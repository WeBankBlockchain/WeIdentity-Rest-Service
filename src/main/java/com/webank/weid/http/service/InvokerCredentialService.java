package com.webank.weid.http.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.webank.weid.http.constant.HttpErrorCode;
import com.webank.weid.http.protocol.request.ReqCreateCredentialArgs;
import com.webank.weid.http.protocol.request.ReqCredentialArgs;
import com.webank.weid.http.protocol.request.ReqVerifyCredentialArgs;
import com.webank.weid.protocol.base.Credential;
import com.webank.weid.protocol.base.WeIdPrivateKey;
import com.webank.weid.protocol.base.WeIdPublicKey;
import com.webank.weid.protocol.request.CreateCredentialArgs;
import com.webank.weid.protocol.request.VerifyCredentialArgs;
import com.webank.weid.protocol.response.ResponseData;
import com.webank.weid.rpc.CredentialService;

@Service
public class InvokerCredentialService {

    private Logger logger = LoggerFactory.getLogger(InvokerCredentialService.class);

    @Autowired
    private CredentialService credentialService;

    /**
     * Generate a credential.
     * @param reqCreateCredentialArgs the args
     * @return the Credential response data
     */
    public ResponseData<Credential> createCredential(ReqCreateCredentialArgs reqCreateCredentialArgs) {

        ResponseData<Credential> response = new ResponseData<Credential>();
        try {
            WeIdPrivateKey weIdPrivateKey = new WeIdPrivateKey();
            weIdPrivateKey.setPrivateKey(reqCreateCredentialArgs.getWeIdPrivateKey());

            CreateCredentialArgs createCredentialArgs = new CreateCredentialArgs();
            createCredentialArgs.setCptId(reqCreateCredentialArgs.getCptId());
            createCredentialArgs.setIssuer(reqCreateCredentialArgs.getIssuer());
            createCredentialArgs.setExpirationDate(reqCreateCredentialArgs.getExpirationDate());
            createCredentialArgs.setWeIdPrivateKey(weIdPrivateKey);
            createCredentialArgs.setClaim(reqCreateCredentialArgs.getClaim());

            response = credentialService.createCredential(createCredentialArgs);
        } catch (Exception e) {
            logger.error("[createCredential]: unknow error. reqCreateCredentialArgs:{}",
                reqCreateCredentialArgs,
                e);
            response.setErrorCode(HttpErrorCode.UNKNOW_ERROR.getCode());
            response.setErrorMessage(HttpErrorCode.UNKNOW_ERROR.getCodeDesc());
        }
        return response;
    }

    /**
     * Verify the validity of a credential without public key provided.
     * @param reqCredentialArgs the args
     * @return the Boolean response data
     */
    public ResponseData<Boolean> verifyCredential(ReqCredentialArgs reqCredentialArgs) {

        ResponseData<Boolean> response = new ResponseData<Boolean>();
        try {

            Credential credential = new Credential();
            credential.setContext(reqCredentialArgs.getContext());
            credential.setId(reqCredentialArgs.getId());
            credential.setCptId(reqCredentialArgs.getCptId());
            credential.setExpirationDate(reqCredentialArgs.getExpirationDate());
            credential.setIssuranceDate(reqCredentialArgs.getIssuranceDate());
            credential.setIssuer(reqCredentialArgs.getIssuer());
            credential.setSignature(reqCredentialArgs.getSignature());
            credential.setClaim(reqCredentialArgs.getClaim());

            response = credentialService.verifyCredential(credential);
        } catch (Exception e) {
            logger.error("[verifyCredential]: unknow error. reqCredentialArgs:{}",
                reqCredentialArgs,
                e);
            response.setErrorCode(HttpErrorCode.UNKNOW_ERROR.getCode());
            response.setErrorMessage(HttpErrorCode.UNKNOW_ERROR.getCodeDesc());
        }
        return response;
    }

    /**
     * Verify the validity of a credential with public key provided.
     * @param reqVerifyCredentialArgs the args
     * @return the Boolean response data
     */
    public ResponseData<Boolean> verifyCredentialWithSpecifiedPubKey(
        ReqVerifyCredentialArgs reqVerifyCredentialArgs) {

        ResponseData<Boolean> response = new ResponseData<Boolean>();
        try {
            Credential credential = new Credential();
            credential.setContext(reqVerifyCredentialArgs.getContext());
            credential.setId(reqVerifyCredentialArgs.getId());
            credential.setCptId(reqVerifyCredentialArgs.getCptId());
            credential.setExpirationDate(reqVerifyCredentialArgs.getExpirationDate());
            credential.setIssuranceDate(reqVerifyCredentialArgs.getIssuranceDate());
            credential.setIssuer(reqVerifyCredentialArgs.getIssuer());
            credential.setSignature(reqVerifyCredentialArgs.getSignature());
            credential.setClaim(reqVerifyCredentialArgs.getClaim());

            VerifyCredentialArgs verifyCredentialArgs = new VerifyCredentialArgs();
            verifyCredentialArgs.setCredential(credential);
            WeIdPublicKey weIdPublicKey = new WeIdPublicKey();
            weIdPublicKey.setPublicKey(reqVerifyCredentialArgs.getWeIdPublicKey());
            verifyCredentialArgs.setWeIdPublicKey(weIdPublicKey);
            response = credentialService.verifyCredentialWithSpecifiedPubKey(verifyCredentialArgs);
        } catch (Exception e) {
            logger.error(
                "[verifyCredentialWithSpecifiedPubKey]: unknow error. reqVerifyCredentialArgs:{}",
                reqVerifyCredentialArgs,
                e);
            response.setErrorCode(HttpErrorCode.UNKNOW_ERROR.getCode());
            response.setErrorMessage(HttpErrorCode.UNKNOW_ERROR.getCodeDesc());
        }
        return response;
    }
}
