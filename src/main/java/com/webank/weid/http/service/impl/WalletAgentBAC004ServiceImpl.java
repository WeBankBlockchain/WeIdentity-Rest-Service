package com.webank.weid.http.service.impl;

import org.fisco.bcos.web3j.protocol.core.methods.response.TransactionReceipt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.webank.payment.protocol.base.BaseAsset;
import com.webank.weid.http.constant.HttpReturnCode;
import com.webank.weid.http.constant.WalletAgentFunctionNames;
import com.webank.weid.http.protocol.request.InputArg;
import com.webank.weid.http.protocol.request.ReqInput;
import com.webank.weid.http.protocol.request.payment.AssetAddressList;
import com.webank.weid.http.protocol.request.payment.BAC004BatchSendInfo;
import com.webank.weid.http.protocol.request.payment.BAC004Info;
import com.webank.weid.http.protocol.request.payment.BAC004SendInfo;
import com.webank.weid.http.protocol.request.payment.BaseQuery;
import com.webank.weid.http.protocol.request.payment.PageQuery;
import com.webank.weid.http.protocol.response.HttpResponseData;
import com.webank.weid.http.service.InvokerBAC004AssetService;
import com.webank.weid.http.service.WalletAgentBAC004Service;
import com.webank.weid.http.util.TransactionEncoderUtilV2;
import com.webank.weid.util.DataToolUtils;

@Component
public class WalletAgentBAC004ServiceImpl 
    extends AbstractRawTransactionService 
    implements WalletAgentBAC004Service {
    
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

    @Override
    protected HttpResponseData<String> doEncodeTransaction(InputArg inputArg) {
        String functionName = inputArg.getFunctionName();
        switch(functionName) {
        case WalletAgentFunctionNames.FUNCNAME_WALLETAGENT_ISSUE:
            return bac004AssetService.issueEncoder(toReqInput(inputArg, BAC004Info.class));
        case WalletAgentFunctionNames.FUNCNAME_WALLETAGENT_CONSTRUCT:
            return bac004AssetService.constructEncoder(toReqInput(inputArg, BAC004Info.class));
        case WalletAgentFunctionNames.FUNCNAME_WALLETAGENT_SEND:
            return bac004AssetService.sendEncoder(toReqInput(inputArg, BAC004SendInfo.class));
        case WalletAgentFunctionNames.FUNCNAME_WALLETAGENT_BATCHSEND:
            return bac004AssetService.batchSendEncoder(toReqInput(inputArg, BAC004BatchSendInfo.class));
        default:
            logger.error("Function name undefined: {}.", functionName);
            return new HttpResponseData<>(null, HttpReturnCode.FUNCTION_NAME_ILLEGAL);
        }
    }

    @Override
    protected HttpResponseData<Object> doSendTransaction(
        InputArg inputArg, 
        TransactionReceipt receipt
    ) {
        String functionName =  inputArg.getFunctionName();
        switch(functionName) {
        case WalletAgentFunctionNames.FUNCNAME_WALLETAGENT_ISSUE:
            return bac004AssetService.issueDeCoder(receipt);
        case WalletAgentFunctionNames.FUNCNAME_WALLETAGENT_CONSTRUCT:
            return bac004AssetService.constructDeCoder(receipt);
        case WalletAgentFunctionNames.FUNCNAME_WALLETAGENT_SEND:
            return bac004AssetService.sendDecoder(receipt);
        case WalletAgentFunctionNames.FUNCNAME_WALLETAGENT_BATCHSEND:
            return bac004AssetService.batchSendDecoder(receipt);
        default:
            logger.error("Function name undefined: {}.", functionName);
            return new HttpResponseData<>(null, HttpReturnCode.FUNCTION_NAME_ILLEGAL);
        }
    }

    @Override
    protected String doGetTo(ReqInput<String> req) {
        BaseAsset asset = DataToolUtils.deserialize(req.getFunctionArg(), BaseAsset.class);
        return asset.getAssetAddress();
    }
}
