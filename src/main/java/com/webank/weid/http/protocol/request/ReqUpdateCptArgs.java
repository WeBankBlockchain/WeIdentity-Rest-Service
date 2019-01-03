package com.webank.weid.http.protocol.request;

import lombok.Data;

@Data
public class ReqUpdateCptArgs {

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
    private String cptPublisher;

    /**
     * Required: the private key for the publisher who register this CPT.
     */
    private String cptPublisherPrivateKey;
}
