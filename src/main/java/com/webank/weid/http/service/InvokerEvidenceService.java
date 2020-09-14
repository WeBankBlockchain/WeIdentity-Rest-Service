package com.webank.weid.http.service;

import com.webank.weid.http.protocol.request.InputArg;
import com.webank.weid.http.protocol.response.HttpResponseData;
import org.springframework.stereotype.Service;

@Service
public interface InvokerEvidenceService {

    HttpResponseData<Object> createEvidenceWithExtraInfo(InputArg args);

    HttpResponseData<Object> createEvidence(InputArg args);

    HttpResponseData<Object> createEvidenceWithBatch(InputArg args);

    HttpResponseData<Object> getEvidenceByCustomKey(InputArg args);

    HttpResponseData<Object> getEvidenceByHash(InputArg args);

}
