package com.webank.weid.http.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.webank.weid.http.constant.WeIdentityFunctionNames;
import com.webank.weid.http.constant.WeIdentityServiceEndpoint;
import com.webank.weid.http.protocol.request.ReqCreateCredentialArgs;
import com.webank.weid.http.protocol.request.ReqCredentialArgs;
import com.webank.weid.http.service.InvokerCredentialService;
import com.webank.weid.protocol.base.CredentialWrapper;
import com.webank.weid.protocol.response.ResponseData;

/**
 * Credential Controller.
 *
 * @author chaoxinhu and darwindu
 */

@RestController
@RequestMapping(value = WeIdentityServiceEndpoint.API_ROOT)
public class CredentialController {

    @Autowired
    private InvokerCredentialService invokerCredentialService;

    /**
     * Generate a credential.
     *
     * @param createCredentialJsonArgs the args in Json String. Keys should include: cptId, issuer,
     * claimData, expirationDate, signature (here the signature will be directly put into the
     * Credential's signature field, so be careful).
     * @return the credential json string response data. Selective disclosure is not supported.
     */
    @RequestMapping(value = WeIdentityFunctionNames.FUNCNAME_CREATE_CREDENTIAL, method = RequestMethod.POST)
    public ResponseData<CredentialWrapper> createCredential(
        @RequestBody ReqCreateCredentialArgs createCredentialJsonArgs) {
        return invokerCredentialService.createCredential(createCredentialJsonArgs);
    }

    /**
     * Verify the validity of a credential.
     *
     * @param verifyCredentialJsonArgs the args. Keys should include: credential json string.
     * @return the Boolean response data
     */
    @RequestMapping(value = WeIdentityFunctionNames.FUNCNAME_VERIFY_CREDENTIAL, method = RequestMethod.POST)
    public ResponseData<Boolean> verifyCredential(
        @RequestBody ReqCredentialArgs verifyCredentialJsonArgs) {
        return invokerCredentialService.verifyCredential(verifyCredentialJsonArgs);
    }
}
