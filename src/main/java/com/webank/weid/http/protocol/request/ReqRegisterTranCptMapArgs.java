package com.webank.weid.http.protocol.request;

import java.util.Map;

import lombok.Data;

@Data
public class ReqRegisterTranCptMapArgs {

    /**
     * Required: The weIdentity DID of the publisher who register this CPT.
     */
    private String weId;

    /**
     * Required: The private key for the publisher who register this CPT.
     */
    private String weIdPrivateKey;

    /**
     * Required: The weIdentity DID of the cptJsonSchema.
     */
    private Map<String, Object> cptJsonSchema;

    private String bodySigned;

}
