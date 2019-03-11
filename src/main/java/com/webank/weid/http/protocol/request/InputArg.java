package com.webank.weid.http.protocol.request;

import lombok.Data;

/**
 * The common input argument for all Service API.
 *
 * @author chaoxinhu
 **/
@Data
public class InputArg {
    String functionArg;
    String transactionArg;
    String functionName;
    String v;
}
