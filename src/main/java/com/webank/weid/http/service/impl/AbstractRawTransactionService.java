package com.webank.weid.http.service.impl;

import com.webank.weid.exception.WeIdBaseException;
import org.apache.commons.lang3.StringUtils;
import org.fisco.bcos.web3j.protocol.core.methods.response.TransactionReceipt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.webank.payment.constant.ErrorCode;
import com.webank.payment.protocol.response.ResponseData;
import com.webank.payment.rpc.RawTransactionService;
import com.webank.payment.service.impl.RawTransactionServiceImpl;
import com.webank.weid.http.constant.HttpReturnCode;
import com.webank.weid.http.constant.SignType;
import com.webank.weid.http.protocol.request.InputArg;
import com.webank.weid.http.protocol.request.ReqInput;
import com.webank.weid.http.protocol.request.TransactionArg;
import com.webank.weid.http.protocol.response.HttpResponseData;
import com.webank.weid.http.service.BaseService;
import com.webank.weid.http.service.RawTransaction;
import com.webank.weid.http.util.JsonUtil;
import com.webank.weid.http.util.TransactionEncoderUtil;
import com.webank.weid.http.util.TransactionEncoderUtilV2;
import com.webank.weid.util.DataToolUtils;

public abstract class AbstractRawTransactionService extends BaseService implements RawTransaction {
    
    private Logger logger = LoggerFactory.getLogger(AbstractRawTransactionService.class);
    
    private RawTransactionService rawTransactionService;

    private RawTransactionService getRawTransactionService() {
        if (rawTransactionService == null) {
            rawTransactionService = new RawTransactionServiceImpl();
        }
        return rawTransactionService;
    }
    
    protected <F> ReqInput<F> toReqInput(InputArg inputArg, Class<F> functionClass) {
        ReqInput<F> reqInput = new ReqInput<F>();
        reqInput.setV(inputArg.getV());
        reqInput.setFunctionName(inputArg.getFunctionName());
        if (functionClass != String.class) {
            reqInput.setFunctionArg(
                DataToolUtils.deserialize(inputArg.getFunctionArg(), functionClass)); 
        }
        reqInput.setTransactionArg(
            DataToolUtils.deserialize(inputArg.getTransactionArg(), TransactionArg.class));
        return reqInput;
    }
    
    @Override
    public HttpResponseData<Object> encodeTransaction(String encodeTransactionJsonArgs) {
        Object loopBack = null;
        try {
            HttpResponseData<InputArg> resp = TransactionEncoderUtil
                .buildInputArg(encodeTransactionJsonArgs);
            InputArg inputArg = resp.getRespBody();
            if (inputArg == null) {
                logger.error("Failed to build input argument: {}", encodeTransactionJsonArgs);
                return new HttpResponseData<>(null, resp.getErrorCode(), resp.getErrorMessage());
            }
            loopBack = getLoopBack(inputArg.getTransactionArg());
            ReqInput<String> req = toReqInput(inputArg, String.class);
            req.setFunctionArg(inputArg.getFunctionArg());
            TransactionArg transactionArg = req.getTransactionArg();
            if (transactionArg == null || StringUtils.isBlank(transactionArg.getNonce())) {
                logger.error("Null input within TransactionArg: {}", transactionArg);
                return new HttpResponseData<>(null, loopBack, HttpReturnCode.NONCE_ILLEGAL);
            }
            String nonce = transactionArg.getNonce();
            HttpResponseData<String> functionRes= doEncodeTransaction(inputArg);
            if (functionRes.getErrorCode() != ErrorCode.SUCCESS.getCode()) {
                return new HttpResponseData<>(
                    null,
                    loopBack,
                    functionRes.getErrorCode(), 
                    functionRes.getErrorMessage()
                );
            }
            String functionEncode = TransactionEncoderUtilV2
                .createClientEncodeResult(
                    functionRes.getRespBody(), 
                    nonce,
                    doGetTo(req), 
                    fiscoConfig.getGroupId()
                );
            return new HttpResponseData<>(
                JsonUtil.convertJsonToSortedMap(functionEncode),
                loopBack,
                functionRes.getErrorCode(),
                functionRes.getErrorMessage());
        } catch (WeIdBaseException e) {
            logger.error("[createEncodingFunction]: createEncodingFunction failed, input argument {}",
                encodeTransactionJsonArgs,
                e);
            return new HttpResponseData<>(null, loopBack, e.getErrorCode().getCode(), e.getMessage());
        } catch (Exception e) {
            logger.error("[createEncodingFunction]: unknown error with input argment {}",
                encodeTransactionJsonArgs,
                e);
            String errorMsg = StringUtils.isBlank(e.getMessage()) ? "" : e.getMessage();
            return new HttpResponseData<>(null, loopBack, HttpReturnCode.UNKNOWN_ERROR.getCode(),
                HttpReturnCode.UNKNOWN_ERROR.getCodeDesc().concat(errorMsg));
        }
    }

