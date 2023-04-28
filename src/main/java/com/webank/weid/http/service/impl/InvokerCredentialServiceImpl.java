/*
 *       Copyright© (2019) WeBank Co., Ltd.
 *
 *       This file is part of weid-http-service.
 *
 *       weid-http-service is free software: you can redistribute it and/or modify
 *       it under the terms of the GNU Lesser General Public License as published by
 *       the Free Software Foundation, either version 3 of the License, or
 *       (at your option) any later version.
 *
 *       weid-http-service is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU Lesser General Public License for more details.
 *
 *       You should have received a copy of the GNU Lesser General Public License
 *       along with weid-http-service.  If not, see <https://www.gnu.org/licenses/>.
 */


package com.webank.weid.http.service.impl;

import static com.webank.weid.util.CredentialPojoUtils.getLiteCredentialThumbprintWithoutSig;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webank.weid.constant.CredentialConstant;
import com.webank.weid.constant.CredentialType;
import com.webank.weid.blockchain.constant.ErrorCode;
import com.webank.weid.constant.ParamKeyConstant;
import com.webank.weid.exception.InitWeb3jException;
import com.webank.weid.exception.LoadContractException;
import com.webank.weid.http.constant.HttpReturnCode;
import com.webank.weid.http.constant.WeIdentityParamKeyConstant;
import com.webank.weid.http.protocol.request.InputArg;
import com.webank.weid.http.protocol.response.HttpResponseData;
import com.webank.weid.http.service.BaseService;
import com.webank.weid.http.service.InvokerCredentialService;
import com.webank.weid.http.util.JsonUtil;
import com.webank.weid.http.util.KeyUtil;
import com.webank.weid.protocol.base.Credential;
import com.webank.weid.protocol.base.CredentialPojo;
import com.webank.weid.protocol.base.WeIdAuthentication;
import com.webank.weid.protocol.base.WeIdPrivateKey;
import com.webank.weid.protocol.request.CreateCredentialArgs;
import com.webank.weid.protocol.request.CreateCredentialPojoArgs;
import com.webank.weid.blockchain.protocol.response.ResponseData;
import com.webank.weid.service.rpc.CredentialPojoService;
import com.webank.weid.service.rpc.CredentialService;
import com.webank.weid.service.impl.CredentialPojoServiceImpl;
import com.webank.weid.service.impl.CredentialServiceImpl;
import com.webank.weid.util.CredentialPojoUtils;
import com.webank.weid.util.CredentialUtils;
import com.webank.weid.util.DataToolUtils;
import com.webank.weid.util.DateUtils;
import com.webank.weid.util.WeIdUtils;
import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InvokerCredentialServiceImpl extends BaseService implements InvokerCredentialService {

    private Logger logger = LoggerFactory.getLogger(InvokerCredentialServiceImpl.class);

    private CredentialService credentialService = new CredentialServiceImpl();
    private CredentialPojoService credentialPojoService = new CredentialPojoServiceImpl();

    /**
     * Generate a credential for client to sign. The signature field is null, and both full claim and claimHash will be returned. The returned json
     * String is an key-ordered compact json.
     *
     * @param createCredentialFuncArgs the functionArgs
     * @return the Map contains Credential content and claimHash.
     */
    @Override
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
                if (cptId < 1) {
                    return new HttpResponseData<>(null, ErrorCode.CPT_ID_ILLEGAL.getCode(),
                        ErrorCode.CPT_ID_ILLEGAL.getCodeDesc());
                }
            } catch (Exception e) {
                return new HttpResponseData<>(null, HttpReturnCode.VALUE_FORMAT_ILLEGAL.getCode(),
                    HttpReturnCode.VALUE_FORMAT_ILLEGAL.getCodeDesc() + ": CPT ID");
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
            if (!WeIdUtils.isWeIdValid(issuerNode.textValue())) {
                return new HttpResponseData<>(null, ErrorCode.CREDENTIAL_ISSUER_INVALID.getCode(),
                    ErrorCode.CREDENTIAL_ISSUER_INVALID.getCodeDesc());
            }
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
                if (claimMap.size() < 1) {
                    return new HttpResponseData<>(null,
                        ErrorCode.CREDENTIAL_CLAIM_DATA_ILLEGAL.getCode(),
                        ErrorCode.CREDENTIAL_CLAIM_DATA_ILLEGAL.getCodeDesc());
                }
            } catch (Exception e) {
                return new HttpResponseData<>(null,
                    ErrorCode.CREDENTIAL_CLAIM_DATA_ILLEGAL.getCode(),
                    ErrorCode.CREDENTIAL_CLAIM_DATA_ILLEGAL.getCodeDesc());
            }
            credential.setClaim(claimMap);

            // check validity 1st round: the create args with an arbitrary private key
            WeIdPrivateKey weIdPrivateKey = new WeIdPrivateKey();
            weIdPrivateKey.setPrivateKey("111111");
            CreateCredentialArgs args = CredentialUtils.extractCredentialMetadata(credential);
            args.setWeIdPrivateKey(weIdPrivateKey);
            ErrorCode errorCode = CredentialUtils.isCreateCredentialArgsValid(args);
            if (errorCode.getCode() != ErrorCode.SUCCESS.getCode()) {
                return new HttpResponseData<>(null, errorCode.getCode(),
                    errorCode.getCodeDesc());
            }

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
                String privateKey = KeyUtil
                    .getPrivateKeyByWeId(KeyUtil.SDK_PRIVKEY_PATH, keyIndexNode.textValue());
                if (!KeyUtil.isPrivateKeyLengthValid(privateKey)) {
                    return new HttpResponseData<>(null, HttpReturnCode.INVOKER_ILLEGAL);
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
    @Override
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
                credMap = JsonUtil.reformatCredentialPojoToJson(credMap);
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
        } catch (LoadContractException e) {
            return new HttpResponseData<>(null, HttpReturnCode.CONTRACT_ERROR.getCode(), HttpReturnCode.CONTRACT_ERROR.getCodeDesc());
        } catch (InitWeb3jException e) {
            return new HttpResponseData<>(null, HttpReturnCode.WEB3J_ERROR.getCode(), HttpReturnCode.WEB3J_ERROR.getCodeDesc());
        } catch (Exception e) {
            logger.error("[verifyCredentialInvoke]: SDK error. reqCredentialArgs:{}",
                verifyCredentialFuncArgs,
                e);
            return new HttpResponseData<>(null, HttpReturnCode.WEID_SDK_ERROR.getCode(),
                HttpReturnCode.WEID_SDK_ERROR.getCodeDesc().concat(e.getMessage()));
        }
    }

    @Override
    public HttpResponseData<Object> createCredentialPojoInvoke(InputArg createCredentialPojoFuncArgs) {
        JsonNode cptIdNode;
        JsonNode issuerNode;
        JsonNode expirationDateNode;
        JsonNode claimNode;
        try {
            JsonNode functionArgNode = new ObjectMapper()
                .readTree(createCredentialPojoFuncArgs.getFunctionArg());
            cptIdNode = functionArgNode.get(ParamKeyConstant.CPT_ID);
            issuerNode = functionArgNode.get(ParamKeyConstant.ISSUER);
            expirationDateNode = functionArgNode.get(ParamKeyConstant.EXPIRATION_DATE);
            claimNode = functionArgNode.get(ParamKeyConstant.CLAIM);
            if (cptIdNode == null || StringUtils.isEmpty(cptIdNode.toString())
                || issuerNode == null || StringUtils.isEmpty(issuerNode.textValue())
                || expirationDateNode == null || StringUtils.isEmpty(expirationDateNode.textValue())
                || claimNode == null || StringUtils.isEmpty(claimNode.toString())) {
                return new HttpResponseData<>(null, HttpReturnCode.INPUT_NULL);
            }
        } catch (Exception e) {
            logger.error("[createCredentialPojoInvoke]: input args error: {}", createCredentialPojoFuncArgs, e);
            return new HttpResponseData<>(null, HttpReturnCode.VALUE_FORMAT_ILLEGAL);
        }

        Integer cptId;
        try {
            cptId = Integer.valueOf(JsonUtil.removeDoubleQuotes(cptIdNode.toString()));
            if (cptId < 1) {
                return new HttpResponseData<>(null, ErrorCode.CPT_ID_ILLEGAL.getCode(),
                    ErrorCode.CPT_ID_ILLEGAL.getCodeDesc());
            }
        } catch (Exception e) {
            return new HttpResponseData<>(null, HttpReturnCode.VALUE_FORMAT_ILLEGAL.getCode(),
                HttpReturnCode.VALUE_FORMAT_ILLEGAL.getCodeDesc() + ": CPT ID");
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

        CredentialPojo credential = new CredentialPojo();
        credential.setId(UUID.randomUUID().toString());
        credential.setCptId(cptId);
        if (!WeIdUtils.isWeIdValid(issuerNode.textValue())) {
            return new HttpResponseData<>(null, ErrorCode.CREDENTIAL_ISSUER_INVALID.getCode(),
                ErrorCode.CREDENTIAL_ISSUER_INVALID.getCodeDesc());
        }
        credential.setIssuer(issuerNode.textValue());
        credential.setExpirationDate(expirationDate);
        credential.setContext(CredentialConstant.DEFAULT_CREDENTIAL_CONTEXT);
        credential.setIssuanceDate(DateUtils.getNoMillisecondTimeStamp());
        Map<String, Object> claimMap;
        try {
            claimMap = (Map<String, Object>) JsonUtil
                .jsonStrToObj(new HashMap<String, Object>(), claimNode.toString());
            if (claimMap.size() < 1) {
                return new HttpResponseData<>(null,
                    ErrorCode.CREDENTIAL_CLAIM_DATA_ILLEGAL.getCode(),
                    ErrorCode.CREDENTIAL_CLAIM_DATA_ILLEGAL.getCodeDesc());
            }
        } catch (Exception e) {
            return new HttpResponseData<>(null,
                ErrorCode.CREDENTIAL_CLAIM_DATA_ILLEGAL.getCode(),
                ErrorCode.CREDENTIAL_CLAIM_DATA_ILLEGAL.getCodeDesc());
        }
        credential.setClaim(claimMap);

        WeIdAuthentication weIdAuthentication;
        try {
            JsonNode txnArgNode = new ObjectMapper().readTree(createCredentialPojoFuncArgs.getTransactionArg());
            JsonNode keyIndexNode = txnArgNode.get(WeIdentityParamKeyConstant.KEY_INDEX);
            String privateKey = KeyUtil
                .getPrivateKeyByWeId(KeyUtil.SDK_PRIVKEY_PATH, keyIndexNode.textValue());
            weIdAuthentication = KeyUtil.buildWeIdAuthenticationFromPrivKey(privateKey);
            if (weIdAuthentication == null) {
                return new HttpResponseData<>(null, HttpReturnCode.INVOKER_ILLEGAL);
            }
        } catch (Exception e) {
            return new HttpResponseData<>(null, ErrorCode.CREDENTIAL_PRIVATE_KEY_NOT_EXISTS.getCode(),
                ErrorCode.CREDENTIAL_PRIVATE_KEY_NOT_EXISTS.getCodeDesc());
        }

        // Client-side check of validity
        CreateCredentialPojoArgs createArg = new CreateCredentialPojoArgs();
        createArg.setClaim(claimMap);
        createArg.setCptId(cptId);
        createArg.setIssuer(issuerNode.textValue());
        createArg.setIssuanceDate(credential.getIssuanceDate());
        createArg.setExpirationDate(expirationDate);
        createArg.setContext(credential.getContext());
        createArg.setId(credential.getId());
        createArg.setWeIdAuthentication(weIdAuthentication);
        ErrorCode errorCode = CredentialPojoUtils.isCreateCredentialPojoArgsValid(createArg);
        if (errorCode.getCode() != ErrorCode.SUCCESS.getCode()) {
            return new HttpResponseData<>(null, errorCode.getCode(),
                errorCode.getCodeDesc());
        }
        ResponseData<CredentialPojo> createResp = credentialPojoService.createCredential(createArg);
        Map<String, Object> credMap = (Map<String, Object>) JsonUtil.jsonStrToObj(new HashMap<String, Object>(),
            DataToolUtils.serialize(createResp.getResult()));
        return new HttpResponseData<>(credMap, createResp.getErrorCode(), createResp.getErrorMessage());
    }

    @Override
    public HttpResponseData<Object> createCredentialPojoAndEncryptInvoke(InputArg createCredentialPojoFuncArgs) {
        JsonNode cptIdNode;
        JsonNode issuerNode;
        JsonNode expirationDateNode;
        JsonNode claimNode;
        JsonNode typeNode;
        try {
            JsonNode functionArgNode = new ObjectMapper()
                .readTree(createCredentialPojoFuncArgs.getFunctionArg());
            cptIdNode = functionArgNode.get(ParamKeyConstant.CPT_ID);
            issuerNode = functionArgNode.get(ParamKeyConstant.ISSUER);
            expirationDateNode = functionArgNode.get(ParamKeyConstant.EXPIRATION_DATE);
            claimNode = functionArgNode.get(ParamKeyConstant.CLAIM);
            typeNode = functionArgNode.get(ParamKeyConstant.PROOF_TYPE);
            if (cptIdNode == null || StringUtils.isEmpty(cptIdNode.toString())
                || issuerNode == null || StringUtils.isEmpty(issuerNode.textValue())
                || expirationDateNode == null || StringUtils.isEmpty(expirationDateNode.toString())
                || claimNode == null || StringUtils.isEmpty(claimNode.toString())
                || typeNode == null || StringUtils.isEmpty(typeNode.textValue())) {
                return new HttpResponseData<>(null, HttpReturnCode.INPUT_NULL);
            }
        } catch (Exception e) {
            logger.error("[createCredentialPojoInvoke]: input args error: {}", createCredentialPojoFuncArgs, e);
            return new HttpResponseData<>(null, HttpReturnCode.VALUE_FORMAT_ILLEGAL);
        }

        Integer cptId;
        try {
            cptId = Integer.valueOf(JsonUtil.removeDoubleQuotes(cptIdNode.toString()));
            if (cptId < 1) {
                return new HttpResponseData<>(null, ErrorCode.CPT_ID_ILLEGAL.getCode(),
                    ErrorCode.CPT_ID_ILLEGAL.getCodeDesc());
            }
        } catch (Exception e) {
            return new HttpResponseData<>(null, HttpReturnCode.VALUE_FORMAT_ILLEGAL.getCode(),
                HttpReturnCode.VALUE_FORMAT_ILLEGAL.getCodeDesc() + ": CPT ID");
        }

        Long expirationDate;
        try {
            expirationDate = Long.valueOf(JsonUtil.removeDoubleQuotes(expirationDateNode.toString()));
            if (expirationDate <= 0) {
                return new HttpResponseData<>(null,
                    ErrorCode.CREDENTIAL_EXPIRE_DATE_ILLEGAL.getCode(),
                    ErrorCode.CREDENTIAL_EXPIRE_DATE_ILLEGAL.getCodeDesc());
            }
        } catch (Exception e) {
            return new HttpResponseData<>(null,
                ErrorCode.CREDENTIAL_EXPIRE_DATE_ILLEGAL.getCode(),
                ErrorCode.CREDENTIAL_EXPIRE_DATE_ILLEGAL.getCodeDesc());
        }

        Map<String, Object> claimMap;
        try {
            claimMap = (Map<String, Object>) JsonUtil
                .jsonStrToObj(new HashMap<String, Object>(), claimNode.toString());
            if (claimMap.size() < 1) {
                return new HttpResponseData<>(null,
                    ErrorCode.CREDENTIAL_CLAIM_DATA_ILLEGAL.getCode(),
                    ErrorCode.CREDENTIAL_CLAIM_DATA_ILLEGAL.getCodeDesc());
            }
        } catch (Exception e) {
            return new HttpResponseData<>(null,
                ErrorCode.CREDENTIAL_CLAIM_DATA_ILLEGAL.getCode(),
                ErrorCode.CREDENTIAL_CLAIM_DATA_ILLEGAL.getCodeDesc());
        }
        CreateCredentialPojoArgs args = new CreateCredentialPojoArgs();
        args.setClaim(claimMap);
        if (!WeIdUtils.isWeIdValid(issuerNode.textValue())) {
            return new HttpResponseData<>(null, ErrorCode.CREDENTIAL_ISSUER_INVALID.getCode(),
                ErrorCode.CREDENTIAL_ISSUER_INVALID.getCodeDesc());
        }
        args.setIssuer(issuerNode.textValue());
        args.setCptId(cptId);
        args.setIssuanceDate(DateUtils.getNoMillisecondTimeStamp());
        args.setExpirationDate(expirationDate);
        args.setContext(CredentialConstant.DEFAULT_CREDENTIAL_CONTEXT);
        args.setId(UUID.randomUUID().toString());

        WeIdAuthentication weIdAuthentication;
        String privateKey;
        try {
            JsonNode txnArgNode = new ObjectMapper().readTree(createCredentialPojoFuncArgs.getTransactionArg());
            JsonNode keyIndexNode = txnArgNode.get(WeIdentityParamKeyConstant.KEY_INDEX);
            privateKey = KeyUtil
                .getPrivateKeyByWeId(KeyUtil.SDK_PRIVKEY_PATH, keyIndexNode.textValue());
            weIdAuthentication = KeyUtil.buildWeIdAuthenticationFromPrivKey(privateKey);
            if (weIdAuthentication == null) {
                return new HttpResponseData<>(null, HttpReturnCode.INVOKER_ILLEGAL);
            }
        } catch (Exception e) {
            return new HttpResponseData<>(null, ErrorCode.CREDENTIAL_PRIVATE_KEY_NOT_EXISTS.getCode(),
                ErrorCode.CREDENTIAL_PRIVATE_KEY_NOT_EXISTS.getCodeDesc());
        }

        args.setWeIdAuthentication(weIdAuthentication);
        if (typeNode.textValue().contains("lite")) {
            args.setType(CredentialType.LITE1);
        } else {
            args.setType(CredentialType.ORIGINAL);
        }

        ResponseData<CredentialPojo> createResp = credentialPojoService.createCredential(args);
        CredentialPojo credentialPojo = createResp.getResult();
        logger.info("Thumbprint without sig: ", getLiteCredentialThumbprintWithoutSig(credentialPojo));
        logger.info("Hash: " + DataToolUtils.hash(getLiteCredentialThumbprintWithoutSig(credentialPojo)));

        String toBeEncryptedCredentialPojo = credentialPojo.toJson();
        System.out.println("Lite Credential toJson: " + toBeEncryptedCredentialPojo);
        logger.info("Lite CredentialPojo Json: ", toBeEncryptedCredentialPojo);
        //TODO:encrypt the data
        /*ECKeyPair ecKeyPair = ECKeyPair.create(new BigInteger(privateKey));
        ECCEncrypt encrypt = new ECCEncrypt(ecKeyPair.getPublicKey());
        try {
            byte[] encryptData = encrypt.encrypt(toBeEncryptedCredentialPojo.getBytes("utf-8"));
            String hexEncryptedData = Hex.toHexString(encryptData);
            return new HttpResponseData<>(toBeEncryptedCredentialPojo, HttpReturnCode.SUCCESS);
        } catch (Exception e) {
            logger.error("Error when creating credential and encode: ", e.getMessage());
            return new HttpResponseData<>(null, HttpReturnCode.UNKNOWN_ERROR.getCode(),
                e.getMessage());
        }*/
        return new HttpResponseData<>(toBeEncryptedCredentialPojo, HttpReturnCode.SUCCESS);
    }

    @Override
    public HttpResponseData<Object> eccEncrypt(InputArg encryptFuncArgs) {
        JsonNode dataNode;
        JsonNode keyIndexNode;
        try {
            JsonNode functionArgNode = new ObjectMapper().readTree(encryptFuncArgs.getFunctionArg());
            dataNode = functionArgNode.get(WeIdentityParamKeyConstant.TRANSACTION_DATA);
            if (dataNode == null || StringUtils.isEmpty(dataNode.textValue())) {
                logger.error("Null or empty json node: data");
                return new HttpResponseData<>(null, HttpReturnCode.INPUT_NULL);
            }
            JsonNode txnArgNode = new ObjectMapper()
                .readTree(encryptFuncArgs.getTransactionArg());
            keyIndexNode = txnArgNode.get(WeIdentityParamKeyConstant.KEY_INDEX);
            if (keyIndexNode == null || StringUtils.isEmpty(keyIndexNode.textValue())) {
                logger.error("Null or empty json node: keyIndex");
                return new HttpResponseData<>(null, HttpReturnCode.INPUT_NULL);
            }
        } catch (Exception e) {
            logger.error("[createCredentialPojoInvoke]: input args error: {}", encryptFuncArgs, e);
            return new HttpResponseData<>(null, HttpReturnCode.VALUE_FORMAT_ILLEGAL);
        }
        //TODO:encrypt the data
        /*String privateKey = KeyUtil
            .getPrivateKeyByWeId(KeyUtil.SDK_PRIVKEY_PATH, keyIndexNode.textValue());
        ECKeyPair ecKeyPair = ECKeyPair.create(new BigInteger(privateKey));
        ECCEncrypt encrypt = new ECCEncrypt(ecKeyPair.getPublicKey());
        try {
            byte[] nonEncryptedData = dataNode.textValue().getBytes();
            byte[] encryptedData;
            System.out.println(dataNode.textValue());
            try {
                encryptedData = encrypt.encrypt(nonEncryptedData);
            } catch (Exception e) {
                logger.error("Error: " + e.getMessage());
                return new HttpResponseData<>(null, HttpReturnCode.UNKNOWN_ERROR.getCode(), e.getMessage());
            }
            String hexEncryptedData = Hex.toHexString(encryptedData);
            return new HttpResponseData<>(hexEncryptedData, HttpReturnCode.SUCCESS);
        } catch (Exception e) {
            logger.error("Error encrypt: " + e.getMessage());
            return new HttpResponseData<>(null, HttpReturnCode.UNKNOWN_ERROR.getCode(), e.getMessage());
        }*/
        return new HttpResponseData<>(dataNode.textValue(), HttpReturnCode.SUCCESS);
    }

    @Override
    public HttpResponseData<Object> eccDecrypt(InputArg decryptFuncArgs) {
        JsonNode dataNode;
        JsonNode keyIndexNode;
        try {
            JsonNode functionArgNode = new ObjectMapper().readTree(decryptFuncArgs.getFunctionArg());
            dataNode = functionArgNode.get(WeIdentityParamKeyConstant.TRANSACTION_DATA);
            if (dataNode == null || StringUtils.isEmpty(dataNode.textValue())) {
                return new HttpResponseData<>(null, HttpReturnCode.INPUT_NULL);
            }
            JsonNode txnArgNode = new ObjectMapper()
                .readTree(decryptFuncArgs.getTransactionArg());
            keyIndexNode = txnArgNode.get(WeIdentityParamKeyConstant.KEY_INDEX);
            if (keyIndexNode == null || StringUtils.isEmpty(keyIndexNode.textValue())) {
                logger.error("Null or empty json node: keyIndex");
                return new HttpResponseData<>(null, HttpReturnCode.INPUT_NULL);
            }
        } catch (Exception e) {
            logger.error("[createCredentialPojoInvoke]: input args error: {}", decryptFuncArgs, e);
            return new HttpResponseData<>(null, HttpReturnCode.VALUE_FORMAT_ILLEGAL);
        }
        /*String privateKey = KeyUtil
            .getPrivateKeyByWeId(KeyUtil.SDK_PRIVKEY_PATH, keyIndexNode.textValue());
        logger.info("Privatekey: " + privateKey);
        System.out.println(privateKey);
        ECKeyPair ecKeyPair = ECKeyPair.create(new BigInteger(privateKey));
        ECCDecrypt decrypt = new ECCDecrypt(ecKeyPair.getPrivateKey());
        byte[] nonDecryptedData = Hex.decode(dataNode.textValue());
        try {
            byte[] decryptData;
            try {
                decryptData = decrypt.decrypt(nonDecryptedData);
            } catch (Exception e) {
                logger.error("Failed to decrypt: " + e.getMessage());
                return new HttpResponseData<>(null, HttpReturnCode.UNKNOWN_ERROR.getCode(),
                    e.getMessage() + " (private key might mismatch or be mal-formatted)");
            }
            String resp = new String(decryptData);
            System.out.println(resp);
            return new HttpResponseData<>(resp, HttpReturnCode.SUCCESS);
        } catch (Exception e) {
            logger.error("Error decrypt: " + e.getMessage());
            return new HttpResponseData<>(null, HttpReturnCode.UNKNOWN_ERROR.getCode(),
                e.getMessage());
        }*/
        return new HttpResponseData<>(dataNode.textValue(), HttpReturnCode.SUCCESS);
    }

    @Override
    public HttpResponseData<Boolean> verifyCredentialPojoInvoke(InputArg verifyCredentialPojoFuncArgs) {
        CredentialPojo credential;
        try {
            credential = DataToolUtils.deserialize(verifyCredentialPojoFuncArgs.getFunctionArg(), CredentialPojo.class);
        } catch (Exception e) {
            try {
                credential = CredentialPojo.fromJson(verifyCredentialPojoFuncArgs.getFunctionArg());
            } catch (Exception ex) {
                logger.error("Input credential format illegal: {}", verifyCredentialPojoFuncArgs);
                return new HttpResponseData<>(null, HttpReturnCode.INPUT_ILLEGAL.getCode(),
                    HttpReturnCode.INPUT_ILLEGAL.getCodeDesc().concat(e.getMessage()));
            }
        }
//        String unifiedSig = TransactionEncoderUtilV2.convertIfGoSigToWeIdJavaSdkSig(credential.getSignature());
//        if (StringUtils.isEmpty(unifiedSig)) {
//            return new HttpResponseData<>(false, ErrorCode.CREDENTIAL_SIGNATURE_BROKEN.getCode(),
//                ErrorCode.CREDENTIAL_SIGNATURE_BROKEN.getCodeDesc());
//        }
//        credential.putProofValue("signatureValue", unifiedSig);
        try {
            ResponseData<Boolean> responseData = credentialPojoService.verify(credential.getIssuer(), credential);
            return new HttpResponseData<>(responseData.getResult(),
                responseData.getErrorCode(), responseData.getErrorMessage());
        } catch (LoadContractException e) {
            return new HttpResponseData<>(null, HttpReturnCode.CONTRACT_ERROR.getCode(), HttpReturnCode.CONTRACT_ERROR.getCodeDesc());
        } catch (InitWeb3jException e) {
            return new HttpResponseData<>(null, HttpReturnCode.WEB3J_ERROR.getCode(), HttpReturnCode.WEB3J_ERROR.getCodeDesc());
        } catch (Exception e) {
            logger.error("[verifyCredentialInvoke]: SDK error. reqCredentialArgs:{}",
                verifyCredentialPojoFuncArgs,
                e);
            return new HttpResponseData<>(null, HttpReturnCode.WEID_SDK_ERROR.getCode(),
                HttpReturnCode.WEID_SDK_ERROR.getCodeDesc().concat(e.getMessage()));
        }
    }
}
