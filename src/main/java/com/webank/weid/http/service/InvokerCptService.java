package com.webank.weid.http.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.webank.weid.http.constant.HttpErrorCode;
import com.webank.weid.http.protocol.request.ReqRegisterCptArgs;
import com.webank.weid.http.protocol.request.ReqUpdateCptArgs;
import com.webank.weid.http.util.JsonUtil;
import com.webank.weid.protocol.base.Cpt;
import com.webank.weid.protocol.base.CptBaseInfo;
import com.webank.weid.protocol.base.WeIdPrivateKey;
import com.webank.weid.protocol.request.RegisterCptArgs;
import com.webank.weid.protocol.request.UpdateCptArgs;
import com.webank.weid.protocol.response.ResponseData;
import com.webank.weid.rpc.CptService;

@Service
public class InvokerCptService {

    private Logger logger = LoggerFactory.getLogger(InvokerCptService.class);

    @Autowired
    private CptService cptService;

    /**
     * This is used to register a new CPT to the blockchain.
     * @param reqRegisterCptArgs the args
     * @return the response data
     */
    public ResponseData<CptBaseInfo> registerCpt(ReqRegisterCptArgs reqRegisterCptArgs) {

        ResponseData<CptBaseInfo> response = new ResponseData<CptBaseInfo>();
        try {
            WeIdPrivateKey cptPublisherPrivateKey = new WeIdPrivateKey();
            cptPublisherPrivateKey.setPrivateKey(reqRegisterCptArgs.getCptPublisherPrivateKey());

            RegisterCptArgs registerCptArgs = new RegisterCptArgs();
            registerCptArgs.setCptPublisher(reqRegisterCptArgs.getCptPublisher());
            registerCptArgs.setCptPublisherPrivateKey(cptPublisherPrivateKey);
            registerCptArgs.setCptJsonSchema(JsonUtil.objToJsonStr(reqRegisterCptArgs.getCptJsonSchema()));

            response = cptService.registerCpt(registerCptArgs);
        } catch (Exception e) {
            logger.error("[registerCpt]: unknow error. reqRegisterCptArgs:{}",
                reqRegisterCptArgs,
                e);
            response.setErrorCode(HttpErrorCode.UNKNOW_ERROR.getCode());
            response.setErrorMessage(HttpErrorCode.UNKNOW_ERROR.getCodeDesc());
        }
        return response;
    }

    /**
     * this is used to query cpt with the latest version which has been registered.
     * @param cptId the cpt id
     * @return the response data
     */
    public ResponseData<Cpt> queryCpt(Integer cptId) {

        ResponseData<Cpt> response = new ResponseData<Cpt>();
        try {
            response = cptService.queryCpt(cptId);
        } catch (Exception e) {
            logger.error("[queryCpt]: unknow error. cptId:{}", cptId, e);
            response.setErrorCode(HttpErrorCode.UNKNOW_ERROR.getCode());
            response.setErrorMessage(HttpErrorCode.UNKNOW_ERROR.getCodeDesc());
        }
        return response;
    }

    /**
     * This is used to update a CPT data which has been register.
     * @param reqUpdateCptArgs the args
     * @return the response data
     */
    public ResponseData<CptBaseInfo> updateCpt(ReqUpdateCptArgs reqUpdateCptArgs) {

        ResponseData<CptBaseInfo> response = new ResponseData<CptBaseInfo>();
        try {
            WeIdPrivateKey cptPublisherPrivateKey = new WeIdPrivateKey();
            cptPublisherPrivateKey.setPrivateKey(reqUpdateCptArgs.getCptPublisherPrivateKey());

            UpdateCptArgs updateCptArgs = new UpdateCptArgs();
            updateCptArgs.setCptId(reqUpdateCptArgs.getCptId());
            updateCptArgs.setCptJsonSchema(reqUpdateCptArgs.getCptJsonSchema());
            updateCptArgs.setCptPublisher(reqUpdateCptArgs.getCptPublisher());
            updateCptArgs.setCptPublisherPrivateKey(cptPublisherPrivateKey);

            response = cptService.updateCpt(updateCptArgs);
        } catch (Exception e) {
            logger.error("[updateCpt]: unknow error. reqUpdateCptArgs:{}", reqUpdateCptArgs, e);
            response.setErrorCode(HttpErrorCode.UNKNOW_ERROR.getCode());
            response.setErrorMessage(HttpErrorCode.UNKNOW_ERROR.getCodeDesc());
        }
        return response;
    }
}
