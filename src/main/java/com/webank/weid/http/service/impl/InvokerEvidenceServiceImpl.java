package com.webank.weid.http.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webank.weid.config.FiscoConfig;
import com.webank.weid.constant.ProcessingMode;
import com.webank.weid.exception.InitWeb3jException;
import com.webank.weid.exception.LoadContractException;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InvokerEvidenceServiceImpl extends BaseService implements
    InvokerEvidenceService {

    private Logger logger = LoggerFactory.getLogger(InvokerEvidenceServiceImpl.class);
    // A map to store group ID and evidence service instances
    Map<Integer, EvidenceService> evidenceServiceInstances = new ConcurrentHashMap<>();

    /**
     * A lazy initialization to create evidence service impl instance.
     *
     * @param groupId passing-in groupId
     * @return evidence service
     */
    private EvidenceService lazyInitializeEvidenceServiceImpl(Integer groupId) throws Exception {
        System.out.println("LAZY GID: " + groupId);
        FiscoConfig fiscoConfig;
        try {
            fiscoConfig = new FiscoConfig();
            fiscoConfig.load();
        } catch (Exception e) {
            logger.error("Failed to load Fisco Config.");
            return null;
        }
        if (groupId == null || groupId == 0) {
            logger.error("Group Id illegal: {}", groupId);
            return null;
        }
        if (groupId.equals(Integer.valueOf(fiscoConfig.getGroupId()))) {
            // this is master group
            logger.info("Requesting master group id evidence service.., {}", groupId);
            EvidenceService evidenceService = evidenceServiceInstances.get(groupId);
            if (evidenceService == null) {
                evidenceService = new EvidenceServiceImpl();
                evidenceServiceInstances.put(groupId, evidenceService);
            }
            return evidenceService;
        } else {
            logger.info("Requesting evidence subgroup id instance.. {}", groupId);
            EvidenceService evidenceService = evidenceServiceInstances.get(groupId);
            if (evidenceService == null) {
                evidenceService = new EvidenceServiceImpl(ProcessingMode.IMMEDIATE, groupId);
                evidenceServiceInstances.put(groupId, evidenceService);
            }
            return evidenceService;
        }
    }

    private EvidenceService lazyInitializeEvidenceServiceImpl() throws Exception {
        FiscoConfig fiscoConfig;
        try {
            fiscoConfig = new FiscoConfig();
            fiscoConfig.load();
        } catch (Exception e) {
            logger.error("Failed to load Fisco Config.");
            return null;
        }
        Integer masterGroupId = Integer.valueOf(fiscoConfig.getGroupId());
        System.out.println("LAZY GID MASTER: " + masterGroupId);
        logger.info("Requesting default (master) group id evidence service: {}", masterGroupId);
        EvidenceService evidenceService = evidenceServiceInstances.get(masterGroupId);
        if (evidenceService == null) {
            evidenceService = new EvidenceServiceImpl();
            evidenceServiceInstances.put(masterGroupId, evidenceService);
        }
        return evidenceService;
    }

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
        JsonNode groupIdNode;
        EvidenceService evidenceService;
        try {
            JsonNode txnArgNode = new ObjectMapper().readTree(args.getTransactionArg());
            groupIdNode = txnArgNode.get(WeIdentityParamKeyConstant.GROUP_ID);
            if (groupIdNode == null || StringUtils.isEmpty(groupIdNode.toString())) {
                logger.info("Cannot find groupId definition, using default.. {}", groupIdNode);
                evidenceService = lazyInitializeEvidenceServiceImpl();
            } else {
                Integer groupId;
                groupId = Integer.valueOf(JsonUtil.removeDoubleQuotes(groupIdNode.toString()));
                evidenceService = lazyInitializeEvidenceServiceImpl(groupId);
            }
            if (evidenceService == null) {
                return new HttpResponseData<>(null, HttpReturnCode.UNKNOWN_ERROR.getCode(),
                    HttpReturnCode.UNKNOWN_ERROR.getCodeDesc() + "(Failed to initialize evidence service, please check logs for details");
            }
        } catch (LoadContractException e) {
            return new HttpResponseData<>(null, HttpReturnCode.CONTRACT_ERROR.getCode(), HttpReturnCode.CONTRACT_ERROR.getCodeDesc());
        } catch (InitWeb3jException e) {
            return new HttpResponseData<>(null, HttpReturnCode.WEB3J_ERROR.getCode(), HttpReturnCode.WEB3J_ERROR.getCodeDesc());
        } catch (Exception e) {
            logger.info("Cannot find groupId definition: {}", e);
            return new HttpResponseData<>(null, HttpReturnCode.INPUT_ILLEGAL.getCode(),
                HttpReturnCode.INPUT_ILLEGAL.getCodeDesc() + "(Group ID illegal)");
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
        JsonNode groupIdNode;
        EvidenceService evidenceService;
        try {
            JsonNode txnArgNode = new ObjectMapper().readTree(args.getTransactionArg());
            groupIdNode = txnArgNode.get(WeIdentityParamKeyConstant.GROUP_ID);
            if (groupIdNode == null || StringUtils.isEmpty(groupIdNode.toString())) {
                logger.info("Cannot find groupId definition, using default.. {}", groupIdNode);
                evidenceService = lazyInitializeEvidenceServiceImpl();
            } else {
                Integer groupId;
                groupId = Integer.valueOf(JsonUtil.removeDoubleQuotes(groupIdNode.toString()));
                evidenceService = lazyInitializeEvidenceServiceImpl(groupId);
            }
            if (evidenceService == null) {
                return new HttpResponseData<>(null, HttpReturnCode.UNKNOWN_ERROR.getCode(),
                    HttpReturnCode.UNKNOWN_ERROR.getCodeDesc() + "(Failed to initialize evidence service, please check logs for details");
            }
        } catch (LoadContractException e) {
            return new HttpResponseData<>(null, HttpReturnCode.CONTRACT_ERROR.getCode(), HttpReturnCode.CONTRACT_ERROR.getCodeDesc());
        } catch (InitWeb3jException e) {
            return new HttpResponseData<>(null, HttpReturnCode.WEB3J_ERROR.getCode(), HttpReturnCode.WEB3J_ERROR.getCodeDesc());
        } catch (Exception e) {
            logger.info("Cannot find groupId definition: {}", e);
            return new HttpResponseData<>(null, HttpReturnCode.INPUT_ILLEGAL.getCode(),
                HttpReturnCode.INPUT_ILLEGAL.getCodeDesc() + "(Group ID illegal)");
        }
        ResponseData<EvidenceInfo> respData = evidenceService.getEvidenceByCustomKey(idNode.textValue());
        if (respData.getResult() == null) {
            return new HttpResponseData<>(null, respData.getErrorCode(), respData.getErrorMessage());
        }
        return new HttpResponseData<>(JsonUtil.convertJsonToSortedMap(JsonUtil.objToJsonStr(respData.getResult())), HttpReturnCode.SUCCESS);
    }
}
