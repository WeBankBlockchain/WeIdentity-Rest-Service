package com.webank.weid.http.protocol.request;

import lombok.Data;

@Data
public class ReqVerifyCredentialArgs extends ReqCredentialArgs {

    String weIdPublicKey;
}
