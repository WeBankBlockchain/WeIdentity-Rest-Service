package com.webank.weid.http.protocol.request;

import lombok.Data;

@Data
public class ReqRegisterAuthorityIssuerArgs {

    /**
     * Required: The WeIdentity DID of the Authority Issuer.
     */
    private String weId;

    /**
     * Required: The organization name of the Authority Issuer.
     */
    private String name;

    /**
     * Required: The create date of the Authority Issuer, in timestamp (Long) format.
     */
    private Long created;

    /**
     * Required: The accumulator value of the Authority Issuer.
     */
    private String accValue;

    /**
     * Required: The WeIdentity DID private key for sending transaction.
     */
    private String weIdPrivateKey;
}
