package com.webank.weid.http.protocol.request;

import java.util.Map;

import lombok.Data;
import org.bcos.web3j.crypto.Sign.SignatureData;

@Data
public class ReqRegisterSignCptMapArgs {


    private Map<String, Object> dataJson;

    private SignatureData signatureData;

    private SignatureData bodySigned;

}
