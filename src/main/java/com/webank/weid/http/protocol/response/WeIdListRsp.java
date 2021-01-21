package com.webank.weid.http.protocol.response;

import java.util.List;
import lombok.Data;

/**
 * get WeId and errorCode by pubkeyList response.
 *
 */

@Data
public class WeIdListRsp {

	private List<String> weIdList;

	private List<Integer> errorCodeList;
}
