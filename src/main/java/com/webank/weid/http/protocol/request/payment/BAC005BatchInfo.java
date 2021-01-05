package com.webank.weid.http.protocol.request.payment;

import com.webank.payment.protocol.base.BaseBACInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class BAC005BatchInfo extends BaseBACInfo {
    
    private List<BAC005Info> list;

    private String remark;
}
