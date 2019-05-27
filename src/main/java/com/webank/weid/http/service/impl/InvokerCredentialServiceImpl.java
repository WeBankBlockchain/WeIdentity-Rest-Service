/*
 *       Copyright© (2019) WeBank Co., Ltd.
 *
 *       This file is part of weidentity-http-service.
 *
 *       weidentity-http-service is free software: you can redistribute it and/or modify
 *       it under the terms of the GNU Lesser General Public License as published by
 *       the Free Software Foundation, either version 3 of the License, or
 *       (at your option) any later version.
 *
 *       weidentity-http-service is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU Lesser General Public License for more details.
 *
 *       You should have received a copy of the GNU Lesser General Public License
 *       along with weidentity-http-service.  If not, see <https://www.gnu.org/licenses/>.
 */


package com.webank.weid.http.service.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.webank.weid.constant.CredentialConstant;
import com.webank.weid.constant.ErrorCode;
import com.webank.weid.constant.ParamKeyConstant;
import com.webank.weid.http.constant.HttpReturnCode;
import com.webank.weid.http.constant.WeIdentityParamKeyConstant;
import com.webank.weid.http.protocol.request.InputArg;
import com.webank.weid.http.protocol.response.HttpResponseData;
import com.webank.weid.http.service.BaseService;
import com.webank.weid.http.service.InvokerCredentialService;
import com.webank.weid.http.util.JsonUtil;
import com.webank.weid.http.util.PrivateKeyUtil;
import com.webank.weid.protocol.base.Credential;
import com.webank.weid.protocol.base.WeIdPrivateKey;
import com.webank.weid.protocol.request.CreateCredentialArgs;
import com.webank.weid.protocol.response.ResponseData;
import com.webank.weid.rpc.CredentialService;
import com.webank.weid.service.impl.CredentialServiceImpl;
import com.webank.weid.util.CredentialUtils;
import com.webank.weid.util.DateUtils;

@Component
public class InvokerCredentialServiceImpl extends BaseService implements InvokerCredentialService {

    private Logger logger = LoggerFactory.getLogger(InvokerCredentialServiceImpl.class);

    private CredentialService credentialService = new CredentialServiceImpl();

