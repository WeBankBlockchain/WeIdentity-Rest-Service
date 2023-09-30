package com.webank.weid.http.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.webank.weid.blockchain.config.FiscoConfig;
import com.webank.weid.blockchain.constant.ErrorCode;
import com.webank.weid.constant.ProcessingMode;
import com.webank.weid.constant.WeIdConstant;
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
import com.webank.weid.blockchain.protocol.response.ResponseData;
import com.webank.weid.service.rpc.EvidenceService;
import com.webank.weid.service.impl.EvidenceServiceImpl;
import com.webank.weid.blockchain.service.fisco.engine.EngineFactoryFisco;
import com.webank.weid.blockchain.service.fisco.engine.EvidenceServiceEngineFisco;
import com.webank.weid.util.DateUtils;
import com.webank.weid.util.WeIdUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InvokerEvidenceServiceImpl extends BaseService implements
    InvokerEvidenceService {

    private static Logger logger = LoggerFactory.getLogger(InvokerEvidenceServiceImpl.class);
    
    private static String masterGroupId;
    static {
        try {
            FiscoConfig fiscoConfig = new FiscoConfig();
            fiscoConfig.load();
            masterGroupId = fiscoConfig.getGroupId();
        } catch (Exception e) {
            logger.error("Failed to load Fisco Config.");
        }
    }
    // A map to store group ID and evidence service instances
    Map<String, EvidenceService> evidenceServiceInstances = new ConcurrentHashMap<>();
    Map<String, EvidenceServiceEngineFisco> evidenceServiceEngineInstances = new ConcurrentHashMap<>();

    /**
     * A lazy initialization to create evidence service impl instance.
     *
     * @param groupId passing-in groupId
     * @return evidence service
     */
    private EvidenceService lazyInitializeEvidenceServiceImpl(String groupId) throws Exception {
        System.out.println("LAZY GID: " + groupId);
        FiscoConfig fiscoConfig;
        try {
            fiscoConfig = new FiscoConfig();
            fiscoConfig.load();
        } catch (Exception e) {
            logger.error("Failed to load Fisco Config.");
            return null;
        }
        if (groupId == null) {
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
                evidenceService = new EvidenceServiceImpl(ProcessingMode.IMMEDIATE, String.valueOf(groupId));
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
        String masterGroupId = fiscoConfig.getGroupId();
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
                String groupId;
                groupId = JsonUtil.removeDoubleQuotes(groupIdNode.toString());
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
        String adminPrivKey = KeyUtil.getPrivateKeyByWeId(KeyUtil.ADMIN_PRIVKEY_PATH,
            PropertiesUtil.getProperty("default.passphrase"));
        if (StringUtils.isEmpty(adminPrivKey)) {
            return new HttpResponseData<>(null, HttpReturnCode.INPUT_ILLEGAL.getCode(),
                HttpReturnCode.INPUT_ILLEGAL.getCodeDesc() + "(Private key empty or failed to unload)");
        }
        String hashString = hashNode.textValue();
        if (!containsHexPrefix(hashString)) {
            hashString = WeIdConstant.HEX_PREFIX + hashString;
        }
        ResponseData<Boolean> createResp = evidenceService.createRawEvidenceWithCustomKey(
                hashString,
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

    public static boolean containsHexPrefix(String input) {
        return !org.fisco.bcos.sdk.utils.StringUtils.isEmpty(input)
                && input.length() > 1
                && input.charAt(0) == '0'
                && input.charAt(1) == 'x';
    }

    @Override
    public HttpResponseData<Object> getEvidenceByHash(InputArg args) {
        JsonNode idNode;
        try {
            JsonNode functionArgNode = new ObjectMapper()
                .readTree(args.getFunctionArg());
            idNode = functionArgNode.get(WeIdentityParamKeyConstant.HASH_VALUE);
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
                String groupId;
                groupId = JsonUtil.removeDoubleQuotes(groupIdNode.toString());
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
        String hashString = idNode.textValue();
        if (!containsHexPrefix(hashString)) {
            hashString = WeIdConstant.HEX_PREFIX + hashString;
        }
        ResponseData<EvidenceInfo> respData = evidenceService.getEvidence(hashString);
        if (respData.getResult() == null) {
            return new HttpResponseData<>(null, respData.getErrorCode(), respData.getErrorMessage());
        }
        return new HttpResponseData<>(JsonUtil.convertJsonToSortedMap(JsonUtil.objToJsonStr(respData.getResult())), HttpReturnCode.SUCCESS);
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
                String groupId;
                groupId = JsonUtil.removeDoubleQuotes(groupIdNode.toString());
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

    private EvidenceService getEvidenceService(InputArg args) throws Exception {
        JsonNode groupIdNode;
        EvidenceService evidenceService;
        JsonNode txnArgNode = new ObjectMapper().readTree(args.getTransactionArg());
        groupIdNode = txnArgNode.get(WeIdentityParamKeyConstant.GROUP_ID);
        if (groupIdNode == null || StringUtils.isEmpty(groupIdNode.toString())) {
            logger.info("Cannot find groupId definition, using default.. {}", groupIdNode);
            evidenceService = lazyInitializeEvidenceServiceImpl();
        } else {
            String groupId;
            groupId = JsonUtil.removeDoubleQuotes(groupIdNode.toString());
            evidenceService = lazyInitializeEvidenceServiceImpl(groupId);
        }
        return evidenceService;
    }

    @Override
    public HttpResponseData<Object> delegateCreateEvidence(InputArg args) {
        JsonNode hashNode;
        JsonNode signNode;
        JsonNode logNode;
        try {
            JsonNode functionArgNode = new ObjectMapper()
                .readTree(args.getFunctionArg());
            hashNode = functionArgNode.get(WeIdentityParamKeyConstant.HASH);
            signNode = functionArgNode.get(WeIdentityParamKeyConstant.SIGN);
            logNode = functionArgNode.get(WeIdentityParamKeyConstant.LOG);
            if ( hashNode == null || StringUtils.isEmpty(hashNode.textValue())
                || signNode == null || StringUtils.isEmpty(signNode.textValue())) {
                return new HttpResponseData<>(false, HttpReturnCode.INPUT_NULL);
            }
        } catch (Exception e) {
            logger.error("[delegateCreateEvidence]: input args error: {}", args, e);
            return new HttpResponseData<>(false, HttpReturnCode.VALUE_FORMAT_ILLEGAL);
        }
        String log = (logNode == null || StringUtils.isEmpty(logNode.textValue())) ? "" : logNode.textValue();
        
        EvidenceService evidenceService;
        try {
            evidenceService = getEvidenceService(args);
            if (evidenceService == null) {
                return new HttpResponseData<>(false, HttpReturnCode.UNKNOWN_ERROR.getCode(),
                    HttpReturnCode.UNKNOWN_ERROR.getCodeDesc() + "(Failed to initialize evidence service, please check logs for details");
            }
        } catch (LoadContractException e) {
            return new HttpResponseData<>(false, HttpReturnCode.CONTRACT_ERROR.getCode(), HttpReturnCode.CONTRACT_ERROR.getCodeDesc());
        } catch (InitWeb3jException e) {
            return new HttpResponseData<>(false, HttpReturnCode.WEB3J_ERROR.getCode(), HttpReturnCode.WEB3J_ERROR.getCodeDesc());
        } catch (Exception e) {
            logger.info("Cannot find groupId definition: {}", e);
            return new HttpResponseData<>(false, HttpReturnCode.INPUT_ILLEGAL.getCode(),
                HttpReturnCode.INPUT_ILLEGAL.getCodeDesc() + "(Group ID illegal)");
        }
        String adminPrivKey = KeyUtil.getPrivateKeyByWeId(KeyUtil.ADMIN_PRIVKEY_PATH,
            PropertiesUtil.getProperty("default.passphrase"));
        if (StringUtils.isEmpty(adminPrivKey)) {
            return new HttpResponseData<>(false, HttpReturnCode.INPUT_ILLEGAL.getCode(),
                HttpReturnCode.INPUT_ILLEGAL.getCodeDesc() + "(Private key empty or failed to unload)");
        }
        String issuer = WeIdUtils.getWeIdFromPrivateKey(adminPrivKey);
        String hashString = hashNode.textValue();
        if (!containsHexPrefix(hashString)) {
            hashString = WeIdConstant.HEX_PREFIX + hashString;
        }
        ResponseData<Boolean> createResp = evidenceService.createRawEvidenceWithSpecificSigner(
            hashString,
            signNode.textValue(), 
            log, 
            DateUtils.getNoMillisecondTimeStamp(),
            null, 
            issuer, 
            adminPrivKey
        );
        if (!createResp.getResult()) {
            return new HttpResponseData<>(false, createResp.getErrorCode(),
                createResp.getErrorMessage());
        }
        return new HttpResponseData<>(true, HttpReturnCode.SUCCESS);
    }
    
    private EvidenceServiceEngineFisco getEvidenceServiceEngine(InputArg args) throws Exception {
        JsonNode groupIdNode;
        JsonNode txnArgNode = new ObjectMapper().readTree(args.getTransactionArg());
        groupIdNode = txnArgNode.get(WeIdentityParamKeyConstant.GROUP_ID);
        String groupId;
        if (groupIdNode == null || StringUtils.isEmpty(groupIdNode.toString())) {
            logger.info("Cannot find groupId definition, using default.. {}", groupIdNode);
            groupId = masterGroupId;
        } else {
            groupId = JsonUtil.removeDoubleQuotes(groupIdNode.toString());
            if (groupId == masterGroupId) {
                logger.info("Requesting master group id evidence service.., {}", groupId);
            } else {
                logger.info("Requesting evidence subgroup id instance.. {}", groupId);
            }
        }
        if (groupId == null) {
            logger.error("Group Id illegal: {}", groupId);
            return null;
        }
        EvidenceServiceEngineFisco evidenceServiceEngine = evidenceServiceEngineInstances.get(groupId);
        if (evidenceServiceEngine == null) {
            evidenceServiceEngine = EngineFactoryFisco.createEvidenceServiceEngine(groupId);
            evidenceServiceEngineInstances.put(groupId, evidenceServiceEngine);
        }
        return evidenceServiceEngine;
    }

    @Override
    public HttpResponseData<Object> delegateCreateEvidenceBatch(InputArg args) {
        JsonNode listNode;
        try {
            JsonNode functionArgNode = new ObjectMapper()
                .readTree(args.getFunctionArg());
            listNode = functionArgNode.get(WeIdentityParamKeyConstant.LIST);
            // 如果节点不是数组
            if (!listNode.isArray()) {
                logger.error("[delegateCreateEvidenceBatch] input does not an Array.");
                return new HttpResponseData<>(null, HttpReturnCode.INPUT_NULL);
            }
        } catch (Exception e) {
            logger.error("[delegateCreateEvidenceBatch]: input args error: {}", args, e);
            return new HttpResponseData<>(null, HttpReturnCode.VALUE_FORMAT_ILLEGAL);
        }

        String adminPrivKey = KeyUtil.getPrivateKeyByWeId(KeyUtil.ADMIN_PRIVKEY_PATH,
            PropertiesUtil.getProperty("default.passphrase"));
        if (StringUtils.isEmpty(adminPrivKey)) {
            return new HttpResponseData<>(null, HttpReturnCode.INPUT_ILLEGAL.getCode(),
                HttpReturnCode.INPUT_ILLEGAL.getCodeDesc() + "(Private key empty or failed to unload)");
        }
        String issuer = WeIdUtils.getWeIdFromPrivateKey(adminPrivKey);
        Long timeStamp = DateUtils.getNoMillisecondTimeStamp();

        List<String> hashValues = new ArrayList<>();
        List<String> signatures = new ArrayList<>();
        List<String> logs = new ArrayList<>();
        List<Long> timestamps = new ArrayList<>();
        List<String> signers = new ArrayList<>(); 
        ArrayNode nodes = (ArrayNode)listNode;
        for (JsonNode jsonNode : nodes) {
            JsonNode hashNode = jsonNode.get(WeIdentityParamKeyConstant.HASH);
            JsonNode signNode = jsonNode.get(WeIdentityParamKeyConstant.SIGN);
            JsonNode logNode = jsonNode.get(WeIdentityParamKeyConstant.LOG);
            if ( hashNode == null || signNode == null ) {
                logger.error("[delegateCreateEvidenceBatch] input params has null.");
                return new HttpResponseData<>(null, HttpReturnCode.INPUT_NULL);
            }
            String log = (logNode == null || StringUtils.isEmpty(logNode.textValue())) ? "" : logNode.textValue();
            String hashString = hashNode.textValue();
            if (!containsHexPrefix(hashString)) {
                hashString = WeIdConstant.HEX_PREFIX + hashString;
            }
            hashValues.add(hashString);
            signatures.add(signNode.textValue());
            logs.add(log);
            timestamps.add(timeStamp);
            signers.add(issuer);
        }

        EvidenceServiceEngineFisco createEvidenceServiceEngine;
        try {
            createEvidenceServiceEngine = getEvidenceServiceEngine(args);
            if (createEvidenceServiceEngine == null) {
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
        com.webank.weid.blockchain.protocol.response.ResponseData<List<Boolean>> response = createEvidenceServiceEngine.batchCreateEvidence(
            hashValues, 
            signatures, 
            logs, 
            timestamps, 
            signers, 
            adminPrivKey
        );
        if (response.getErrorCode().intValue() != ErrorCode.SUCCESS.getCode()) {
            return new HttpResponseData<>(response.getResult(), response.getErrorCode(),
                response.getErrorMessage());
        }
        return new HttpResponseData<>(response.getResult(), HttpReturnCode.SUCCESS);
    }
}
