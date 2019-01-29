package com.webank.weid.http.protocol.request;

import lombok.Data;

@Data
public class ReqUpdateCptStringArgs {

    /**
     * Required: the id for the CPT.
     */
    private Integer cptId;

    /**
     * Required: the json schema content defined for this CPT.
     */
    private String cptJsonSchema;

    /**
     * Required: the WeIdentity DID of the publisher who register this CPT.
     */
    private String weId;

    /**
     * Required: The private key for the publisher who register this CPT.
     */
    private String weIdPrivateKey;
}
