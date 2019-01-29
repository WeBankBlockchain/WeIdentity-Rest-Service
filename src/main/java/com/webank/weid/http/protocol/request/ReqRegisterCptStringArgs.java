package com.webank.weid.http.protocol.request;

import lombok.Data;

@Data
public class ReqRegisterCptStringArgs {

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
    private String cptJsonSchema;

}
