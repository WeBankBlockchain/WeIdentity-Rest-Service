package com.webank.weid.http.protocol.request;

import java.util.List;

import com.webank.payment.protocol.base.BAC005AssetInfo;
import lombok.Data;

@Data
public class FunctionArg {

    private String shortName;
    private String description;
    private String assetAddress;
    private String recipient;
    private String assetHolder;
    private Integer amount;
    private Integer assetId;
    private String assetUri;
    private Integer index;
    private Integer num;
    private String data;
    private List<String> assetAddressList;
    private List<FunctionArg> objectList;
    private List<BAC005AssetInfo> bac005AssetInfoList;
    
}
