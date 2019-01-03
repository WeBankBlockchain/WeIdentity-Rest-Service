package com.webank.weid.http.protocol.request;

import lombok.Data;

@Data
public class ReqCreateCredentialArgs {

    /**
     * Required: The CPT type in standard integer format.
     */
    private Integer cptId;

    /**
     * Required: The issuer WeIdentity DID.
     */
    private String issuer;

    /**
     * Required: The expire date.
     */
    private Long expirationDate;

    /**
     * Required: The claim data.
     */
    private String claim;

    /**
     * Required: The private key structure used for signing.
     */
    private String weIdPrivateKey;
}
