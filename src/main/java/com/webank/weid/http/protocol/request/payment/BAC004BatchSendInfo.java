package com.webank.weid.http.protocol.request.payment;

import java.util.List;

import com.webank.payment.protocol.base.BaseAsset;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class BAC004BatchSendInfo extends BaseAsset {
    
    private List<BAC004SendInfo> list;
}