    /**
     * Generate a credential for client to sign. The signature field is null, and both full claim
     * and claimHash will be returned. The returned json String is an key-ordered compact json.
     *
     * @param createCredentialFuncArgs the functionArgs
     * @return the Map contains Credential content and claimHash.
     */
    public HttpResponseData<Object> createCredentialInvoke(
        InputArg createCredentialFuncArgs) {
        try {
            JsonNode functionArgNode = new ObjectMapper()
                .readTree(createCredentialFuncArgs.getFunctionArg());
            JsonNode cptIdNode = functionArgNode.get(ParamKeyConstant.CPT_ID);
            JsonNode issuerNode = functionArgNode.get(ParamKeyConstant.ISSUER);
            JsonNode expirationDateNode = functionArgNode.get(ParamKeyConstant.EXPIRATION_DATE);
            JsonNode claimNode = functionArgNode.get(ParamKeyConstant.CLAIM);
            if (cptIdNode == null || StringUtils.isEmpty(cptIdNode.toString())
                || issuerNode == null || StringUtils.isEmpty(issuerNode.textValue())
                || expirationDateNode == null || StringUtils.isEmpty(expirationDateNode.textValue())
                || claimNode == null || StringUtils.isEmpty(claimNode.toString())) {
                return new HttpResponseData<>(null, HttpReturnCode.INPUT_NULL);
            }
            Integer cptId;
            try {
                cptId = Integer.valueOf(JsonUtil.removeDoubleQuotes(cptIdNode.toString()));
            } catch (Exception e) {
                return new HttpResponseData<>(null, HttpReturnCode.VALUE_FORMAT_ILLEGAL);
            }
            Long expirationDate;
            try {
                expirationDate = DateUtils
                    .convertUtcDateToTimeStamp(expirationDateNode.textValue());
            } catch (Exception e) {
                return new HttpResponseData<>(null,
                    ErrorCode.CREDENTIAL_EXPIRE_DATE_ILLEGAL.getCode(),
                    ErrorCode.CREDENTIAL_EXPIRE_DATE_ILLEGAL.getCodeDesc());
            }
            Credential credential = new Credential();
            credential.setId(UUID.randomUUID().toString());
            credential.setCptId(cptId);
            credential.setIssuer(issuerNode.textValue());
            credential.setExpirationDate(expirationDate);
            credential.setContext(CredentialConstant.DEFAULT_CREDENTIAL_CONTEXT);
            // Now here is a trick - timestamp granularity is too "fine". Need to make it coarse.
            Long issuanceDate = DateUtils.convertUtcDateToTimeStamp(
                DateUtils.convertTimestampToUtc(DateUtils.getCurrentTimeStamp()));
            credential.setIssuanceDate(issuanceDate);
            Map<String, Object> claimMap;
            try {
                claimMap = (Map<String, Object>) JsonUtil
                    .jsonStrToObj(new HashMap<String, Object>(), claimNode.toString());
            } catch (Exception e) {
                return new HttpResponseData<>(null,
                    ErrorCode.CREDENTIAL_CLAIM_DATA_ILLEGAL.getCode(),
                    ErrorCode.CREDENTIAL_CLAIM_DATA_ILLEGAL.getCodeDesc());
            }
            credential.setClaim(claimMap);

            JsonNode txnArgNode = new ObjectMapper()
                .readTree(createCredentialFuncArgs.getTransactionArg());
            JsonNode keyIndexNode = txnArgNode.get(WeIdentityParamKeyConstant.KEY_INDEX);

            // Decide the key holding mechanism
            if (keyIndexNode == null || StringUtils.isEmpty(keyIndexNode.textValue())) {
                // this is the client-storage privkey approach
                String claimHash = CredentialUtils.getClaimHash(credential, null);
                // Construct return value - a middle term
                Map<String, Object> credMap = JsonUtil.objToMap(credential);
                credMap.put(WeIdentityParamKeyConstant.CLAIM_HASH, claimHash);
                return new HttpResponseData<>(
                    JsonUtil.convertJsonToSortedMap(JsonUtil.mapToCompactJson(credMap)),
                    HttpReturnCode.SUCCESS);
            } else {
                // this is the server-hosting privkey approach
                String privateKey = PrivateKeyUtil
                    .getPrivateKeyByWeId(PrivateKeyUtil.SDK_PRIVKEY_PATH, keyIndexNode.textValue());
                if (StringUtils.isEmpty(privateKey)) {
                    return new HttpResponseData<>(null, HttpReturnCode.INVOKER_ILLEGAL);
                }
                // check validity 1st round
                WeIdPrivateKey weIdPrivateKey = new WeIdPrivateKey();
                weIdPrivateKey.setPrivateKey(privateKey);
                CreateCredentialArgs createArgs = new CreateCredentialArgs();
                createArgs.setCptId(cptId);
                createArgs.setExpirationDate(expirationDate);
                createArgs.setClaim(claimMap);
                createArgs.setIssuer(issuerNode.textValue());
                createArgs.setWeIdPrivateKey(weIdPrivateKey);
                ErrorCode errorCode = CredentialUtils.isCreateCredentialArgsValid(createArgs);
                if (errorCode.getCode() != ErrorCode.SUCCESS.getCode()) {
                    return new HttpResponseData<>(null, errorCode.getCode(),
                        errorCode.getCodeDesc());
                }

                Map<String, String> credentialProof = CredentialUtils
                    .buildCredentialProof(credential, privateKey, null);
                credential.setProof(credentialProof);
                // check validity 2nd round
                errorCode = CredentialUtils.isCredentialValid(credential);
                if (errorCode.getCode() != ErrorCode.SUCCESS.getCode()) {
                    return new HttpResponseData<>(null, errorCode.getCode(),
                        errorCode.getCodeDesc());
                }
                ResponseData<String> response = credentialService.getCredentialJson(credential);
                return new HttpResponseData<>(
                    JsonUtil.convertJsonToSortedMap(response.getResult()),
                    response.getErrorCode(), response.getErrorMessage());
            }
        } catch (IOException e) {
            return new HttpResponseData<>(null, HttpReturnCode.INPUT_ILLEGAL);
        } catch (Exception e) {
            logger.error("[createCredentialInvoke]: SDK error. reqCreateCredentialArgs:{}",
                createCredentialFuncArgs,
                e);
            return new HttpResponseData<>(null, HttpReturnCode.WEID_SDK_ERROR.getCode(),
                HttpReturnCode.WEID_SDK_ERROR.getCodeDesc().concat(e.getMessage()));
        }
    }

