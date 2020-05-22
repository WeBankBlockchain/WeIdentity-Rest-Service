package com.webank.weid.http.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webank.weid.constant.ProcessingMode;
import com.webank.weid.http.constant.HttpReturnCode;
import com.webank.weid.http.constant.WeIdentityParamKeyConstant;
import com.webank.weid.http.protocol.request.InputArg;
import com.webank.weid.http.protocol.response.HttpResponseData;
import com.webank.weid.http.service.BaseService;
import com.webank.weid.http.service.InvokerEvidenceService;
import com.webank.weid.http.util.JsonUtil;
import com.webank.weid.http.util.KeyUtil;
import com.webank.weid.http.util.PropertiesUtil;
import com.webank.weid.protocol.base.EvidenceInfo;
import com.webank.weid.protocol.response.ResponseData;
import com.webank.weid.rpc.EvidenceService;
import com.webank.weid.service.impl.EvidenceServiceImpl;
import com.webank.weid.util.DateUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InvokerEvidenceServiceImpl extends BaseService implements
    InvokerEvidenceService {

    private Logger logger = LoggerFactory.getLogger(InvokerEvidenceServiceImpl.class);
    private String evidenceGroupIdStr = PropertiesUtil.getProperty("evidence.group.id");
    private EvidenceService evidenceService = NumberUtils.isDigits(evidenceGroupIdStr) ?
        new EvidenceServiceImpl(ProcessingMode.IMMEDIATE, Integer.parseInt(evidenceGroupIdStr)) :
        new EvidenceServiceImpl();

    @Override
    public HttpResponseData<Object> createEvidenceWithExtraInfo(InputArg args) {
        JsonNode idNode;
        JsonNode hashNode;
        JsonNode proofNode;
        JsonNode logNode;
        try {
            JsonNode functionArgNode = new ObjectMapper()
                .readTree(args.getFunctionArg());
            idNode = functionArgNode.get(WeIdentityParamKeyConstant.CREDENTIAL_ID);
            hashNode = functionArgNode.get(WeIdentityParamKeyConstant.HASH);
            proofNode = functionArgNode.get(WeIdentityParamKeyConstant.PROOF);
            logNode = functionArgNode.get(WeIdentityParamKeyConstant.LOG);
            if (idNode == null || StringUtils.isEmpty(idNode.textValue())
                || hashNode == null || StringUtils.isEmpty(hashNode.textValue())
                || proofNode == null || StringUtils.isEmpty(proofNode.textValue())
                || logNode == null || StringUtils.isEmpty(logNode.textValue())) {
                return new HttpResponseData<>(null, HttpReturnCode.INPUT_NULL);
            }
        } catch (Exception e) {
            logger.error("[createEvidenceWithExtraInfo]: input args error: {}", args, e);
            return new HttpResponseData<>(null, HttpReturnCode.VALUE_FORMAT_ILLEGAL);
        }
        String adminPrivKey = KeyUtil.getPrivateKeyByWeId(KeyUtil.SDK_PRIVKEY_PATH,
            PropertiesUtil.getProperty("default.passphrase"));
        if (StringUtils.isEmpty(adminPrivKey)) {
            return new HttpResponseData<>(null, HttpReturnCode.INPUT_ILLEGAL.getCode(),
                HttpReturnCode.INPUT_ILLEGAL.getCodeDesc() + "(Private key empty or failed to unload)");
        }
        ResponseData<Boolean> createResp = evidenceService.createRawEvidenceWithCustomKey(
            hashNode.textValue(),
            proofNode.textValue(),
            logNode.textValue(),
            DateUtils.getNoMillisecondTimeStamp(),
            idNode.textValue(),
            adminPrivKey
        );
        if (!createResp.getResult()) {
            return new HttpResponseData<>(false, createResp.getErrorCode(),
                createResp.getErrorMessage());
        }
        return new HttpResponseData<>(true, HttpReturnCode.SUCCESS);
    }

    @Override
    public HttpResponseData<Object> getEvidenceByCustomKey(InputArg args) {
        JsonNode idNode;
        try {
            JsonNode functionArgNode = new ObjectMapper()
                .readTree(args.getFunctionArg());
            idNode = functionArgNode.get(WeIdentityParamKeyConstant.CREDENTIAL_ID);
        } catch (Exception e) {
            logger.error("[getEvidenceByCustomKey]: input args error: {}", args, e);
            return new HttpResponseData<>(null, HttpReturnCode.VALUE_FORMAT_ILLEGAL);
        }
        ResponseData<EvidenceInfo> respData = evidenceService.getEvidenceByCustomKey(idNode.textValue());
        if (respData.getResult() == null) {
            return new HttpResponseData<>(null, respData.getErrorCode(), respData.getErrorMessage());
        }
        return new HttpResponseData<>(JsonUtil.convertJsonToSortedMap(JsonUtil.objToJsonStr(respData.getResult())), HttpReturnCode.SUCCESS);
    }
}
