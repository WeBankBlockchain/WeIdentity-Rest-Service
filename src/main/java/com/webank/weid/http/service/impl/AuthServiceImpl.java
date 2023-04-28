package com.webank.weid.http.service.impl;

import com.webank.weid.http.constant.HttpReturnCode;
import com.webank.weid.http.protocol.request.EndpointRequest;
import com.webank.weid.http.protocol.response.EndpointInfo;
import com.webank.weid.http.protocol.response.HttpResponseData;
import com.webank.weid.http.service.AuthService;
import com.webank.weid.http.service.BaseService;
import com.webank.weid.http.service.rpc.RpcConnectionHandler;
import com.webank.weid.http.util.EndpointDataUtil;
import com.webank.weid.protocol.base.CredentialPojo;
import com.webank.weid.blockchain.protocol.response.ResponseData;
import com.webank.weid.service.rpc.CredentialPojoService;
import com.webank.weid.service.impl.CredentialPojoServiceImpl;
import com.webank.weid.util.DataToolUtils;
import java.net.URI;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AuthServiceImpl extends BaseService implements AuthService {

    private Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);
    private CredentialPojoService credentialPojoService = new CredentialPojoServiceImpl();
    //private WeIdService weIdService = new WeIdServiceImpl();

    static {
        RpcConnectionHandler.init();
    }

    @Override
    public HttpResponseData<String> requestNonce(CredentialPojo authToken) {
        ResponseData<Boolean> verifyResp = credentialPojoService.verify(authToken.getIssuer(), authToken);
        if (!verifyResp.getResult()) {
            return new HttpResponseData<>(StringUtils.EMPTY, verifyResp.getErrorCode(), verifyResp.getErrorMessage());
        }
        return new HttpResponseData<>(DataToolUtils.getRandomSalt(), HttpReturnCode.SUCCESS);
    }

    @Override
    public HttpResponseData<String> fetchData(CredentialPojo authToken, String signedNonce) {
        ResponseData<Boolean> verifyResp = credentialPojoService.verify(authToken.getIssuer(), authToken);
        if (!verifyResp.getResult()) {
            return new HttpResponseData<>(StringUtils.EMPTY, verifyResp.getErrorCode(), verifyResp.getErrorMessage());
        }
        // TODO verify signedNonce I: with local storage
        // verify signedNonce II: with on-chain toWeId
        /*
        String toWeId = (String) authToken.getClaim().get("toWeId");
        Sign.SignatureData signatureData =
            DataToolUtils.simpleSignatureDeserialization(
                DataToolUtils.base64Decode(
                    signedNonce.getBytes(StandardCharsets.UTF_8)
                )
            );
        String credentialIssuer = authToken.getIssuer();
        ResponseData<WeIdDocument> innerResponseData =
        weIdService.getWeIdDocument(credentialIssuer);
        WeIdDocument weIdDocument = innerResponseData.getResult();
        ErrorCode errorCode = DataToolUtils
            .verifySignatureFromWeId(rawData, signatureData, weIdDocument);
        if (errorCode.getCode() != ErrorCode.SUCCESS.getCode()) {
            return new ResponseData<>(false, errorCode);
        }
        */
        // verify III: check if serviceUrl exists in this local registered endpoints
        String serviceUrl = (String) authToken.getClaim().get("serviceUrl");
        String hostname;
        Integer port;
        String endpointName;
        try {
            URI uri = new URI(serviceUrl);
            hostname = uri.getHost();
            port = uri.getPort();
            String path = uri.getPath();
            if (StringUtils.isEmpty(hostname) || StringUtils.isEmpty(path) || port < 0) {
                logger.error("Service URL illegal: {}", serviceUrl);
                return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.INPUT_ILLEGAL);
            }
            // Truncate the first slash
            endpointName = path.substring(1);
        } catch (Exception e) {
            logger.error("Service URL illegal: {}", serviceUrl);
            return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.INPUT_ILLEGAL);
        }
        String endpointInAddr = hostname + ":" + port.toString();
        logger.debug("The Endpoint to fetch is {} and the endpointName is {}", endpointInAddr, endpointName);
        EndpointDataUtil.loadAllEndpointInfoFromProps();
        List<EndpointInfo> allEndpoints = EndpointDataUtil.getAllEndpointInfo();
        boolean found = false;
        for (EndpointInfo endpointInfo : allEndpoints) {
            logger.debug("Endpoint name to-check is {}", endpointInfo.getRequestName());
            if (endpointName.equalsIgnoreCase(endpointInfo.getRequestName())) {
                List<String> inAddrs = endpointInfo.getInAddr();
                for (String inAddr : inAddrs) {
                    logger.debug("endpoint addr to-check is {}", endpointInAddr);
                    if (inAddr.equalsIgnoreCase(endpointInAddr)) {
                        found = true;
                        break;
                    }
                }
                if (found) {
                    break;
                }
            }
        }
        if (!found) {
            return new HttpResponseData<>(StringUtils.EMPTY, HttpReturnCode.RPC_ENDPOINT_NOT_EXIST);
        }
        // Final: invoke endpoint via provided resourceId
        EndpointRequest endpointRequest = new EndpointRequest();
        endpointRequest.setRequestName(endpointName);
        endpointRequest.setRequestBody((String) authToken.getClaim().get("resourceId"));

        try {
            String uuid = RpcConnectionHandler
                .send(endpointInAddr, endpointRequest).getRespBody();
            if (StringUtils.isEmpty(uuid)) {
                return new HttpResponseData<>(null, HttpReturnCode.RPC_SEND_FAIL);
            }
            return RpcConnectionHandler.get(uuid);
        } catch (Exception e) {
            return new HttpResponseData<>(null, HttpReturnCode.RPC_SEND_FAIL.getCode(),
                HttpReturnCode.RPC_GET_FAIL.getCodeDesc() + e.getMessage());
        }
    }
}
