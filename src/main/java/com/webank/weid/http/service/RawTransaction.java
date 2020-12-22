package com.webank.weid.http.service;

import com.webank.weid.http.protocol.response.HttpResponseData;

public interface RawTransaction {
    
    /**
     * Create an Encoded Transaction.
     *
     * @param encodeTransactionJsonArgs json format args. It should contain 4 keys: functionArgs
     * (including all business related params), transactionArgs, functionName and apiVersion.
     * Hereafter, functionName will decide which WeID SDK method to engage, and assemble all input
     * params into SDK readable format to send there; apiVersion is for extensibility purpose.
     * @return encoded transaction in Base64 format, and the data segment in RawTransaction.
     */
    HttpResponseData<Object> encodeTransaction(String encodeTransactionJsonArgs);

    /**
     * Send Transaction to Blockchain.
     *
     * @param sendTransactionJsonArgs the json format args. It should contain 4 keys: functionArgs
     * (including all business related params), transactionArgs, functionName and apiVersion.
     * @return the json string from SDK response.
     */
    HttpResponseData<Object> sendTransaction(String sendTransactionJsonArgs);
}

