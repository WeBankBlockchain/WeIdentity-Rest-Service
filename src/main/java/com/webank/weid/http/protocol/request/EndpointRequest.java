package com.webank.weid.http.protocol.request;

import lombok.Data;

@Data
public class EndpointRequest {

    private String requestName;
    private String requestBody;
}
