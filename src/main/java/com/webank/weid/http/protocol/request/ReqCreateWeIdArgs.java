package com.webank.weid.http.protocol.request;

import lombok.Data;

@Data
public class ReqCreateWeIdArgs {

    /**
     * Required: Public Key.
     */
    private String publicKey;

    /**
     * Required: WeIdentity DID private key.
     */
    private String weIdPrivateKey;
}
