package com.webank.weid.http.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.webank.weid.http.constant.HttpReturnCode;
import com.webank.weid.http.constant.WalletAgentFunctionNames;
import com.webank.weid.http.protocol.request.InputArg;
import com.webank.weid.http.protocol.request.ReqInput;
import com.webank.weid.http.protocol.request.TransactionArg;
import com.webank.weid.http.protocol.request.payment.AssetAddressList;
import com.webank.weid.http.protocol.request.payment.BAC004BatchSendInfo;
import com.webank.weid.http.protocol.request.payment.BAC004Info;
import com.webank.weid.http.protocol.request.payment.BAC004SendInfo;
import com.webank.weid.http.protocol.request.payment.BaseQuery;
import com.webank.weid.http.protocol.request.payment.PageQuery;
import com.webank.weid.http.protocol.response.HttpResponseData;
import com.webank.weid.http.service.BaseService;
import com.webank.weid.http.service.InvokerBAC004AssetService;
import com.webank.weid.http.service.WalletAgentBAC004Service;
import com.webank.weid.http.util.TransactionEncoderUtilV2;
import com.webank.weid.util.DataToolUtils;

@Component
public class WalletAgentBAC004ServiceImpl extends BaseService implements WalletAgentBAC004Service {
    
    private Logger logger = LoggerFactory.getLogger(WalletAgentBAC004ServiceImpl.class);
    
    @Autowired
    private InvokerBAC004AssetService bac004AssetService;
    
    @Override
    public HttpResponseData<Object> invokeFunction(String invokeFunctionJsonArgs) {
        HttpResponseData<InputArg> resp = TransactionEncoderUtilV2
                .buildInputArg(invokeFunctionJsonArgs);
        InputArg inputArg = resp.getRespBody();
        if (inputArg == null) {
            logger.error("Failed to build input argument: {}", invokeFunctionJsonArgs);
            return new HttpResponseData<>(null, resp.getErrorCode(), resp.getErrorMessage());
        }
        String functionName = inputArg.getFunctionName();
        try {
            switch(functionName) {
            case WalletAgentFunctionNames.FUNCNAME_WALLETAGENT_CONSTRUCT:
                return bac004AssetService.construct(toReqInput(inputArg, BAC004Info.class));
            case WalletAgentFunctionNames.FUNCNAME_WALLETAGENT_ISSUE:
                return bac004AssetService.issue(toReqInput(inputArg, BAC004Info.class));
            case WalletAgentFunctionNames.FUNCNAME_WALLETAGENT_CONSTRUCTANDISSUE:
                return bac004AssetService.constructAndIssue(toReqInput(inputArg, BAC004Info.class));
            case WalletAgentFunctionNames.FUNCNAME_WALLETAGENT_GETBALANCE:
                return bac004AssetService.getBalance(toReqInput(inputArg, BaseQuery.class));
            case WalletAgentFunctionNames.FUNCNAME_WALLETAGENT_GETBATCHBALANCE:
                return bac004AssetService.getBatchBalance(toReqInput(inputArg, AssetAddressList.class));
            case WalletAgentFunctionNames.FUNCNAME_WALLETAGENT_GETBALANCEBYWEID:
                return bac004AssetService.getBalanceByWeId(toReqInput(inputArg, PageQuery.class));
            case WalletAgentFunctionNames.FUNCNAME_WALLETAGENT_SEND:
                return bac004AssetService.send(toReqInput(inputArg, BAC004SendInfo.class));
            case WalletAgentFunctionNames.FUNCNAME_WALLETAGENT_BATCHSEND:
                return bac004AssetService.batchSend(toReqInput(inputArg, BAC004BatchSendInfo.class));
            case WalletAgentFunctionNames.FUNCNAME_WALLETAGENT_GETBASEINFO:
                return bac004AssetService.getBaseInfo(toReqInput(inputArg, AssetAddressList.class));
            case WalletAgentFunctionNames.FUNCNAME_WALLETAGENT_GETBASEINFOBYWEID:
                return bac004AssetService.getBaseInfoByWeId(toReqInput(inputArg, PageQuery.class));
            }
            logger.error("Function name undefined: {}.", functionName);
            return new HttpResponseData<>(null, HttpReturnCode.FUNCTION_NAME_ILLEGAL);
        } catch (Exception e) {
            logger.error("[invokeFunction]: unknown error with input argument {}",
                invokeFunctionJsonArgs,
                e);
            return new HttpResponseData<>(null, HttpReturnCode.UNKNOWN_ERROR.getCode(),
                HttpReturnCode.UNKNOWN_ERROR.getCodeDesc());
        }
    }
    
    private <F> ReqInput<F> toReqInput(InputArg inputArg, Class<F> functionClass) {
        ReqInput<F> reqInput = new ReqInput<F>();
        reqInput.setV(inputArg.getV());
        reqInput.setFunctionName(inputArg.getFunctionName());
        reqInput.setFunctionArg(
            DataToolUtils.deserialize(inputArg.getFunctionArg(), functionClass));
        reqInput.setTransactionArg(
            DataToolUtils.deserialize(inputArg.getTransactionArg(), TransactionArg.class));
        return reqInput;
    }

}