    /**
     * Verify the validity of a credential. Need format conversion (UTC date and @context)
     *
     * @param verifyCredentialFuncArgs the credential json args
     * @return the Boolean response data
     */
    public HttpResponseData<Object> verifyCredentialInvoke(InputArg verifyCredentialFuncArgs) {
        Credential credential = null;
        try {
            credential = (Credential) JsonUtil
                .jsonStrToObj(new Credential(), verifyCredentialFuncArgs.getFunctionArg());
        } catch (Exception e) {
            // The input credential is a json, so keep moving down.
            logger.info("Detected Portable-Json format credential, continuing..");
        }

        if (credential == null) {
            try {
                Map<String, Object> credMap = (Map<String, Object>) JsonUtil
                    .jsonStrToObj(new HashMap<String, Object>(),
                        verifyCredentialFuncArgs.getFunctionArg());

                // Convert context into @context
                Object context = credMap
                    .get(CredentialConstant.CREDENTIAL_CONTEXT_PORTABLE_JSON_FIELD);
                credMap.remove(CredentialConstant.CREDENTIAL_CONTEXT_PORTABLE_JSON_FIELD);
                if (!StringUtils.isEmpty(context.toString())) {
                    credMap.put(ParamKeyConstant.CONTEXT, context);
                } else if (StringUtils.isEmpty(credMap.get(ParamKeyConstant.CONTEXT).toString())) {
                    credMap
                        .put(ParamKeyConstant.CONTEXT,
                            CredentialConstant.DEFAULT_CREDENTIAL_CONTEXT);
                }
                // Convert dates
                String issuanceDate = credMap.get(ParamKeyConstant.ISSUANCE_DATE).toString();
                String expirationDate = credMap.get(ParamKeyConstant.EXPIRATION_DATE).toString();
                credMap.put(ParamKeyConstant.ISSUANCE_DATE,
                    DateUtils.convertUtcDateToTimeStamp(issuanceDate));
                credMap.put(ParamKeyConstant.EXPIRATION_DATE,
                    DateUtils.convertUtcDateToTimeStamp(expirationDate));
                Map<String, Object> proofMap = (Map<String, Object>) credMap
                    .get(ParamKeyConstant.PROOF);
                String proofDate = proofMap.get(ParamKeyConstant.PROOF_CREATED).toString();
                //  proofMap.put(ParamKeyConstant.PROOF_CREATED,
                //    DateUtils.convertUtcDateToTimeStamp(proofDate));
                credMap.put(ParamKeyConstant.PROOF, proofMap);
                credential = (Credential) JsonUtil
                    .jsonStrToObj(new Credential(), JsonUtil.mapToCompactJson(credMap));
            } catch (Exception e) {
                logger.error("Input credential format illegal: {}", verifyCredentialFuncArgs);
                return new HttpResponseData<>(null, HttpReturnCode.INPUT_ILLEGAL.getCode(),
                    HttpReturnCode.INPUT_ILLEGAL.getCodeDesc().concat(e.getMessage()));
            }
        }

        try {
            ResponseData<Boolean> responseData = credentialService.verify(credential);
            return new HttpResponseData<>(responseData.getResult(),
                responseData.getErrorCode(), responseData.getErrorMessage());
        } catch (Exception e) {
            logger.error("[verifyCredentialInvoke]: SDK error. reqCredentialArgs:{}",
                verifyCredentialFuncArgs,
                e);
            return new HttpResponseData<>(null, HttpReturnCode.WEID_SDK_ERROR.getCode(),
                HttpReturnCode.WEID_SDK_ERROR.getCodeDesc().concat(e.getMessage()));
        }
    }
}
