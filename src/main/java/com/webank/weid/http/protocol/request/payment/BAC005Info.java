package com.webank.weid.http.protocol.request.payment;

import com.webank.payment.protocol.base.BaseBACInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class BAC005Info extends BaseBACInfo {
    private String recipient;
    private String data;
    private Integer assetId;
    private String assetUri;
}
