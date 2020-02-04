package com.webank.weid.http.service.impl;

import com.webank.weid.http.constant.HttpReturnCode;
import com.webank.weid.http.constant.WeIdentityServiceEndpoint;
import com.webank.weid.http.protocol.request.EndpointRequest;
import com.webank.weid.http.protocol.response.EndpointInfo;
import com.webank.weid.http.protocol.response.HttpResponseData;
import com.webank.weid.http.service.AuthService;
import com.webank.weid.http.service.BaseService;
import com.webank.weid.http.service.rpc.RpcConnectionHandler;
import com.webank.weid.http.util.EndpointDataUtil;
import com.webank.weid.protocol.base.CredentialPojo;
import com.webank.weid.protocol.response.ResponseData;
import com.webank.weid.rpc.CredentialPojoService;
import com.webank.weid.service.impl.CredentialPojoServiceImpl;
import com.webank.weid.util.DataToolUtils;
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
        // verify III: check serviceUrl exists in this local registered endpoints
        String serviceUrl = (String) authToken.getClaim().get("serviceUrl");
        EndpointDataUtil.loadAllEndpointInfoFromProps();
        List<EndpointInfo> allEndpoints = EndpointDataUtil.getAllEndpointInfo();
        boolean found = false;
        for (EndpointInfo endpointInfo : allEndpoints) {
            if (WeIdentityServiceEndpoint.AUTHO_DEFAULT_FETCH_REQ_NAME
                .equalsIgnoreCase(endpointInfo.getRequestName())) {
                List<String> inAddrs = endpointInfo.getInAddr();
                for (String inAddr : inAddrs) {
                    if (inAddr.equalsIgnoreCase(serviceUrl)) {
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
        endpointRequest.setRequestName(WeIdentityServiceEndpoint.AUTHO_DEFAULT_FETCH_REQ_NAME);
        endpointRequest.setRequestBody((String) authToken.getClaim().get("resourceId"));

        try {
            String uuid = RpcConnectionHandler
                .send(serviceUrl, endpointRequest).getRespBody();
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
