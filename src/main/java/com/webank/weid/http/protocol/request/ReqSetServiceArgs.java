package com.webank.weid.http.protocol.request;

import lombok.Data;

@Data
public class ReqSetServiceArgs {

    /**
     * Required: user's WeIdentity DID.
     */
    private String weId;

    /**
     * Required: service type.
     */
    private String type;

    /**
     * Required: service endpoint.
     */
    private String serviceEndpoint;

    /**
     * Required: WeIdentity DID private key.
     */
    private String userWeIdPrivateKey;
}
