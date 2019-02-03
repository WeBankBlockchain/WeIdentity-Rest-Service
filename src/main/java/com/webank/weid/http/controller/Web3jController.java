package com.webank.weid.http.controller;

import java.math.BigInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.webank.weid.http.service.InvokerWeb3jService;
import com.webank.weid.protocol.response.ResponseData;

/**
 * @author darwindu
 **/
@RestController
@RequestMapping(value = "weIdentity")
public class Web3jController {

    @Autowired
    private InvokerWeb3jService invokerWeb3jService;

    @RequestMapping(value = "getBlockLimit", method = RequestMethod.GET)
    public ResponseData<BigInteger> getBlockLimit() {
        return invokerWeb3jService.getBlockLimit();
    }

    @RequestMapping(value = "getNonce", method = RequestMethod.GET)
    public ResponseData<BigInteger> getNonce() {
        return invokerWeb3jService.getNonce();
    }
}
