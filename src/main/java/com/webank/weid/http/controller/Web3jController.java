package com.webank.weid.http.controller;

import java.math.BigInteger;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.webank.weid.protocol.response.ResponseData;
import com.webank.weid.service.BaseService;

/**
 * @author darwindu
 **/
@RestController
@RequestMapping(value = "weIdentity")
public class Web3jController {

    @RequestMapping(value = "getBlockLimit", method = RequestMethod.GET)
    public ResponseData<BigInteger> getBlockLimit() {
        return BaseService.getBlockLimit();
    }
}
