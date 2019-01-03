package com.webank.weid.http.protocol.request;

import lombok.Data;

@Data
public class ReqRemoveAuthorityIssuerArgs {

    /**
     * Required: WeIdentity DID.
     */
    private String weId;

    /**
     * Required: WeIdentity DID private key.
     */
    private String weIdPrivateKey;
}
