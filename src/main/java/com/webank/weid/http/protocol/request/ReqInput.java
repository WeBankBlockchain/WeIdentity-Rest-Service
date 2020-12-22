package com.webank.weid.http.protocol.request;

import lombok.Data;

@Data
public class ReqInput<F> {
    /**
     * Required: the function related arguments.
     */
    F functionArg;

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
