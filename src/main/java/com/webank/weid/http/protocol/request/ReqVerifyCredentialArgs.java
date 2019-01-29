package com.webank.weid.http.protocol.request;

import java.util.Map;

import lombok.Data;

@Data
public class ReqVerifyCredentialArgs {

    /**
     * Required: The context field.
     */
    private String context;

    /**
     * Required: The ID.
     */
    private String id;

    /**
     * Required: The CPT type in standard integer format.
     */
    private Integer cptId;

    /**
     * Required: The issuer WeIdentity DID.
     */
    private String issuer;

    /**
     * Required: The create date.
     */
    private Long issuranceDate;

    /**
     * Required: The expire date.
     */
    private Long expirationDate;

    /**
     * Required: The claim data.
     */
    private Map<String, Object> claim;

    /**
     * Required: The signature of the Credential. Selective Disclosure is supported together with
     * Claim Data structure.
     */
    private String signature;

    /**
     * Required: The public key.
     */
    private String weIdPublicKey;
}
