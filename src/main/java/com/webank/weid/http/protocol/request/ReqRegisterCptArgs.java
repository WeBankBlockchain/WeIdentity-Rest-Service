package com.webank.weid.http.protocol.request;

import java.util.LinkedHashMap;

import lombok.Data;

@Data
public class ReqRegisterCptArgs {

    /**
     * Required: The weIdentity DID of the publisher who register this CPT.
     */
    private String cptPublisher;

    /**
     * Required: The private key for the publisher who register this CPT.
     */
    private String cptPublisherPrivateKey;

    private LinkedHashMap<String, Object> cptJsonSchema;

}
