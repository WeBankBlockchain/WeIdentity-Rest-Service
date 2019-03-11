package com.webank.weid.http.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.webank.weid.constant.CredentialConstant;
import com.webank.weid.constant.ErrorCode;
import com.webank.weid.constant.ParamKeyConstant;
import com.webank.weid.http.constant.HttpReturnCode;
import com.webank.weid.http.constant.WeIdentityParamKeyConstant;
import com.webank.weid.http.protocol.request.ReqCreateCredentialArgs;
import com.webank.weid.http.protocol.request.ReqCredentialArgs;
import com.webank.weid.http.protocol.response.HttpResponseData;
import com.webank.weid.http.util.InputUtil;
import com.webank.weid.protocol.base.Credential;
import com.webank.weid.protocol.base.CredentialWrapper;
import com.webank.weid.protocol.base.WeIdPrivateKey;
import com.webank.weid.protocol.request.CreateCredentialArgs;
import com.webank.weid.protocol.response.ResponseData;
import com.webank.weid.rpc.CredentialService;
import com.webank.weid.service.impl.CredentialServiceImpl;
import com.webank.weid.util.CredentialUtils;
import com.webank.weid.util.DateUtils;
import com.webank.weid.util.JsonUtil;

@Service
public class InvokerCredentialService extends BaseService {

    private Logger logger = LoggerFactory.getLogger(InvokerCredentialService.class);

    private CredentialService credentialService = new CredentialServiceImpl();