    @Override
    public HttpResponseData<Object> sendTransaction(String sendTransactionJsonArgs) {
        Object loopBack = null;
        try {
            HttpResponseData<InputArg> resp = TransactionEncoderUtil
                .buildInputArg(sendTransactionJsonArgs);
            InputArg inputArg = resp.getRespBody();
            if (inputArg == null) {
                logger.error("Failed to build input argument: {}", sendTransactionJsonArgs);
                return new HttpResponseData<>(StringUtils.EMPTY, resp.getErrorCode(),
                    resp.getErrorMessage());
            }
            loopBack = getLoopBack(inputArg.getTransactionArg());
            ReqInput<String> req = toReqInput(inputArg, String.class);
            req.setFunctionArg(inputArg.getFunctionArg());
            TransactionArg transactionArg = req.getTransactionArg();
            String nonce = transactionArg.getNonce();
            String blockLimit = transactionArg.getBlockLimit();
            String data = transactionArg.getData();
            String signedMessage = transactionArg.getSignedMessage();
            String to = transactionArg.getToAddress();
            if (nonce == null || StringUtils.isEmpty(nonce)) {
                logger.error("Null input within: {}", transactionArg);
                return new HttpResponseData<>(null, loopBack, HttpReturnCode.NONCE_ILLEGAL);
            }
            if (blockLimit == null || StringUtils.isEmpty(blockLimit)) {
                logger.error("Null input within: {}", transactionArg);
                return new HttpResponseData<>(null, loopBack, HttpReturnCode.BLOCK_LIMIT_ILLEGAL);
            }
            if (data == null || StringUtils.isEmpty(data)) {
                logger.error("Null input within: {}", transactionArg);
                return new HttpResponseData<>(null, loopBack, HttpReturnCode.DATA_ILLEGAL);
            }
            if (signedMessage == null || StringUtils.isEmpty(signedMessage)) {
                logger.error("Null input within: {}", transactionArg);
                return new HttpResponseData<>(null, loopBack, HttpReturnCode.SIGNED_MSG_ILLEGAL);
            }
            if (transactionArg.getSignType() == 0) {
                logger.error("Null input within: {}", transactionArg);
                return new HttpResponseData<>(null, loopBack, HttpReturnCode.SIGN_TYPE_ILLEGAL);
            }
            //如果为空则说明向系统获取
            if (StringUtils.isEmpty(to)) {
                to = doGetTo(req);
            }
            String txnHex = TransactionEncoderUtilV2.createTxnHex(
                signedMessage, 
                nonce, 
                to, 
                data,
                blockLimit,
                SignType.getSignTypeByCode(transactionArg.getSignType())
            );
            ResponseData<TransactionReceipt> sendTransaction = 
                getRawTransactionService().sendTransaction(txnHex);
            if (sendTransaction.getErrorCode() != ErrorCode.SUCCESS.getCode()) {
                return new HttpResponseData<>(
                    null,
                    loopBack,
                    sendTransaction.getErrorCode(), 
                    sendTransaction.getErrorMessage()
                );
            }
            HttpResponseData<Object> responseData = doSendTransaction(inputArg, sendTransaction.getResult());
            responseData.setLoopback(loopBack);
            return responseData;
        } catch (WeIdBaseException e) {
            logger.error("[sendTransaction]: sendTransaction failed, input argument {}",
                sendTransactionJsonArgs,
                e);
            return new HttpResponseData<>(null, loopBack, e.getErrorCode().getCode(), e.getMessage());
        } catch (Exception e) {
            logger.error("[sendTransaction]: unknown error with input argument {}",
                sendTransactionJsonArgs,
                e);
            String errorMsg = StringUtils.isBlank(e.getMessage()) ? "" : e.getMessage();
            return new HttpResponseData<>(null, loopBack, HttpReturnCode.UNKNOWN_ERROR.getCode(),
                HttpReturnCode.UNKNOWN_ERROR.getCodeDesc().concat(errorMsg));
        }
    }
    
    protected abstract HttpResponseData<String> doEncodeTransaction(InputArg inputArg);
    protected abstract HttpResponseData<Object> doSendTransaction(InputArg inputArg, TransactionReceipt receipt);
    protected abstract String doGetTo(ReqInput<String> req);
}
