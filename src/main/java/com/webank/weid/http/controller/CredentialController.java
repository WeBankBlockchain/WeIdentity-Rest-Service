package com.webank.weid.http.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.webank.weid.http.protocol.request.ReqCreateCredentialArgs;
import com.webank.weid.http.protocol.request.ReqCredentialArgs;
import com.webank.weid.http.protocol.request.ReqVerifyCredentialArgs;
import com.webank.weid.http.service.InvokerCredentialService;
import com.webank.weid.protocol.base.Credential;
import com.webank.weid.protocol.response.ResponseData;

@RestController
@RequestMapping(value = "weIdentity")
public class CredentialController {

    private Logger logger = LoggerFactory.getLogger(CredentialController.class);
    @Autowired
    private InvokerCredentialService invokerCredentialService;

    /**
     * Generate a credential.
     * @param reqCreateCredentialArgs the args
     * @return the Credential response data
     */
    @RequestMapping(value = "createCredential", method = RequestMethod.POST)
    public ResponseData<Credential> createCredential(
        @RequestBody ReqCreateCredentialArgs reqCreateCredentialArgs) {
        return invokerCredentialService.createCredential(reqCreateCredentialArgs);
    }

    /**
     * Verify the validity of a credential without public key provided.
     * @param reqCredentialArgs the args
     * @return the Boolean response data
     */
    @RequestMapping(value = "verifyCredential", method = RequestMethod.POST)
    public ResponseData<Boolean> verifyCredential(@RequestBody ReqCredentialArgs reqCredentialArgs) {
        return invokerCredentialService.verifyCredential(reqCredentialArgs);
    }

    /**
     * Verify the validity of a credential with public key provided.
     * @param reqVerifyCredentialArgs the args
     * @return the Boolean response data
     */
    @RequestMapping(value = "verifyCredentialWithSpecifiedPubKey", method = RequestMethod.POST)
    public ResponseData<Boolean> verifyCredentialWithSpecifiedPubKey(
        @RequestBody ReqVerifyCredentialArgs reqVerifyCredentialArgs) {
        return invokerCredentialService.verifyCredentialWithSpecifiedPubKey(reqVerifyCredentialArgs);
    }

}
