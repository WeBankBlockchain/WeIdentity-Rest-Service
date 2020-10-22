package com.webank.weid.http.protocol.request;

import java.util.List;

import lombok.Data;

@Data
public class FunctionArg {

    private String shortName;
    private String description;
    private String assetAddress;
    private String recipient;
    private String assetHolder;
    private Integer amount;
    private Integer index;
    private Integer num;
    private String data;
    private List<String> assetAddressList;
    private List<FunctionArg> objectList;
    
}
