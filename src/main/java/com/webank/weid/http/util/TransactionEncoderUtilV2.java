package com.webank.weid.http.util;

import static com.webank.weid.service.impl.CredentialPojoServiceImpl.generateSalt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webank.weid.config.FiscoConfig;
import com.webank.weid.constant.CredentialConstant;
import com.webank.weid.constant.CredentialConstant.CredentialProofType;
import com.webank.weid.constant.ErrorCode;
import com.webank.weid.constant.ParamKeyConstant;
import com.webank.weid.constant.WeIdConstant;
import com.webank.weid.exception.WeIdBaseException;
import com.webank.weid.http.constant.HttpReturnCode;
import com.webank.weid.http.constant.WeIdentityFunctionNames;
import com.webank.weid.http.protocol.request.InputArg;
import com.webank.weid.http.protocol.response.HttpResponseData;
import com.webank.weid.protocol.base.CredentialPojo;
import com.webank.weid.protocol.request.CreateCredentialPojoArgs;
import com.webank.weid.protocol.response.RsvSignature;
import com.webank.weid.service.BaseService;
import com.webank.weid.util.CredentialPojoUtils;
import com.webank.weid.util.CredentialUtils;
import com.webank.weid.util.DataToolUtils;
import com.webank.weid.util.DateUtils;
import com.webank.weid.util.TransactionUtils;
import com.webank.weid.util.WeIdUtils;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.bcos.web3j.utils.Numeric;
import org.fisco.bcos.web3j.abi.FunctionEncoder;
import org.fisco.bcos.web3j.abi.TypeReference;
import org.fisco.bcos.web3j.abi.datatypes.Address;
import org.fisco.bcos.web3j.abi.datatypes.DynamicBytes;
import org.fisco.bcos.web3j.abi.datatypes.Function;
import org.fisco.bcos.web3j.abi.datatypes.generated.Bytes32;
import org.fisco.bcos.web3j.abi.datatypes.generated.Int256;
import org.fisco.bcos.web3j.abi.datatypes.generated.StaticArray128;
import org.fisco.bcos.web3j.abi.datatypes.generated.StaticArray16;
import org.fisco.bcos.web3j.abi.datatypes.generated.StaticArray8;
import org.fisco.bcos.web3j.abi.datatypes.generated.Uint8;
import org.fisco.bcos.web3j.crypto.ExtendedRawTransaction;
import org.fisco.bcos.web3j.crypto.Sign;
import org.fisco.bcos.web3j.crypto.Sign.SignatureData;
import org.fisco.bcos.web3j.protocol.Web3j;
import org.fisco.bcos.web3j.protocol.core.methods.response.NodeVersion;
import org.fisco.bcos.web3j.protocol.core.methods.response.TransactionReceipt;
import org.fisco.bcos.web3j.rlp.RlpType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionEncoderUtilV2 {

    private static Logger logger = LoggerFactory.getLogger(TransactionEncoderUtilV2.class);

    public static HttpResponseData<String> createEncoder(
        String inputParam,
        String nonce,
        String functionName) {
        FiscoConfig fiscoConfig = new FiscoConfig();
        fiscoConfig.load();
        Function function;
        String to;
        if (functionName.equalsIgnoreCase(WeIdentityFunctionNames.FUNCNAME_CREATE_WEID)) {
            to = fiscoConfig.getWeIdAddress();
            function = buildCreateWeIdFunction(inputParam, functionName);
        } else if (functionName.equalsIgnoreCase(WeIdentityFunctionNames.FUNCNAME_REGISTER_AUTHORITY_ISSUER)) {
            to = fiscoConfig.getIssuerAddress();
            function = buildRegisterAuthorityIssuerFunction(inputParam, functionName);
        } else if (functionName.equalsIgnoreCase(WeIdentityFunctionNames.FUNCCALL_REGISTER_CPT)) {
            to = fiscoConfig.getCptAddress();
            function = buildRegisterCptFunction(inputParam, functionName);
        } else {
            logger.error("Unknown function name: {}", functionName);
            return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.FUNCTION_NAME_ILLEGAL);
        }
        if (function == null) {
            logger.error("Error occurred when building input param with: {} on function name: {}",
                inputParam, functionName);
            return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.INPUT_ILLEGAL);
        }
        String encodeResult = createClientEncodeResult(function, nonce, to, fiscoConfig.getGroupId());
        return new HttpResponseData<>(encodeResult, HttpReturnCode.SUCCESS);
    }

    public static Function buildCreateWeIdFunction(String inputParam, String functionName) {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode inputParamNode;
        try {
            inputParamNode = objectMapper.readTree(inputParam);
        } catch (Exception e) {
            logger.error("Failed to decode JsonInput");
            return null;
        }
        JsonNode publicKeyNode = inputParamNode.get(ParamKeyConstant.PUBLIC_KEY);
        if (publicKeyNode == null) {
            return null;
        }
        String publicKey = publicKeyNode.textValue();
        if (StringUtils.isEmpty(publicKey)) {
            logger.error("[createWeId]: input parameter publickey is null.");
            return null;
        }
        String weId = WeIdUtils.convertPublicKeyToWeId(publicKey);
        String addr = WeIdUtils.convertWeIdToAddress(weId);
        if (!WeIdUtils.isValidAddress(addr)) {
            logger.error("[createWeId]: input parameter publickey is invalid.");
            return null;
        }
        String auth = new StringBuffer()
            .append(publicKey)
            .append(WeIdConstant.SEPARATOR)
            .append(addr)
            .toString();
        return new Function(
            WeIdentityFunctionNames.FUNCNAME_CALL_MAP_V2.get(functionName),
            Arrays.<org.fisco.bcos.web3j.abi.datatypes.Type>asList(
                new Address(addr),
                new DynamicBytes(DataToolUtils.stringToByteArray(auth)),
                new DynamicBytes(DataToolUtils.stringToByteArray(DateUtils.getNoMillisecondTimeStampString())),
                new Int256(BigInteger.valueOf(DateUtils.getNoMillisecondTimeStamp()))
            ),
            Collections.<TypeReference<?>>emptyList());
    }

    public static Function buildRegisterAuthorityIssuerFunction(String inputParam, String functionName) {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode inputParamNode;
        try {
            inputParamNode = objectMapper.readTree(inputParam);
        } catch (Exception e) {
            logger.error("Failed to decode JsonInput");
            return null;
        }
        JsonNode weIdNode = inputParamNode.get(ParamKeyConstant.WEID);
        JsonNode nameNode = inputParamNode.get(ParamKeyConstant.AUTHORITY_ISSUER_NAME);
        if (weIdNode == null || nameNode == null) {
            return null;
        }
        String weId = weIdNode.textValue();
        if (!WeIdUtils.isWeIdValid(weId)) {
            return null;
        }
        String name = nameNode.textValue();
        if (StringUtils.isEmpty(name)
            || name.length() > WeIdConstant.MAX_AUTHORITY_ISSUER_NAME_LENGTH) {
            logger.error("Input cpt publisher : {} is invalid.", name);
            return null;
        }
        String weAddress = WeIdUtils.convertWeIdToAddress(weId);
        List<byte[]> stringAttributes = new ArrayList<byte[]>();
        stringAttributes.add(name.getBytes());
        List<BigInteger> longAttributes = new ArrayList<>();
        Long createDate = DateUtils.getNoMillisecondTimeStamp();
        longAttributes.add(BigInteger.valueOf(createDate));
        String accValue = "1";
        return new Function(
            WeIdentityFunctionNames.FUNCNAME_CALL_MAP_V2.get(functionName),
            Arrays.<org.fisco.bcos.web3j.abi.datatypes.Type>asList(
                new Address(weAddress),
                new StaticArray16<Bytes32>(
                    org.fisco.bcos.web3j.abi.Utils.typeMap(
                        DataToolUtils.bytesArrayListToBytes32ArrayList(
                            stringAttributes,
                            WeIdConstant.AUTHORITY_ISSUER_ARRAY_LEGNTH
                        ), Bytes32.class)),
                new StaticArray16<Int256>(
                    org.fisco.bcos.web3j.abi.Utils.typeMap(
                        DataToolUtils.listToListBigInteger(
                            longAttributes,
                            WeIdConstant.AUTHORITY_ISSUER_ARRAY_LEGNTH
                        ),
                        Int256.class)),
                new DynamicBytes(accValue.getBytes(StandardCharsets.UTF_8))
            ),
            Collections.<TypeReference<?>>emptyList());
    }

    public static Function buildRegisterCptFunction(String inputParam, String functionName) {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode inputParamNode;
        try {
            inputParamNode = objectMapper.readTree(inputParam);
        } catch (Exception e) {
            logger.error("Failed to decode JsonInput");
            return null;
        }
        JsonNode weIdNode = inputParamNode.get(ParamKeyConstant.WEID);
        JsonNode cptJsonSchemaNode = inputParamNode.get(ParamKeyConstant.CPT_JSON_SCHEMA);
        JsonNode cptSignatureNode = inputParamNode.get(ParamKeyConstant.CPT_SIGNATURE);
        if (weIdNode == null || cptJsonSchemaNode == null || cptSignatureNode == null) {
            return null;
        }

        String weId = weIdNode.textValue();
        if (!WeIdUtils.isWeIdValid(weId)) {
            logger.error("WeID illegal: {}", weId);
            return null;
        }

        String cptJsonSchema = cptJsonSchemaNode.toString();
        String cptJsonSchemaNew = TransactionUtils.complementCptJsonSchema(cptJsonSchema);
        try {
            if (StringUtils.isEmpty(cptJsonSchemaNew)
                || !DataToolUtils.isCptJsonSchemaValid(cptJsonSchemaNew)) {
                logger.error("Input cpt json schema : {} is invalid.", cptJsonSchemaNew);
                return null;
            }
        } catch (Exception e) {
            logger.error("Input cpt json schema : {} is invalid.", cptJsonSchemaNew);
            return null;
        }
        String cptSignature = cptSignatureNode.textValue();
        if (!DataToolUtils.isValidBase64String(cptSignature)) {
            logger.error("Input cpt signature invalid: {}", cptSignature);
            return null;
        }
        RsvSignature rsvSignature =
            DataToolUtils.convertSignatureDataToRsv(
                DataToolUtils.convertBase64StringToSignatureData(cptSignature)
            );
        String weAddress = WeIdUtils.convertWeIdToAddress(weId);
        List<byte[]> byteArray = new ArrayList<>();
        return new Function(
            WeIdentityFunctionNames.FUNCNAME_CALL_MAP_V2.get(functionName),
            Arrays.<org.fisco.bcos.web3j.abi.datatypes.Type>asList(
                new Address(weAddress),
                new StaticArray8<Int256>(
                    org.fisco.bcos.web3j.abi.Utils.typeMap(
                        DataToolUtils.listToListBigInteger(
                            DataToolUtils.getParamCreatedList(WeIdConstant.CPT_LONG_ARRAY_LENGTH),
                            WeIdConstant.CPT_LONG_ARRAY_LENGTH
                        ), Int256.class)),
                new StaticArray8<Bytes32>(
                    org.fisco.bcos.web3j.abi.Utils.typeMap(
                        DataToolUtils.bytesArrayListToBytes32ArrayList(
                            byteArray,
                            WeIdConstant.CPT_STRING_ARRAY_LENGTH
                        ), Bytes32.class)),
                new StaticArray128<Bytes32>(
                    org.fisco.bcos.web3j.abi.Utils.typeMap(
                        DataToolUtils.stringToByte32ArrayList(
                            cptJsonSchemaNew,
                            WeIdConstant.JSON_SCHEMA_ARRAY_LENGTH
                        ), Bytes32.class)),
                new Uint8(rsvSignature.getV().getValue()),
                new Bytes32(rsvSignature.getR().getValue()),
                new Bytes32(rsvSignature.getS().getValue())
            ),
            Collections.<TypeReference<?>>emptyList());
    }

    public static String createTxnHex(String encodedSig, String nonce, String to, String data) {
        SignatureData sigData = TransactionEncoderUtilV2
            .simpleSignatureDeserialization(DataToolUtils.base64Decode(encodedSig.getBytes()));
        FiscoConfig fiscoConfig = new FiscoConfig();
        fiscoConfig.load();
        ExtendedRawTransaction rawTransaction = TransactionEncoderUtilV2.buildRawTransaction(nonce,
            fiscoConfig.getGroupId(), data, to);
        byte[] encodedSignedTxn = TransactionEncoderUtilV2.encode(rawTransaction, sigData);
        return Numeric.toHexString(encodedSignedTxn);
    }

    public static String createClientEncodeResult(Function function, String nonce, String to, String groupId) {
        // 1. encode the Function
        String data = FunctionEncoder.encode(function);
        // 2. server generate encodedTransaction
        ExtendedRawTransaction rawTransaction = TransactionEncoderUtilV2.buildRawTransaction(nonce,
            groupId, data, to);
        byte[] encodedTransaction = TransactionEncoderUtilV2.encode(rawTransaction);
        // 3. server sends encodeTransaction (in base64) and data back to client
        return TransactionEncoderUtil.getEncodeOutput(encodedTransaction, data);
    }

    public static BigInteger getNonce() {
        Random r = new SecureRandom();
        BigInteger randomid = new BigInteger(250, r);
        return randomid;
    }

    public static byte[] encode(ExtendedRawTransaction rawTransaction) {
        return encode(rawTransaction, null);
    }

    public static byte[] encode(
        ExtendedRawTransaction rawTransaction, Sign.SignatureData signatureData) {
        List<RlpType> values = asRlpValues(rawTransaction, signatureData);
        org.fisco.bcos.web3j.rlp.RlpList rlpList = new org.fisco.bcos.web3j.rlp.RlpList(values);
        return org.fisco.bcos.web3j.rlp.RlpEncoder.encode(rlpList);
    }

    static List<org.fisco.bcos.web3j.rlp.RlpType> asRlpValues(
        ExtendedRawTransaction rawTransaction, Sign.SignatureData signatureData) {
        List<org.fisco.bcos.web3j.rlp.RlpType> result = new ArrayList<>();
        result.add(org.fisco.bcos.web3j.rlp.RlpString.create(rawTransaction.getRandomid()));
        result.add(org.fisco.bcos.web3j.rlp.RlpString.create(rawTransaction.getGasPrice()));
        result.add(org.fisco.bcos.web3j.rlp.RlpString.create(rawTransaction.getGasLimit()));
        result.add(org.fisco.bcos.web3j.rlp.RlpString.create(rawTransaction.getBlockLimit()));
        // an empty to address (contract creation) should not be encoded as a numeric 0 value
        String to = rawTransaction.getTo();
        if (to != null && to.length() > 0) {
            // addresses that start with zeros should be encoded with the zeros included, not
            // as numeric values
            result.add(org.fisco.bcos.web3j.rlp.RlpString.create(org.fisco.bcos.web3j.utils.Numeric.hexStringToByteArray(to)));
        } else {
            result.add(org.fisco.bcos.web3j.rlp.RlpString.create(""));
        }

        result.add(org.fisco.bcos.web3j.rlp.RlpString.create(rawTransaction.getValue()));

        // value field will already be hex encoded, so we need to convert into binary first
        byte[] data = org.fisco.bcos.web3j.utils.Numeric.hexStringToByteArray(rawTransaction.getData());
        result.add(org.fisco.bcos.web3j.rlp.RlpString.create(data));

        // add extra data!!!

        result.add(org.fisco.bcos.web3j.rlp.RlpString.create(rawTransaction.getFiscoChainId()));
        result.add(org.fisco.bcos.web3j.rlp.RlpString.create(rawTransaction.getGroupId()));
        if (rawTransaction.getExtraData() == null) {
            result.add(org.fisco.bcos.web3j.rlp.RlpString.create(""));
        } else {
            result.add(
                org.fisco.bcos.web3j.rlp.RlpString.create(org.fisco.bcos.web3j.utils.Numeric.hexStringToByteArray(rawTransaction.getExtraData())));
        }
        if (signatureData != null) {
            if (org.fisco.bcos.web3j.crypto.EncryptType.encryptType == 1) {
                result.add(org.fisco.bcos.web3j.rlp.RlpString.create(org.fisco.bcos.web3j.utils.Bytes.trimLeadingZeroes(signatureData.getPub())));
                // logger.debug("RLP-Pub:{},RLP-PubLen:{}",Hex.toHexString(signatureData.getPub()),signatureData.getPub().length);
                result.add(org.fisco.bcos.web3j.rlp.RlpString.create(org.fisco.bcos.web3j.utils.Bytes.trimLeadingZeroes(signatureData.getR())));
                // logger.debug("RLP-R:{},RLP-RLen:{}",Hex.toHexString(signatureData.getR()),signatureData.getR().length);
                result.add(org.fisco.bcos.web3j.rlp.RlpString.create(org.fisco.bcos.web3j.utils.Bytes.trimLeadingZeroes(signatureData.getS())));
                // logger.debug("RLP-S:{},RLP-SLen:{}",Hex.toHexString(signatureData.getS()),signatureData.getS().length);
            } else {
                result.add(org.fisco.bcos.web3j.rlp.RlpString.create(signatureData.getV()));
                result.add(org.fisco.bcos.web3j.rlp.RlpString.create(org.fisco.bcos.web3j.utils.Bytes.trimLeadingZeroes(signatureData.getR())));
                result.add(org.fisco.bcos.web3j.rlp.RlpString.create(org.fisco.bcos.web3j.utils.Bytes.trimLeadingZeroes(signatureData.getS())));
            }
        }
        return result;
    }

    public static ExtendedRawTransaction buildRawTransaction(String nonce, String groupId, String data, String to) {
        ExtendedRawTransaction rawTransaction =
            ExtendedRawTransaction.createTransaction(
                new BigInteger(nonce),
                new BigInteger("99999999999"),
                new BigInteger("99999999999"),
                getBlocklimitV2(),
                to, // to address
                BigInteger.ZERO, // value to transfer
                data,
                getChainIdV2(), // chainId
                new BigInteger(groupId), // groupId
                null);
        return rawTransaction;
    }

    public static byte[] simpleSignatureSerialization(Sign.SignatureData signatureData) {
        byte[] serializedSignatureData = new byte[65];
        serializedSignatureData[0] = signatureData.getV();
        System.arraycopy(signatureData.getR(), 0, serializedSignatureData, 1, 32);
        System.arraycopy(signatureData.getS(), 0, serializedSignatureData, 33, 32);
        return serializedSignatureData;
    }

    public static Sign.SignatureData simpleSignatureDeserialization(
        byte[] serializedSignatureData) {
        if (65 != serializedSignatureData.length) {
            throw new WeIdBaseException("signature data illegal");
        }
        byte v = serializedSignatureData[0];
        byte[] r = new byte[32];
        byte[] s = new byte[32];
        System.arraycopy(serializedSignatureData, 1, r, 0, 32);
        System.arraycopy(serializedSignatureData, 33, s, 0, 32);
        Sign.SignatureData signatureData = new Sign.SignatureData(v, r, s);
        return signatureData;
    }

    /**
     * Get the chainId for FISCO-BCOS v2.x chainId. Consumed by Restful API service.
     *
     * @return chainId in BigInt.
     */
    public static BigInteger getChainIdV2() {
        try {
            NodeVersion.Version nodeVersion = ((org.fisco.bcos.web3j.protocol.Web3j) BaseService
                .getWeb3j()).getNodeVersion().send().getNodeVersion();
            String chainId = nodeVersion.getChainID();
            return new BigInteger(chainId);
        } catch (Exception e) {
            return BigInteger.ONE;
        }
    }

    /**
     * Get the Blocklimit for FISCO-BCOS v2.0 blockchain. This already adds 600 to block height.
     *
     * @return chainId in BigInt.
     */
    public static BigInteger getBlocklimitV2() {
        try {
            return ((org.fisco.bcos.web3j.protocol.Web3j) BaseService.getWeb3j())
                .getBlockNumberCache();
        } catch (Exception e) {
            return null;
        }
    }

    public static Optional<TransactionReceipt> getTransactionReceiptRequest(String transactionHash) {
        Optional<TransactionReceipt> receiptOptional = Optional.empty();
        Web3j web3j = (Web3j) BaseService.getWeb3j();
        try {
            for (int i = 0; i < 5; i++) {
                receiptOptional = web3j.getTransactionReceipt(transactionHash).send().getTransactionReceipt();
                if (!receiptOptional.isPresent()) {
                    Thread.sleep(1000);
                } else {
                    return receiptOptional;
                }
            }
        } catch (IOException | InterruptedException e) {
            System.out.println();
        }
        return receiptOptional;
    }


    /**
     * Encode Credential to client side for further signing. The raw data will be put into the signature part.
     *
     * @param createCredentialPojoFuncArgs createCredentialPojo args
     * @return encodedCredential with rawData supplied in the signature
     */
    public static HttpResponseData<Object> encodeCredential(InputArg createCredentialPojoFuncArgs) {
        try {
            JsonNode cptIdNode;
            JsonNode issuerNode;
            JsonNode expirationDateNode;
            JsonNode claimNode;
            // build createCredentialPojoArgs
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
            CreateCredentialPojoArgs args = new CreateCredentialPojoArgs();
            args.setExpirationDate(expirationDate);
            args.setCptId(cptId);
            args.setIssuer(issuerNode.textValue());
            Map<String, Object> claimMap;
            try {
                claimMap = (HashMap<String, Object>) JsonUtil
                    .jsonStrToObj(new HashMap<String, Object>(), claimNode.toString());
            } catch (Exception e) {
                return new HttpResponseData<>(null,
                    ErrorCode.CREDENTIAL_CLAIM_DATA_ILLEGAL.getCode(),
                    ErrorCode.CREDENTIAL_CLAIM_DATA_ILLEGAL.getCodeDesc());
            }
            args.setClaim(claimMap);

            // Create CredentialPojo
            CredentialPojo result = new CredentialPojo();
            String context = CredentialUtils.getDefaultCredentialContext();
            result.setContext(context);
            if (StringUtils.isBlank(args.getId())) {
                result.setId(UUID.randomUUID().toString());
            } else {
                result.setId(args.getId());
            }
            result.setCptId(args.getCptId());
            result.setIssuanceDate(DateUtils.getNoMillisecondTimeStamp());
            result.setIssuer(args.getIssuer());
            Long newExpirationDate =
                DateUtils.convertToNoMillisecondTimeStamp(args.getExpirationDate());
            if (newExpirationDate == null) {
                logger.error("Create Credential Args illegal.");
                return new HttpResponseData<>(null, ErrorCode.CREDENTIAL_EXPIRE_DATE_ILLEGAL.getCode(),
                    ErrorCode.CREDENTIAL_EXPIRE_DATE_ILLEGAL.getCodeDesc());
            } else {
                result.setExpirationDate(newExpirationDate);
            }
            result.addType(CredentialConstant.DEFAULT_CREDENTIAL_TYPE);
            result.addType(CredentialConstant.ORIGINAL_CREDENTIAL_TYPE);
            Object claimObject = args.getClaim();
            String claimStr;
            if (!(claimObject instanceof String)) {
                claimStr = DataToolUtils.serialize(claimObject);
            } else {
                claimStr = (String) claimObject;
            }

            HashMap<String, Object> claimMapNew = DataToolUtils.deserialize(claimStr, HashMap.class);
            result.setClaim(claimMapNew);

            Map<String, Object> saltMap = DataToolUtils.clone(claimMapNew);
            generateSalt(saltMap, null);
            String rawData = CredentialPojoUtils
                .getCredentialThumbprintWithoutSig(result, saltMap, null);
            System.out.println(rawData);
            result.putProofValue(ParamKeyConstant.PROOF_CREATED, result.getIssuanceDate());

            String weIdPublicKeyId = issuerNode.textValue() + "#keys-0";
            result.putProofValue(ParamKeyConstant.PROOF_CREATOR, weIdPublicKeyId);

            String proofType = CredentialProofType.ECDSA.getTypeName();
            result.putProofValue(ParamKeyConstant.PROOF_TYPE, proofType);
            result.putProofValue(ParamKeyConstant.PROOF_SIGNATURE, rawData);
            result.setSalt(saltMap);
            Map<String, Object> credMap = JsonUtil.objToMap(result);
            return new HttpResponseData<>(credMap, HttpReturnCode.SUCCESS);
        } catch (Exception e) {
            logger.error("Generate Credential failed due to system error. ", e);
            return new HttpResponseData<>(null, ErrorCode.CREDENTIAL_ERROR.getCode(),
                ErrorCode.CREDENTIAL_ERROR.getCodeDesc());
        }
    }
}
