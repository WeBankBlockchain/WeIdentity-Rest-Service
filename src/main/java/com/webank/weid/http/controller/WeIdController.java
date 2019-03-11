package com.webank.weid.http.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.webank.weid.http.constant.WeIdentityServiceEndpoint;
import com.webank.weid.http.protocol.request.ReqCreateWeIdArgs;
import com.webank.weid.http.protocol.request.ReqSetAuthenticationArgs;
import com.webank.weid.http.protocol.request.ReqSetPublicKeyArgs;
import com.webank.weid.http.protocol.request.ReqSetServiceArgs;
import com.webank.weid.http.service.InvokerWeIdService;
import com.webank.weid.protocol.base.WeIdDocument;
import com.webank.weid.protocol.request.CreateWeIdArgs;
import com.webank.weid.protocol.response.CreateWeIdDataResult;
import com.webank.weid.protocol.response.ResponseData;

@RestController
@RequestMapping(value = WeIdentityServiceEndpoint.API_ROOT)
public class WeIdController {

    @Autowired
    private InvokerWeIdService invokerWeIdService;

    /**
     * @fixme
     * createEcKeyPair.
     * @return publicKey and privateKey
     */
    @RequestMapping(value = "createEcKeyPair", method = RequestMethod.GET)
    public ResponseData<CreateWeIdArgs> createEcKeyPair() {
        return invokerWeIdService.createEcKeyPair();
    }

    /**
     * @fixme
     * Create WeIdentity DID.
     * @return the response data
     */
    @RequestMapping(value = "createWeId", method = RequestMethod.GET)
    public ResponseData<CreateWeIdDataResult> createWeId() {
        return invokerWeIdService.createWeId();
    }

    /**
     * @fixme
     * Create a WeIdentity DID.
     * @param reqCreateWeIdArgs the create WeIdentity DID args
     * @return the response data
     */
    @RequestMapping(value = "createWeId", method = RequestMethod.POST)
    public ResponseData<String> createWeId(@RequestBody ReqCreateWeIdArgs reqCreateWeIdArgs) {
        return invokerWeIdService.createWeId(reqCreateWeIdArgs);
    }

    /**
     * @fixme
     * Get a WeIdentity DID Document.
     * @param weId response WeIdentity DID.
     * @return the WeIdentity DID document.
     */
    @RequestMapping(value = "getWeIdDocument/{weId}", method = RequestMethod.GET)
    public ResponseData<WeIdDocument> getWeIdDocument(@PathVariable("weId") String weId) {
        return invokerWeIdService.getWeIdDocument(weId);
    }

    /**
     * @fixme
     * Get a WeIdentity DID Document.
     * @param weId response WeIdentity DID.
     * @return the WeIdentity DID document json.
     */
    @RequestMapping(value = "getWeIdDocumentJson/{weId}", method = RequestMethod.GET)
    public ResponseData<String> getWeIdDocumentJson(@PathVariable("weId") String weId) {
        return invokerWeIdService.getWeIdDocumentJson(weId);
    }

    /**
     * @fixme
     * Set Public Key.
     * @param reqSetPublicKeyArgs the set public key args
     * @return the response data
     */
    @RequestMapping(value = "setPublicKey", method = RequestMethod.POST)
    public ResponseData<Boolean> setPublicKey(@RequestBody ReqSetPublicKeyArgs reqSetPublicKeyArgs) {
        return invokerWeIdService.setPublicKey(reqSetPublicKeyArgs);
    }

    /**
     * @fixme
     * Set Service.
     * @param reqSetServiceArgs the set service args
     * @return the response data
     */
    @RequestMapping(value = "setService", method = RequestMethod.POST)
    public ResponseData<Boolean> setService(@RequestBody ReqSetServiceArgs reqSetServiceArgs) {
        return invokerWeIdService.setService(reqSetServiceArgs);
    }

    /**
     * @fixme
     * Set Authentication.
     * @param reqSetAuthenticationArgs the set authentication args
     * @return the response data
     */
    @RequestMapping(value = "setAuthentication", method = RequestMethod.POST)
    public ResponseData<Boolean> setAuthentication(@RequestBody ReqSetAuthenticationArgs reqSetAuthenticationArgs) {
        return invokerWeIdService.setAuthentication(reqSetAuthenticationArgs);
    }

    /**
     * @fixme
     * Check if WeIdentity DID exists on Chain.
     * @param weId the WeIdentity DID
     * @return true if exists, false otherwise
     */
    @RequestMapping(value = "isWeIdExist/{weId}", method = RequestMethod.GET)
    public ResponseData<Boolean> isWeIdExist(@PathVariable("weId") String weId) {
        return invokerWeIdService.isWeIdExist(weId);
    }

}
