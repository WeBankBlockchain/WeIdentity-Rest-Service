package com.webank.weid.http.protocol.request;

import lombok.Data;

@Data
public class ReqInput {
    /**
     * Required: the function related arguments.
     */
    FunctionArg functionArg;

    /**
     * Required: the transaction related arguments.
     */
    TransactionArg transactionArg;

    /**
     * Required: the function name to be called.
     */
    String functionName;

    /**
     * Required: the API version.
     */
    String v;
}
