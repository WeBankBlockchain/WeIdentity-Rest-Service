package com.webank.weid.http.protocol.request;

import lombok.Data;

@Data
public class TransactionArg {
    
    String invokerWeId;

    private Object loopback;
}
