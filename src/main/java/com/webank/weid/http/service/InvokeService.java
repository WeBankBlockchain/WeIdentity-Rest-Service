package com.webank.weid.http.service;

import com.webank.weid.http.protocol.response.HttpResponseData;

public interface InvokeService {
    
    /**
     * Directly invoke an SDK function. No client-side sign needed.
     *
     * @param invokeFunctionJsonArgs the json format args. It should contain 4 keys: functionArgs,
     * (including all business related params), EMPTY transactionArgs, functionName and apiVersion.
     * @return the json string from SDK response.
     */
    HttpResponseData<Object> invokeFunction(String invokeFunctionJsonArgs);
}
