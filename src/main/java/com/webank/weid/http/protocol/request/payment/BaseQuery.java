package com.webank.weid.http.protocol.request.payment;

import com.webank.payment.protocol.base.BaseAsset;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class BaseQuery extends BaseAsset {
    private String assetHolder;
}
