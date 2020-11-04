package com.webank.weid.http.protocol.request.payment;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class PageQuery extends BaseQuery{
    private Integer amount;
    private Integer index;
    private Integer num;
}
