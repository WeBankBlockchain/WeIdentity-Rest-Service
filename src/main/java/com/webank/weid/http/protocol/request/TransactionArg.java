package com.webank.weid.http.protocol.request;

import lombok.Data;

@Data
public class TransactionArg {
    
    String invokerWeId;
    String nonce;
    String data;
    String signedMessage;
    String toAddress;
    Object loopback;
}
