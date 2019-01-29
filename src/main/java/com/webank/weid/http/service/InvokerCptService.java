package com.webank.weid.http.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.webank.weid.http.constant.HttpErrorCode;
import com.webank.weid.http.protocol.request.ReqRegisterCptMapArgs;
import com.webank.weid.http.protocol.request.ReqRegisterCptStringArgs;
import com.webank.weid.http.protocol.request.ReqRegisterTranCptMapArgs;
import com.webank.weid.http.protocol.request.ReqUpdateCptArgs;
import com.webank.weid.http.protocol.request.ReqUpdateCptStringArgs;
import com.webank.weid.protocol.base.Cpt;
import com.webank.weid.protocol.base.CptBaseInfo;
import com.webank.weid.protocol.base.WeIdAuthentication;
import com.webank.weid.protocol.base.WeIdPrivateKey;
import com.webank.weid.protocol.request.CptMapArgs;
import com.webank.weid.protocol.request.CptStringArgs;
import com.webank.weid.protocol.response.ResponseData;
import com.webank.weid.rpc.CptService;

@Service
public class InvokerCptService {

    private Logger logger = LoggerFactory.getLogger(InvokerCptService.class);

    @Autowired
    private CptService cptService;

    /**
     * This is used to register a new CPT to the blockchain.
     * @param reqRegisterCptMapArgs the args
     * @return the response data
     */
    public ResponseData<CptBaseInfo> registerCpt(
        ReqRegisterCptMapArgs reqRegisterCptMapArgs) {

        ResponseData<CptBaseInfo> response = new ResponseData<CptBaseInfo>();
        try {
            WeIdPrivateKey weIdPrivateKey = new WeIdPrivateKey();
            weIdPrivateKey.setPrivateKey(reqRegisterCptMapArgs.getWeIdPrivateKey());

            WeIdAuthentication weIdAuthentication = this.buildWeIdAuthority(
                reqRegisterCptMapArgs.getWeIdPrivateKey(),
                reqRegisterCptMapArgs.getWeId());

            CptMapArgs cptMapArgs = new CptMapArgs();
            cptMapArgs.setWeIdAuthentication(weIdAuthentication);
            cptMapArgs.setCptJsonSchema(reqRegisterCptMapArgs.getCptJsonSchema());

            response = cptService.registerCpt(cptMapArgs);
        } catch (Exception e) {
            logger.error("[registerCpt]: unknow error. reqRegisterCptArgs:{}",
                reqRegisterCptMapArgs,
                e);
            response.setErrorCode(HttpErrorCode.UNKNOW_ERROR.getCode());
            response.setErrorMessage(HttpErrorCode.UNKNOW_ERROR.getCodeDesc());
        }
        return response;
    }

    public ResponseData<CptBaseInfo> registerTranCpt(
        ReqRegisterTranCptMapArgs reqRegisterTranCptMapArgs) {

        ResponseData<CptBaseInfo> response = new ResponseData<CptBaseInfo>();
        try {
            WeIdPrivateKey weIdPrivateKey = new WeIdPrivateKey();
            weIdPrivateKey.setPrivateKey(reqRegisterTranCptMapArgs.getWeIdPrivateKey());

            WeIdAuthentication weIdAuthentication = null;

            CptMapArgs cptMapArgs = new CptMapArgs();
            cptMapArgs.setCptJsonSchema(reqRegisterTranCptMapArgs.getCptJsonSchema());

            response = cptService.registerCpt(cptMapArgs, reqRegisterTranCptMapArgs.getBodySigned());
        } catch (Exception e) {
            logger.error("[registerCpt]: unknow error. reqRegisterCptArgs:{}",
                reqRegisterTranCptMapArgs,
                e);
            response.setErrorCode(HttpErrorCode.UNKNOW_ERROR.getCode());
            response.setErrorMessage(HttpErrorCode.UNKNOW_ERROR.getCodeDesc());
        }
        return response;
    }

