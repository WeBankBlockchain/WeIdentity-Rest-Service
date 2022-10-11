package com.webank.weid.http.protocol.base;

import lombok.Data;
/**
 * The base data structure to handle WeId Resolution Metadata.
 *
 * @author afeexian 2022.9.29
 */
@Data
public class ResolutionMetadata {
    /**
     *  Media Type of the returned WeId Document Stream, WeId currently support JSON-LD.
     */
    private String contentType;
    /**
     *  error message.
     */
    private String error;
}
