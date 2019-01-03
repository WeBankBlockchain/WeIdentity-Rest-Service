package com.webank.weid.http.protocol.request;

import lombok.Data;

@Data
public class ReqSetPublicKeyArgs {

    /**
     * Required: The WeIdentity DID.
     */
    private String weId;

    /**
     * Required: The type.
     */
    private String type = "Secp256k1";

    /**
     * Required: The owner.
     */
    private String owner;

    /**
     * Required: The public key.
     */
    private String publicKey;

    /**
     * Required: The WeIdentity DID private key.
     */
    private String userWeIdPrivateKey;
}
