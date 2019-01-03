package com.webank.weid.http.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.webank.weid.http.protocol.request.ReqRegisterAuthorityIssuerArgs;
import com.webank.weid.http.protocol.request.ReqRemoveAuthorityIssuerArgs;
import com.webank.weid.http.service.InvokerAuthorityIssuerService;
import com.webank.weid.protocol.base.AuthorityIssuer;
import com.webank.weid.protocol.response.ResponseData;

@RestController
@RequestMapping(value = "weIdentity")
public class AuthorityIssuerController {

    @Autowired
    private InvokerAuthorityIssuerService invokerAuthorityIssuerService;

    /**
     * Register a new Authority Issuer on Chain.
     * @param reqRegisterAuthorityIssuerArgs the args
     * @return the Boolean response data
     */
    @RequestMapping(value = "registerAuthorityIssuer", method = RequestMethod.POST)
    public ResponseData<Boolean> registerAuthorityIssuer(
        @RequestBody ReqRegisterAuthorityIssuerArgs reqRegisterAuthorityIssuerArgs) {
        return invokerAuthorityIssuerService.registerAuthorityIssuer(
            reqRegisterAuthorityIssuerArgs);
    }

    /**
     * Remove a new Authority Issuer on Chain.
     * @param reqRemoveAuthorityIssuerArgs the args
     * @return the Boolean response data
     */
    @RequestMapping(value = "removeAuthorityIssuer", method = RequestMethod.POST)
    public ResponseData<Boolean> removeAuthorityIssuer(
        @RequestBody ReqRemoveAuthorityIssuerArgs reqRemoveAuthorityIssuerArgs) {
        return invokerAuthorityIssuerService.removeAuthorityIssuer(reqRemoveAuthorityIssuerArgs);
    }

    /**
     * Check whether the given weId is an authority issuer.
     * @param weId the WeIdentity DID
     * @return the Boolean response data
     */
    @RequestMapping(value = "isAuthorityIssuer/{weId}", method = RequestMethod.GET)
    public ResponseData<Boolean> isAuthorityIssuer(@PathVariable("weId") String weId) {
        return invokerAuthorityIssuerService.isAuthorityIssuer(weId);
    }

    /**
     * Query the authority issuer information given weId.
     * @param weId the WeIdentity DID
     * @return the AuthorityIssuer response data
     */
    @RequestMapping(value = "queryAuthorityIssuerInfo/{weId}", method = RequestMethod.GET)
    public ResponseData<AuthorityIssuer> queryAuthorityIssuerInfo(@PathVariable("weId") String weId) {
        return invokerAuthorityIssuerService.queryAuthorityIssuerInfo(weId);
    }
}
