package com.webank.weid.http.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.webank.weid.http.protocol.request.ReqRegisterCptArgs;
import com.webank.weid.http.protocol.request.ReqUpdateCptArgs;
import com.webank.weid.http.service.InvokerCptService;
import com.webank.weid.protocol.base.Cpt;
import com.webank.weid.protocol.base.CptBaseInfo;
import com.webank.weid.protocol.response.ResponseData;

@RestController
@RequestMapping(value = "weIdentity")
public class CptController {

    @Autowired
    private InvokerCptService invokerCptService;

    /**
     * This is used to register a new CPT to the blockchain.
     * @param reqRegisterCptArgs the args
     * @return the response data
     */
    @RequestMapping(value = "registerCpt", method = RequestMethod.POST)
    public ResponseData<CptBaseInfo> registerCpt(@RequestBody ReqRegisterCptArgs reqRegisterCptArgs) {
        return invokerCptService.registerCpt(reqRegisterCptArgs);
    }

    /**
     * this is used to query cpt with the latest version which has been registered.
     * @param cptId the cpt id
     * @return the response data
     */
    @RequestMapping(value = "queryCpt/{cptId}", method = RequestMethod.GET)
    public ResponseData<Cpt> queryCpt(@PathVariable("cptId") Integer cptId) {
        return invokerCptService.queryCpt(cptId);
    }

    /**
     * This is used to update a CPT data which has been register.
     * @param reqUpdateCptArgs the args
     * @return the response data
     */
    @RequestMapping(value = "updateCpt", method = RequestMethod.POST)
    public ResponseData<CptBaseInfo> updateCpt(@RequestBody ReqUpdateCptArgs reqUpdateCptArgs) {
        return invokerCptService.updateCpt(reqUpdateCptArgs);
    }
}
