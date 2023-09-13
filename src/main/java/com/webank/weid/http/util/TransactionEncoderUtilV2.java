package com.webank.weid.http.util;

import static com.webank.weid.service.impl.CredentialPojoServiceImpl.generateSalt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webank.weid.blockchain.config.FiscoConfig;
import com.webank.weid.blockchain.service.fisco.CryptoFisco;
import com.webank.weid.constant.CredentialConstant;
import com.webank.weid.constant.CredentialConstant.CredentialProofType;
import com.webank.weid.constant.ErrorCode;
import com.webank.weid.constant.ParamKeyConstant;
import com.webank.weid.constant.WeIdConstant;
import com.webank.weid.http.constant.HttpReturnCode;
import com.webank.weid.http.constant.SignType;
import com.webank.weid.http.constant.WeIdentityFunctionNames;
import com.webank.weid.http.constant.WeIdentityParamKeyConstant;
import com.webank.weid.http.protocol.request.InputArg;
import com.webank.weid.http.protocol.response.HttpResponseData;
import com.webank.weid.protocol.base.AuthenticationProperty;
import com.webank.weid.protocol.base.CredentialPojo;
import com.webank.weid.protocol.base.ServiceProperty;
import com.webank.weid.protocol.request.CreateCredentialPojoArgs;
import com.webank.weid.protocol.response.RsvSignature;
import com.webank.weid.blockchain.service.fisco.BaseServiceFisco;
import com.webank.weid.service.impl.WeIdServiceImpl;
import com.webank.weid.service.rpc.WeIdService;
import com.webank.weid.util.CredentialPojoUtils;
import com.webank.weid.util.CredentialUtils;
import com.webank.weid.util.DataToolUtils;
import com.webank.weid.util.DateUtils;
import com.webank.weid.util.Multibase.Multibase;
import com.webank.weid.util.Multicodec.Multicodec;
import com.webank.weid.util.Multicodec.MulticodecEncoder;
import com.webank.weid.blockchain.util.TransactionUtils;
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
import org.fisco.bcos.sdk.abi.FunctionEncoder;
import org.fisco.bcos.sdk.abi.TypeReference;
import org.fisco.bcos.sdk.abi.Utils;
import org.fisco.bcos.sdk.abi.datatypes.*;
import org.fisco.bcos.sdk.abi.datatypes.generated.*;
import org.fisco.bcos.sdk.client.Client;
import org.fisco.bcos.sdk.crypto.signature.ECDSASignatureResult;
import org.fisco.bcos.sdk.crypto.signature.SM2SignatureResult;
import org.fisco.bcos.sdk.model.CryptoType;
import org.fisco.bcos.sdk.model.NodeVersion;
import org.fisco.bcos.sdk.model.TransactionReceipt;
import org.fisco.bcos.sdk.rlp.RlpEncoder;
import org.fisco.bcos.sdk.rlp.RlpList;
import org.fisco.bcos.sdk.rlp.RlpString;
import org.fisco.bcos.sdk.rlp.RlpType;
import org.fisco.bcos.sdk.transaction.builder.TransactionBuilderService;
import org.fisco.bcos.sdk.transaction.model.po.RawTransaction;
import org.fisco.bcos.sdk.utils.ByteUtils;
import org.fisco.bcos.sdk.utils.Numeric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionEncoderUtilV2 {

    private static Logger logger = LoggerFactory.getLogger(TransactionEncoderUtilV2.class);

    public static HttpResponseData<String> createEncoder(
        FiscoConfig fiscoConfig,
        String inputParam,
        String nonce,
        String functionName,
        SignType signType
    ) {
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
            function = buildRegisterCptFunction(inputParam, functionName, signType);
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
        String created = String.valueOf(System.currentTimeMillis());
        AuthenticationProperty authenticationProperty = new AuthenticationProperty();
        //在创建weid时默认添加一个id为#keys-[hash(publicKey)]的verification method
        authenticationProperty.setId(weId + "#keys-" + DataToolUtils.hash(publicKey).substring(58));
        //verification method controller默认为自己
        authenticationProperty.setController(weId);
        //这里把publicKey用multicodec编码，然后使用Multibase格式化，国密和非国密使用不同的编码
        byte[] publicKeyEncode = MulticodecEncoder.encode(CryptoFisco.cryptoSuite.getCryptoTypeConfig() == CryptoType.ECDSA_TYPE? Multicodec.ED25519_PUB:Multicodec.SM2_PUB,
                publicKey.getBytes(StandardCharsets.UTF_8));
        authenticationProperty.setPublicKeyMultibase(Multibase.encode(Multibase.Base.Base58BTC, publicKeyEncode));
        List<String> authList = new ArrayList<>();
        authList.add(authenticationProperty.toString());
        List<String> serviceList = new ArrayList<>();
        ServiceProperty serviceProperty = new ServiceProperty();
        serviceProperty.setServiceEndpoint("https://github.com/WeBankBlockchain/WeIdentity");
        serviceProperty.setType("WeIdentity");
        serviceProperty.setId(authenticationProperty.getController() + '#' + DataToolUtils.hash(serviceProperty.getServiceEndpoint()).substring(58));
        serviceList.add(serviceProperty.toString());
        return new Function(
            WeIdentityFunctionNames.FUNCNAME_CALL_MAP_V2.get(functionName),
                Arrays.<Type>asList(new Address(addr),
                        new Utf8String(created),
                        new DynamicArray<Utf8String>(
                                Utils.typeMap(authList, Utf8String.class)),
                        new DynamicArray<Utf8String>(
                                Utils.typeMap(serviceList, Utf8String.class))),
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
            Arrays.<Type>asList(
                new Address(weAddress),
                new StaticArray16<Bytes32>(
                    Utils.typeMap(
                            com.webank.weid.blockchain.util.DataToolUtils.bytesArrayListToBytes32ArrayList(
                            stringAttributes,
                            WeIdConstant.AUTHORITY_ISSUER_ARRAY_LEGNTH
                        ), Bytes32.class)),
                new StaticArray16<Int256>(
                    Utils.typeMap(
                            com.webank.weid.blockchain.util.DataToolUtils.listToListBigInteger(
                            longAttributes,
                            WeIdConstant.AUTHORITY_ISSUER_ARRAY_LEGNTH
                        ),
                        Int256.class)),
                new DynamicBytes(accValue.getBytes(StandardCharsets.UTF_8))
            ),
            Collections.<TypeReference<?>>emptyList());
    }

    public static Function buildRegisterCptFunction(
        String inputParam, 
        String functionName, 
        SignType signType
    ) {
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
        RsvSignature signatureData =
            simpleSignatureDeserialization(
                DataToolUtils.base64Decode(cptSignature.getBytes()), 
                signType
            );
        if (signatureData == null) {
            return null;
        }
        String weAddress = WeIdUtils.convertWeIdToAddress(weId);
        List<byte[]> byteArray = new ArrayList<>();
        return new Function(
            WeIdentityFunctionNames.FUNCNAME_CALL_MAP_V2.get(functionName),
            Arrays.<Type>asList(
                new Address(weAddress),
                new StaticArray8<Int256>(
                    Utils.typeMap(
                            com.webank.weid.blockchain.util.DataToolUtils.listToListBigInteger(
                            DataToolUtils.getParamCreatedList(WeIdConstant.CPT_LONG_ARRAY_LENGTH),
                            WeIdConstant.CPT_LONG_ARRAY_LENGTH
                        ), Int256.class)),
                new StaticArray8<Bytes32>(
                    Utils.typeMap(
                            com.webank.weid.blockchain.util.DataToolUtils.bytesArrayListToBytes32ArrayList(
                            byteArray,
                            WeIdConstant.CPT_STRING_ARRAY_LENGTH
                        ), Bytes32.class)),
                new StaticArray128<Bytes32>(
                    Utils.typeMap(
                            com.webank.weid.blockchain.util.DataToolUtils.stringToByte32ArrayList(
                            cptJsonSchemaNew,
                            WeIdConstant.JSON_SCHEMA_ARRAY_LENGTH
                        ), Bytes32.class)),
                    signatureData.getV(),
                    signatureData.getR(),
                    signatureData.getS()
            ),
            Collections.<TypeReference<?>>emptyList());
    }

    public static String createTxnHex(
        String encodedSig, 
        String nonce, 
        String to, 
        String data, 
        String blockLimit,
        SignType signType
    ) {
        RsvSignature sigData = TransactionEncoderUtilV2
            .simpleSignatureDeserialization(DataToolUtils.base64Decode(encodedSig.getBytes()), signType);
        FiscoConfig fiscoConfig = new FiscoConfig();
        fiscoConfig.load();
        RawTransaction rawTransaction = TransactionEncoderUtilV2.buildRawTransaction(nonce,
            fiscoConfig.getGroupId(), data, to, new BigInteger(blockLimit));
        byte[] encodedSignedTxn = TransactionEncoderUtilV2.encode(rawTransaction, sigData);
        return Numeric.toHexString(encodedSignedTxn);
    }

    public static String createClientEncodeResult(Function function, String nonce, String to, String groupId) {
        // 1. encode the Function
        FunctionEncoder functionEncoder = new FunctionEncoder(CryptoFisco.cryptoSuite);
        String data = functionEncoder.encode(function);
        return createClientEncodeResult(data, nonce, to, groupId);
    }
    
    public static String createClientEncodeResult(String functionEncode, String nonce, String to, String groupId) {
        BigInteger blockLimit = getBlocklimitV2();
        // 2. server generate encodedTransaction
        RawTransaction rawTransaction = TransactionEncoderUtilV2.buildRawTransaction(nonce,
            groupId, functionEncode, to, blockLimit);
        byte[] encodedTransaction = TransactionEncoderUtilV2.encode(rawTransaction);
        // 3. server sends encodeTransaction (in base64) and data back to client
        return TransactionEncoderUtil.getEncodeOutput(encodedTransaction, functionEncode, blockLimit);
    }

    public static BigInteger getNonce() {
        Random r = new SecureRandom();
        BigInteger randomid = new BigInteger(250, r);
        return randomid;
    }

    public static byte[] encode(RawTransaction rawTransaction) {
        return encode(rawTransaction, null);
    }

    public static byte[] encode(
            RawTransaction rawTransaction, RsvSignature signatureData) {
        List<RlpType> values = asRlpValues(rawTransaction, signatureData);
        RlpList rlpList = new RlpList(values);
        return RlpEncoder.encode(rlpList);
    }

    static List<RlpType> asRlpValues(
        RawTransaction rawTransaction, RsvSignature signatureData) {
        List<RlpType> result = new ArrayList<>();
        result.add(RlpString.create(rawTransaction.getRandomid()));
        result.add(RlpString.create(rawTransaction.getGasPrice()));
        result.add(RlpString.create(rawTransaction.getGasLimit()));
        result.add(RlpString.create(rawTransaction.getBlockLimit()));
        // an empty to address (contract creation) should not be encoded as a numeric 0 value
        String to = rawTransaction.getTo();
        if (to != null && to.length() > 0) {
            // addresses that start with zeros should be encoded with the zeros included, not
            // as numeric values
            result.add(RlpString.create(Numeric.hexStringToByteArray(to)));
        } else {
            result.add(RlpString.create(""));
        }

        result.add(RlpString.create(rawTransaction.getValue()));

        // value field will already be hex encoded, so we need to convert into binary first
        byte[] data = Numeric.hexStringToByteArray(rawTransaction.getData());
        result.add(RlpString.create(data));

        // add extra data!!!

        result.add(RlpString.create(rawTransaction.getFiscoChainId()));
        result.add(RlpString.create(rawTransaction.getGroupId()));
        if (rawTransaction.getExtraData() == null) {
            result.add(RlpString.create(""));
        } else {
            result.add(
                RlpString.create(Numeric.hexStringToByteArray(rawTransaction.getExtraData())));
        }
        if (signatureData != null) {
            if(CryptoFisco.cryptoSuite.getCryptoTypeConfig() == CryptoType.ECDSA_TYPE){
                ECDSASignatureResult signatureResult = new ECDSASignatureResult(
                        signatureData.getV().getValue().byteValueExact(),
                        signatureData.getR().getValue(),
                        signatureData.getS().getValue());
                result.addAll(signatureResult.encode());
            } else {
                result.add(RlpString.create(ByteUtils.trimLeadingZeroes(signatureData.getR().getValue())));
                result.add(RlpString.create(ByteUtils.trimLeadingZeroes(signatureData.getS().getValue())));
            }
            //result.add(RlpString.create(String.valueOf(signatureData.getV()).getBytes(StandardCharsets.UTF_8)));
            //result.add(RlpString.create(ByteUtils.trimLeadingZeroes(signatureData.getR().getValue())));
            //result.add(RlpString.create(ByteUtils.trimLeadingZeroes(signatureData.getS().getValue())));
        }
        return result;
    }

    public static RawTransaction buildRawTransaction(String nonce, String groupId, String data, String to, BigInteger blockLimit) {
        RawTransaction rawTransaction =
                RawTransaction.createTransaction(
                new BigInteger(nonce),
                new BigInteger("99999999999"),
                new BigInteger("99999999999"),
                blockLimit,
                to, // to address
                BigInteger.ZERO, // value to transfer
                data,
                getChainIdV2(), // chainId
                new BigInteger(groupId), // groupId
                null);
        return rawTransaction;
    }

    public static byte[] simpleSignatureSerialization(RsvSignature signatureData) {
        byte[] serializedSignatureData = new byte[65];
        serializedSignatureData[0] = signatureData.getV().getValue().byteValue();
        System.arraycopy(signatureData.getR(), 0, serializedSignatureData, 1, 32);
        System.arraycopy(signatureData.getS(), 0, serializedSignatureData, 33, 32);
        return serializedSignatureData;
    }

    public static byte[] goSignatureSerialization(RsvSignature signatureData) {
        byte[] serializedSignatureData = new byte[65];
        serializedSignatureData[64] = (byte) (signatureData.getV().getValue().byteValue() - 27);
        System.arraycopy(signatureData.getR(), 0, serializedSignatureData, 0, 32);
        System.arraycopy(signatureData.getS(), 0, serializedSignatureData, 32, 32);
        return serializedSignatureData;
    }

    public static String convertIfGoSigToWeIdJavaSdkSig(String goSig) {
        byte[] serializedSig = DataToolUtils.base64Decode(goSig.getBytes(StandardCharsets.UTF_8));
        RsvSignature sigData = simpleSignatureDeserialization(serializedSig, SignType.VSR);
        if (sigData == null) {
            return StringUtils.EMPTY;
        }
        return new String(DataToolUtils.base64Encode(simpleSignatureSerialization(sigData)),
            StandardCharsets.UTF_8);
    }

    public static RsvSignature simpleSignatureDeserialization(
        byte[] serializedSignatureData,
        SignType signType
    ) {
        if (65 == serializedSignatureData.length || 64 == serializedSignatureData.length) {
            if(CryptoFisco.cryptoSuite.getCryptoTypeConfig() == CryptoType.ECDSA_TYPE){
                // Determine signature type
                Byte javav = serializedSignatureData[0];
                Byte lwcv = serializedSignatureData[64];
                byte[] r = new byte[32];
                byte[] s = new byte[32];
                RsvSignature signatureData = new RsvSignature();
                if (signType == SignType.RSV) {
                    // this is the signature from java client
                    logger.info("Java Client signature checked.");
                    System.arraycopy(serializedSignatureData, 1, r, 0, 32);
                    System.arraycopy(serializedSignatureData, 33, s, 0, 32);
                    signatureData.setS(new Bytes32(s));
                    signatureData.setR(new Bytes32(r));
                    signatureData.setV(new Uint8(javav));
                } else if (signType == SignType.VSR) {
                    // this is the standard raw ecdsa sig method (go version client uses this)
                    logger.info("Standard Client signature checked.");
                    lwcv = (byte) (lwcv.intValue());
                    System.arraycopy(serializedSignatureData, 0, r, 0, 32);
                    System.arraycopy(serializedSignatureData, 32, s, 0, 32);
                    signatureData.setS(new Bytes32(s));
                    signatureData.setR(new Bytes32(r));
                    signatureData.setV(new Uint8(lwcv));
                }
                return signatureData;
            } else {
                // Determine signature type
                byte[] r = new byte[32];
                byte[] s = new byte[32];
                RsvSignature signatureData = new RsvSignature();
                if (signType == SignType.RSV) {
                    // this is the signature from java client
                    logger.info("Java Client signature checked.");
                    System.arraycopy(serializedSignatureData, 0, r, 0, 32);
                    System.arraycopy(serializedSignatureData, 32, s, 0, 32);
                    signatureData.setS(new Bytes32(s));
                    signatureData.setR(new Bytes32(r));
                    signatureData.setV(new Uint8(0));
                } else if (signType == SignType.VSR) {
                    // this is the standard raw ecdsa sig method (go version client uses this)
                    logger.info("Standard Client signature checked.");
                    System.arraycopy(serializedSignatureData, 0, r, 0, 32);
                    System.arraycopy(serializedSignatureData, 32, s, 0, 32);
                    signatureData.setS(new Bytes32(s));
                    signatureData.setR(new Bytes32(r));
                    signatureData.setV(new Uint8(0));
                }
                return signatureData;
            }
        }
        return null;
    }

    /**
     * Get the chainId for FISCO-BCOS v2.x chainId. Consumed by Restful API service.
     *
     * @return chainId in BigInt.
     */
    public static BigInteger getChainIdV2() {
        try {
            Client web3j = (Client) BaseServiceFisco.getClient();
            NodeVersion.ClientVersion nodeVersion = web3j.getNodeVersion().getNodeVersion();
            String chainId = nodeVersion.getChainId();
            return new BigInteger(chainId);
        } catch (Exception e) {
            return BigInteger.ONE;
        }
    }

    /**
     * Get the Blocklimit for FISCO-BCOS v2.0 blockchain. This already adds 600 to block height.
     *
     * @return Blocklimit in BigInt.
     */
    public static BigInteger getBlocklimitV2() {
        try {
            Client web3j = (Client) BaseServiceFisco.getClient();
            return web3j.getBlockLimit();
        } catch (Exception e) {
            return null;
        }
    }

    public static Optional<TransactionReceipt> getTransactionReceiptRequest(String transactionHash) {
        Optional<TransactionReceipt> receiptOptional = Optional.empty();
        Client web3j = (Client) BaseServiceFisco.getClient();
        try {
            for (int i = 0; i < 5; i++) {
                receiptOptional = web3j.getTransactionReceipt(transactionHash).getTransactionReceipt();
                if (!receiptOptional.isPresent()) {
                    Thread.sleep(1000);
                } else {
                    return receiptOptional;
                }
            }
        } catch (InterruptedException e) {
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
            result.addType(CredentialConstant.SELECTIVE_CREDENTIAL_TYPE);
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
            result.putProofValue(ParamKeyConstant.PROOF_SIGNATURE,
                new String(DataToolUtils.base64Encode(rawData.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
            result.setSalt(saltMap);
            Map<String, Object> credMap = JsonUtil.objToMap(result);
            return new HttpResponseData<>(credMap, HttpReturnCode.SUCCESS);
        } catch (Exception e) {
            logger.error("Generate Credential failed due to system error. ", e);
            return new HttpResponseData<>(null, ErrorCode.CREDENTIAL_ERROR.getCode(),
                ErrorCode.CREDENTIAL_ERROR.getCodeDesc());
        }
    }
    
    /**
     * Extract and build Input arg for all Service APIs.
     *
     * @param inputJson the inputJson String
     * @return An InputArg instance
     */
    public static HttpResponseData<InputArg> buildInputArg(String inputJson) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(inputJson);
            if (jsonNode == null) {
                logger.error("Null input within: {}", inputJson);
                return new HttpResponseData<>(null, HttpReturnCode.INPUT_NULL);
            }
            JsonNode functionNameNode = jsonNode.get(WeIdentityParamKeyConstant.FUNCTION_NAME);
            JsonNode versionNode = jsonNode.get(WeIdentityParamKeyConstant.API_VERSION);
            if (functionNameNode == null || StringUtils.isEmpty(functionNameNode.textValue())) {
                logger.error("Null input within: {}", jsonNode.toString());
                return new HttpResponseData<>(null, HttpReturnCode.FUNCTION_NAME_ILLEGAL);
            }
            if (versionNode == null || StringUtils.isEmpty(versionNode.textValue())) {
                logger.error("Null input within: {}", jsonNode.toString());
                return new HttpResponseData<>(null, HttpReturnCode.VER_ILLEGAL);
            }
            // Need to use toString() for pure Objects and textValue() for pure String
            JsonNode functionArgNode = jsonNode.get(WeIdentityParamKeyConstant.FUNCTION_ARG);
            if (functionArgNode == null || StringUtils.isEmpty(functionArgNode.toString())) {
                logger.error("Null input within: {}", jsonNode.toString());
                return new HttpResponseData<>(null, HttpReturnCode.FUNCARG_ILLEGAL);
            }
            JsonNode txnArgNode = jsonNode.get(WeIdentityParamKeyConstant.TRANSACTION_ARG);
            if (txnArgNode == null || StringUtils.isEmpty(txnArgNode.toString())) {
                logger.error("Null input within: {}", jsonNode.toString());
                return new HttpResponseData<>(null, HttpReturnCode.TXNARG_ILLEGAL);
            }

            String functionArg = functionArgNode.toString();
            String txnArg = txnArgNode.toString();
            InputArg inputArg = new InputArg();
            inputArg.setFunctionArg(functionArg);
            inputArg.setTransactionArg(txnArg);
            inputArg.setFunctionName(functionNameNode.textValue());
            inputArg.setV(versionNode.textValue());
            return new HttpResponseData<>(inputArg, HttpReturnCode.SUCCESS);
        } catch (Exception e) {
            logger.error("Json Extraction error within: {}", inputJson);
            return new HttpResponseData<>(null, HttpReturnCode.UNKNOWN_ERROR.getCode(),
                HttpReturnCode.UNKNOWN_ERROR.getCodeDesc().concat(e.getMessage()));
        }
    }
}
