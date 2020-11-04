package com.webank.weid.http.protocol.request.payment;

import java.util.List;

import lombok.Data;

@Data
public class AssetAddressList {
    private String assetHolder;
    private List<String> assetAddressList;
}
