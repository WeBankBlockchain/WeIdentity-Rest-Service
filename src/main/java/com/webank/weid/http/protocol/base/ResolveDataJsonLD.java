package com.webank.weid.http.protocol.base;

import com.webank.weid.protocol.base.WeIdDocument;
import com.webank.weid.protocol.base.WeIdDocumentMetadata;
import lombok.Data;

/**
 * The base data structure to handle WeIdentity DID DocumentStream in json-ld and DocumentMetadata info.
 *
 * @author afeexian 2022.9.29
 */
@Data
public class ResolveDataJsonLD {
    /**
     *  Resolution Metadata.
     */
    private ResolutionMetadata resolutionMetadata;
    /**
     *  WeId Document.
     */
    private String weIdDocumentJson;

    /**
     *  WeId Document Metadata.
     */
    WeIdDocumentMetadata weIdDocumentMetadata;
}