    /**
     * build WeIdAuthority
     * @param weIdPrivateKeyStr this is String
     * @param weId this is String
     * @return
     */
    private WeIdAuthentication buildWeIdAuthority(String weIdPrivateKeyStr, String weId) {

        WeIdPrivateKey weIdPrivateKey = new WeIdPrivateKey();
        weIdPrivateKey.setPrivateKey(weIdPrivateKeyStr);

        WeIdAuthentication weIdAuthentication = new WeIdAuthentication();
        weIdAuthentication.setWeId(weId);
        weIdAuthentication.setWeIdPrivateKey(weIdPrivateKey);
        return weIdAuthentication;
    }

    /**
     * This is used to register a new CPT to the blockchain.
     * @param reqRegisterCptStringArgs the args
     * @return the response data
     */
    public ResponseData<CptBaseInfo> registerCpt(
        ReqRegisterCptStringArgs reqRegisterCptStringArgs) {

        ResponseData<CptBaseInfo> response = new ResponseData<CptBaseInfo>();
        try {
            WeIdAuthentication weIdAuthentication = this.buildWeIdAuthority(
                reqRegisterCptStringArgs.getWeIdPrivateKey(),
                reqRegisterCptStringArgs.getWeId());

            CptStringArgs cptStringArgs = new CptStringArgs();
            cptStringArgs.setWeIdAuthentication(weIdAuthentication);
            cptStringArgs.setCptJsonSchema(reqRegisterCptStringArgs.getCptJsonSchema());

            response = cptService.registerCpt(cptStringArgs);
        } catch (Exception e) {
            logger.error("[registerCpt]: unknow error. reqRegisterCptArgs:{}",
                reqRegisterCptStringArgs,
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
            WeIdAuthentication weIdAuthentication = this.buildWeIdAuthority(
                reqUpdateCptArgs.getWeIdPrivateKey(),
                reqUpdateCptArgs.getWeId());

            CptMapArgs cptMapArgs = new CptMapArgs();
            cptMapArgs.setCptJsonSchema(reqUpdateCptArgs.getCptJsonSchema());
            cptMapArgs.setWeIdAuthentication(weIdAuthentication);

            response = cptService.updateCpt(cptMapArgs, reqUpdateCptArgs.getCptId());
        } catch (Exception e) {
            logger.error("[updateCpt]: unknow error. reqUpdateCptArgs:{}", reqUpdateCptArgs, e);
            response.setErrorCode(HttpErrorCode.UNKNOW_ERROR.getCode());
            response.setErrorMessage(HttpErrorCode.UNKNOW_ERROR.getCodeDesc());
        }
        return response;
    }

    /**
     * This is used to update a CPT data which has been register.
     * @param reqUpdateCptStringArgs the args
     * @return the response data
     */
    public ResponseData<CptBaseInfo> updateCpt(ReqUpdateCptStringArgs reqUpdateCptStringArgs) {

        ResponseData<CptBaseInfo> response = new ResponseData<CptBaseInfo>();
        try {
            WeIdAuthentication weIdAuthentication = this.buildWeIdAuthority(
                reqUpdateCptStringArgs.getWeIdPrivateKey(),
                reqUpdateCptStringArgs.getWeId());

            CptStringArgs cptStringArgs = new CptStringArgs();
            cptStringArgs.setCptJsonSchema(reqUpdateCptStringArgs.getCptJsonSchema());
            cptStringArgs.setWeIdAuthentication(weIdAuthentication);

            response = cptService.updateCpt(cptStringArgs, reqUpdateCptStringArgs.getCptId());
        } catch (Exception e) {
            logger.error("[updateCpt]: unknow error. reqUpdateCptArgs:{}", reqUpdateCptStringArgs, e);
            response.setErrorCode(HttpErrorCode.UNKNOW_ERROR.getCode());
            response.setErrorMessage(HttpErrorCode.UNKNOW_ERROR.getCodeDesc());
        }
        return response;
    }
}