    /**
     * Generate a credential for client to sign. The signature field is null, and both full claim
     * and claimHash will be returned. The returned json String is an key-ordered compact json.
     *
     * @param createCredentialFuncArgs the functionArgs
     * @return the Map contains Credential content and claimHash.
     */
    public HttpResponseData<String> createCredentialInvoke(String createCredentialFuncArgs) {
        try {
            JsonNode functionArgNode = new ObjectMapper().readTree(createCredentialFuncArgs);
            JsonNode cptIdNode = functionArgNode.get(ParamKeyConstant.CPT_ID);
            JsonNode issuerNode = functionArgNode.get(ParamKeyConstant.ISSUER);
            JsonNode expirationDateNode = functionArgNode.get(ParamKeyConstant.EXPIRATION_DATE);
            JsonNode claimNode = functionArgNode.get(ParamKeyConstant.CLAIM);
            if (cptIdNode == null || StringUtils.isEmpty(cptIdNode.toString())
                || issuerNode == null || StringUtils.isEmpty(issuerNode.textValue())
                || expirationDateNode == null || StringUtils.isEmpty(expirationDateNode.textValue())
                || claimNode == null || StringUtils.isEmpty(claimNode.toString())) {
                return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.INPUT_NULL);
            }
            String cptId = InputUtil.removeDoubleQuotes(cptIdNode.toString());
            Credential credential = new Credential();
            credential.setId(UUID.randomUUID().toString());
            credential.setCptId(Integer.valueOf(cptId));
            credential.setIssuer(issuerNode.textValue());
            credential.setExpirationDate(
                DateUtils.convertUtcDateToTimeStamp(expirationDateNode.textValue()));
            credential.setContext(CredentialConstant.DEFAULT_CREDENTIAL_CONTEXT);
            credential.setIssuranceDate(DateUtils.getCurrentTimeStamp());
            Map<String, Object> claimMap = (Map<String, Object>) JsonUtil
                .jsonStrToObj(new HashMap<String, Object>(), claimNode.toString());
            credential.setClaim(claimMap);
            String claimHash = CredentialUtils.getClaimHash(credential, null);

            // Construct return value
            Map<String, Object> credMap = JsonUtil.objToMap(credential);
            credMap.put(WeIdentityParamKeyConstant.CLIAM_HASH, claimHash);
            return new HttpResponseData<>(JsonUtil.mapToCompactJson(credMap),
                HttpReturnCode.SUCCESS);
        } catch (Exception e) {
            logger.error("[createCredentialInvoke]: unknow error. reqCreateCredentialArgs:{}",
                createCredentialFuncArgs,
                e);
        }
        return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.UNKNOWN_ERROR);
    }

    /**
     * Verify the validity of a credential. Need format conversion (UTC date and @context)
     *
     * @param verifyCredentialFuncArgs the credential json args
     * @return the Boolean response data
     */
    public HttpResponseData<String> verifyCredentialInvoke(String verifyCredentialFuncArgs) {
        try {
            Map<String, Object> credMap = (Map<String, Object>) JsonUtil
                .jsonStrToObj(new HashMap<String, Object>(), verifyCredentialFuncArgs);

            // Convert format
            Object context = credMap.get(CredentialConstant.CREDENTIAL_CONTEXT_PORTABLE_JSON_FIELD);
            credMap.remove(CredentialConstant.CREDENTIAL_CONTEXT_PORTABLE_JSON_FIELD);
            if (!StringUtils.isEmpty(context.toString())) {
                credMap.put(ParamKeyConstant.CONTEXT, context);
            } else if (StringUtils.isEmpty(credMap.get(ParamKeyConstant.CONTEXT).toString())) {
                credMap
                    .put(ParamKeyConstant.CONTEXT, CredentialConstant.DEFAULT_CREDENTIAL_CONTEXT);
            }
            String issuanceDate = credMap.get(ParamKeyConstant.ISSURANCE_DATE).toString();
            String expirationDate = credMap.get(ParamKeyConstant.EXPIRATION_DATE).toString();
            credMap.replace(ParamKeyConstant.ISSURANCE_DATE,
                DateUtils.convertUtcDateToTimeStamp(issuanceDate));
            credMap.replace(ParamKeyConstant.EXPIRATION_DATE,
                DateUtils.convertUtcDateToTimeStamp(expirationDate));
            Credential credential = (Credential) JsonUtil
                .jsonStrToObj(new Credential(), JsonUtil.mapToCompactJson(credMap));

            ResponseData<Boolean> responseData = credentialService.verify(credential);
            return new HttpResponseData<>(responseData.getResult().toString(),
                responseData.getErrorCode(), responseData.getErrorMessage());
        } catch (Exception e) {
            logger.error("[verifyCredentialInvoke]: unknow error. reqCredentialArgs:{}",
                verifyCredentialFuncArgs,
                e);
            return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.UNKNOWN_ERROR);
        }
    }

    /**
     * Verify the validity of a credential. Strictly requires non converted Credential as input.
     *
     * @param getCredentialJsonFuncArgs the credential json args
     * @return the Boolean response data
     */
    public HttpResponseData<String> getCredentialJsonInvoke(String getCredentialJsonFuncArgs) {
        try {
            Credential credential = (Credential) JsonUtil
                .jsonStrToObj(new Credential(), getCredentialJsonFuncArgs);
            ResponseData<String> responseData = credentialService.getCredentialJson(credential);
            return new HttpResponseData<>(responseData.getResult(),
                responseData.getErrorCode(), responseData.getErrorMessage());
        } catch (Exception e) {
            logger.error("[verifyCredentialInvoke]: unknow error. reqCredentialArgs:{}",
                getCredentialJsonFuncArgs,
                e);
            return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.UNKNOWN_ERROR);
        }
    }


    /**
     * Generate a credential.
     *
     * @param reqCreateCredentialArgs the args
     * @return the Credential response data
     */
    public ResponseData<CredentialWrapper> createCredential(
        ReqCreateCredentialArgs reqCreateCredentialArgs) {

        ResponseData<CredentialWrapper> response = new ResponseData<>();
        try {
            WeIdPrivateKey weIdPrivateKey = new WeIdPrivateKey();
            weIdPrivateKey.setPrivateKey(reqCreateCredentialArgs.getWeIdPrivateKey());

            CreateCredentialArgs createCredentialArgs = new CreateCredentialArgs();
            createCredentialArgs.setCptId(reqCreateCredentialArgs.getCptId());
            createCredentialArgs.setIssuer(reqCreateCredentialArgs.getIssuer());
            createCredentialArgs.setExpirationDate(reqCreateCredentialArgs.getExpirationDate());
            createCredentialArgs.setWeIdPrivateKey(weIdPrivateKey);
            createCredentialArgs.setClaim(reqCreateCredentialArgs.getClaim());

            response = credentialService.createCredential(createCredentialArgs);
        } catch (Exception e) {
            logger.error("[createCredential]: unknow error. reqCreateCredentialArgs:{}",
                reqCreateCredentialArgs,
                e);
            response.setErrorCode(ErrorCode.UNKNOW_ERROR);
            response.setErrorMessage(ErrorCode.UNKNOW_ERROR.getCodeDesc());
        }
        return response;
    }

    /**
     * Verify the validity of a credential without public key provided.
     *
     * @param reqCredentialArgs the args
     * @return the Boolean response data
     */
    public ResponseData<Boolean> verifyCredential(ReqCredentialArgs reqCredentialArgs) {

        ResponseData<Boolean> response = new ResponseData<Boolean>();
        try {

            Credential credential = new Credential();
            credential.setContext(reqCredentialArgs.getContext());
            credential.setId(reqCredentialArgs.getId());
            credential.setCptId(reqCredentialArgs.getCptId());
            credential.setExpirationDate(reqCredentialArgs.getExpirationDate());
            credential.setIssuranceDate(reqCredentialArgs.getIssuranceDate());
            credential.setIssuer(reqCredentialArgs.getIssuer());
            credential.setSignature(reqCredentialArgs.getSignature());
            credential.setClaim(reqCredentialArgs.getClaim());

            response = credentialService.verify(credential);
        } catch (Exception e) {
            logger.error("[verifyCredential]: unknow error. reqCredentialArgs:{}",
                reqCredentialArgs,
                e);
            response.setErrorCode(ErrorCode.UNKNOW_ERROR);
            response.setErrorMessage(ErrorCode.UNKNOW_ERROR.getCodeDesc());
        }
        return response;
    }

}
