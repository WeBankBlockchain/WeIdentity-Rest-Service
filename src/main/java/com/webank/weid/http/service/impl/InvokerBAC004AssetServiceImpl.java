/*
 *       Copyright© (2019) WeBank Co., Ltd.
 *
 *       This file is part of weid-http-service.
 *
 *       weid-http-service is free software: you can redistribute it and/or modify
 *       it under the terms of the GNU Lesser General Public License as published by
 *       the Free Software Foundation, either version 3 of the License, or
 *       (at your option) any later version.
 *
 *       weid-http-service is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU Lesser General Public License for more details.
 *
 *       You should have received a copy of the GNU Lesser General Public License
 *       along with weid-http-service.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.webank.weid.http.service.impl;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.fisco.bcos.web3j.protocol.core.methods.response.TransactionReceipt;
import org.springframework.stereotype.Component;

import com.webank.payment.protocol.base.Authentication;
import com.webank.payment.protocol.base.BAC004Balance;
import com.webank.payment.protocol.base.BAC004BaseInfo;
import com.webank.payment.protocol.base.BaseAsset;
import com.webank.payment.protocol.request.SendAssetArgs;
import com.webank.payment.protocol.response.ResponseData;
import com.webank.payment.rpc.BAC004AssetService;
import com.webank.payment.service.impl.BAC004AssetServiceImpl;
import com.webank.weid.constant.ErrorCode;
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
import com.webank.weid.rpc.WeIdService;
import com.webank.weid.service.impl.WeIdServiceImpl;
import com.webank.weid.util.WeIdUtils;

@Component
public class InvokerBAC004AssetServiceImpl 
    extends BaseService 
    implements InvokerBAC004AssetService  {

    private BAC004AssetService bac004Service;
    private WeIdService weIdService = new WeIdServiceImpl();

    private BAC004AssetService getBac004Service() {
        if (bac004Service == null) {
            bac004Service = new BAC004AssetServiceImpl();
        }
        return bac004Service;
    }

    @Override
    public HttpResponseData<Object> construct(ReqInput<BAC004Info> inputArg) {
        BAC004Info functionArg = inputArg.getFunctionArg();
        TransactionArg transactionArg = inputArg.getTransactionArg();
        // 获取用户身份信息
        Authentication auth = super.getAuthentication(transactionArg.getInvokerWeId());
        // 调用资产发布
        ResponseData<BaseAsset> res = getBac004Service().construct(
            functionArg.getShortName(), 
            functionArg.getDescription(), 
            auth
        );
        return new HttpResponseData<>(res.getResult(), res.getErrorCode(), res.getErrorMessage());
    }

    @Override
    public HttpResponseData<Object> issue(ReqInput<BAC004Info> inputArg) {
        BAC004Info functionArg = inputArg.getFunctionArg();
        TransactionArg transactionArg = inputArg.getTransactionArg();

        HttpResponseData<Object> checkWeIdExistRsp = 
            super.checkWeIdExist(this.weIdService, functionArg.getRecipient());
        if (Objects.nonNull(checkWeIdExistRsp)) {
            return new HttpResponseData<>(
                StringUtils.EMPTY, 
                ErrorCode.WEID_DOES_NOT_EXIST.getCode(), 
                ErrorCode.WEID_DOES_NOT_EXIST.getCodeDesc()
            );
        };
        
        // 获取用户身份信息
        Authentication auth = super.getAuthentication(transactionArg.getInvokerWeId());
        ResponseData<Boolean> res = getBac004Service().issue(
            functionArg.getAssetAddress(), 
            WeIdUtils.convertWeIdToAddress(functionArg.getRecipient()), 
            BigInteger.valueOf(functionArg.getAmount()), 
            functionArg.getData(), 
            auth
        );
        return new HttpResponseData<>(res.getResult(), res.getErrorCode(), res.getErrorMessage());
    }

    @Override
    public HttpResponseData<Object> constructAndIssue(ReqInput<BAC004Info> inputArg) {
        HttpResponseData<Object> puhlishRes = construct(inputArg);
        if (puhlishRes.getErrorCode().intValue() ==  ErrorCode.SUCCESS.getCode()) {
            BaseAsset assetInfo = (BaseAsset)puhlishRes.getRespBody();
            inputArg.getFunctionArg().setAssetAddress(String.valueOf(assetInfo.getAssetAddress()));
            HttpResponseData<Object> issueRes = issue(inputArg);
            return new HttpResponseData<>(
                puhlishRes.getRespBody(), 
                issueRes.getErrorCode(), 
                issueRes.getErrorMessage()
            );
        }
        return puhlishRes;
    }

    @Override
    public HttpResponseData<Object> getBalance(ReqInput<BaseQuery> inputArg) {
        BaseQuery functionArg = inputArg.getFunctionArg();
        ResponseData<BAC004Balance> res = getBac004Service().getBalance(
            functionArg.getAssetAddress(), 
            WeIdUtils.convertWeIdToAddress(functionArg.getAssetHolder())
        );
        return new HttpResponseData<>(res.getResult(), res.getErrorCode(), res.getErrorMessage());
    }

    @Override
    public HttpResponseData<Object> getBatchBalance(ReqInput<AssetAddressList> inputArg) {
        AssetAddressList functionArg = inputArg.getFunctionArg();
        ResponseData<List<BAC004Balance>> res = getBac004Service().getBatchBalance(
            functionArg.getAssetAddressList(), 
            WeIdUtils.convertWeIdToAddress(functionArg.getAssetHolder())
        );
        return new HttpResponseData<>(res.getResult(), res.getErrorCode(), res.getErrorMessage());
    }

    @Override
    public HttpResponseData<Object> getBalanceByWeId(ReqInput<PageQuery> inputArg) {
        PageQuery functionArg = inputArg.getFunctionArg();
        ResponseData<List<BAC004Balance>> res = getBac004Service().getBalanceByWeId(
            WeIdUtils.convertWeIdToAddress(functionArg.getAssetHolder()),
            functionArg.getIndex(), 
            functionArg.getNum()
        );
        return new HttpResponseData<>(res.getResult(), res.getErrorCode(), res.getErrorMessage());
    }

    @Override
    public HttpResponseData<Object> send(ReqInput<BAC004SendInfo> inputArg) {
        BAC004SendInfo functionArg = inputArg.getFunctionArg();
        TransactionArg transactionArg = inputArg.getTransactionArg();
        HttpResponseData<Object> checkWeIdExistRsp = 
            super.checkWeIdExist(this.weIdService, functionArg.getRecipient());
        if (Objects.nonNull(checkWeIdExistRsp)) {
            return new HttpResponseData<>(
                StringUtils.EMPTY, 
                ErrorCode.WEID_DOES_NOT_EXIST.getCode(), 
                ErrorCode.WEID_DOES_NOT_EXIST.getCodeDesc()
            );
        };
            
        // 获取用户身份信息
        Authentication auth = super.getAuthentication(transactionArg.getInvokerWeId());
        SendAssetArgs sendAssetArgs = new  SendAssetArgs();
        sendAssetArgs.setAmount(BigInteger.valueOf(functionArg.getAmount()));
        sendAssetArgs.setRecipient(WeIdUtils.convertWeIdToAddress(functionArg.getRecipient()));
        sendAssetArgs.setData(functionArg.getData());
        ResponseData<Boolean> res = getBac004Service().send(
            functionArg.getAssetAddress(), 
            sendAssetArgs, 
            auth
        );
        return new HttpResponseData<>(res.getResult(), res.getErrorCode(), res.getErrorMessage());
    }

    @Override
    public HttpResponseData<Object> batchSend(ReqInput<BAC004BatchSendInfo> inputArg) {
        BAC004BatchSendInfo functionArg = inputArg.getFunctionArg();
        TransactionArg transactionArg = inputArg.getTransactionArg();
        // 获取用户身份信息
        Authentication auth = super.getAuthentication(transactionArg.getInvokerWeId());
        List<SendAssetArgs> sendAssetArgList = new ArrayList<SendAssetArgs>();
        List<BAC004SendInfo> objectList = functionArg.getList();
        for (BAC004SendInfo input : objectList) {
            SendAssetArgs sendAssetArgs = new  SendAssetArgs();
            sendAssetArgs.setAmount(BigInteger.valueOf(input.getAmount()));
            sendAssetArgs.setRecipient(WeIdUtils.convertWeIdToAddress(input.getRecipient()));
            sendAssetArgs.setData(input.getData());
            sendAssetArgList.add(sendAssetArgs);
            HttpResponseData<Object> checkWeIdExistRsp = 
                super.checkWeIdExist(this.weIdService, input.getRecipient());
            if (Objects.nonNull(checkWeIdExistRsp)) {
                return new HttpResponseData<>(
                    StringUtils.EMPTY, 
                    ErrorCode.WEID_DOES_NOT_EXIST.getCode(), 
                    ErrorCode.WEID_DOES_NOT_EXIST.getCodeDesc()
                );
            };
        }
        ResponseData<Boolean> res = getBac004Service().batchSend(
            functionArg.getAssetAddress(), 
            sendAssetArgList, 
            auth
        );
        return new HttpResponseData<>(res.getResult(), res.getErrorCode(), res.getErrorMessage());
    }

    @Override
    public HttpResponseData<Object> getBaseInfo(ReqInput<AssetAddressList> inputArg) {
        AssetAddressList functionArg = inputArg.getFunctionArg();
        ResponseData<List<BAC004BaseInfo>> res = getBac004Service().getBaseInfo(
            functionArg.getAssetAddressList()
        );
        return new HttpResponseData<>(res.getResult(), res.getErrorCode(), res.getErrorMessage());
    }

    @Override
    public HttpResponseData<Object> getBaseInfoByWeId(ReqInput<PageQuery> inputArg) {
        PageQuery functionArg = inputArg.getFunctionArg();
        ResponseData<List<BAC004BaseInfo>> res = getBac004Service().getBaseInfoByWeId(
            WeIdUtils.convertWeIdToAddress(functionArg.getAssetHolder()),
            functionArg.getIndex(), 
            functionArg.getNum()
        );
        return new HttpResponseData<>(res.getResult(), res.getErrorCode(), res.getErrorMessage());
    }
    
    @Override
    public HttpResponseData<String> issueEncoder(ReqInput<BAC004Info> inputArg) {
        BAC004Info functionArg = inputArg.getFunctionArg();
        HttpResponseData<Object> checkWeIdExistRsp = 
            super.checkWeIdExist(this.weIdService, functionArg.getRecipient());
        if (Objects.nonNull(checkWeIdExistRsp)) {
            return new HttpResponseData<>(
                StringUtils.EMPTY, 
                ErrorCode.WEID_DOES_NOT_EXIST.getCode(), 
                ErrorCode.WEID_DOES_NOT_EXIST.getCodeDesc()
            );
        };
        ResponseData<String> res = getBac004Service().issueEncoder(
            WeIdUtils.convertWeIdToAddress(functionArg.getRecipient()), 
            BigInteger.valueOf(functionArg.getAmount()), 
            functionArg.getData()
        );
        return new HttpResponseData<>(res.getResult(), res.getErrorCode(), res.getErrorMessage());
    }

    @Override
    public HttpResponseData<Object> issueDeCoder(TransactionReceipt receipt) {
        ResponseData<Boolean> res = getBac004Service().issueDecoder(receipt);
        return new HttpResponseData<>(res.getResult(), res.getErrorCode(), res.getErrorMessage());
    }
    
    @Override
    public HttpResponseData<String> constructEncoder(ReqInput<BAC004Info> inputArg) {
        BAC004Info functionArg = inputArg.getFunctionArg();
        ResponseData<String> res = getBac004Service().constructEncoder(
            functionArg.getShortName(), 
            functionArg.getDescription()
        );
        return new HttpResponseData<>(res.getResult(), res.getErrorCode(), res.getErrorMessage());
    }

    @Override
    public HttpResponseData<Object> constructDeCoder(TransactionReceipt receipt) {
        ResponseData<String> res = getBac004Service().constructDecoder(receipt);
        return new HttpResponseData<>(res.getResult(), res.getErrorCode(), res.getErrorMessage());
    }

    @Override
    public HttpResponseData<String> sendEncoder(ReqInput<BAC004SendInfo> inputArg) {
        BAC004SendInfo functionArg = inputArg.getFunctionArg();
        HttpResponseData<Object> checkWeIdExistRsp = 
            super.checkWeIdExist(this.weIdService, functionArg.getRecipient());
        if (Objects.nonNull(checkWeIdExistRsp)) {
            return new HttpResponseData<>(
                StringUtils.EMPTY, 
                ErrorCode.WEID_DOES_NOT_EXIST.getCode(), 
                ErrorCode.WEID_DOES_NOT_EXIST.getCodeDesc()
            );
        };

        // 获取用户身份信息
        SendAssetArgs sendAssetArgs = new  SendAssetArgs();
        sendAssetArgs.setAmount(BigInteger.valueOf(functionArg.getAmount()));
        sendAssetArgs.setRecipient(WeIdUtils.convertWeIdToAddress(functionArg.getRecipient()));
        sendAssetArgs.setData(functionArg.getData());
        ResponseData<String> res = getBac004Service().sendEncoder(sendAssetArgs);
        return new HttpResponseData<>(res.getResult(), res.getErrorCode(), res.getErrorMessage());
    }

    @Override
    public HttpResponseData<Object> sendDecoder(TransactionReceipt receipt) {
        ResponseData<Boolean> res = getBac004Service().sendDecoder(receipt);
        return new HttpResponseData<>(res.getResult(), res.getErrorCode(), res.getErrorMessage());
    }

    @Override
    public HttpResponseData<String> batchSendEncoder(ReqInput<BAC004BatchSendInfo> inputArg) {
        BAC004BatchSendInfo functionArg = inputArg.getFunctionArg();
        // 获取用户身份信息
        List<SendAssetArgs> sendAssetArgList = new ArrayList<SendAssetArgs>();
        List<BAC004SendInfo> objectList = functionArg.getList();
        for (BAC004SendInfo input : objectList) {
            SendAssetArgs sendAssetArgs = new  SendAssetArgs();
            sendAssetArgs.setAmount(BigInteger.valueOf(input.getAmount()));
            sendAssetArgs.setRecipient(WeIdUtils.convertWeIdToAddress(input.getRecipient()));
            sendAssetArgs.setData(input.getData());
            sendAssetArgList.add(sendAssetArgs);
            HttpResponseData<Object> checkWeIdExistRsp = 
                super.checkWeIdExist(this.weIdService, input.getRecipient());
            if (Objects.nonNull(checkWeIdExistRsp)) {
                return new HttpResponseData<>(
                    StringUtils.EMPTY, 
                    ErrorCode.WEID_DOES_NOT_EXIST.getCode(), 
                    ErrorCode.WEID_DOES_NOT_EXIST.getCodeDesc()
                );
            };
        }
        ResponseData<String> res = getBac004Service().batchSendEncoder(sendAssetArgList);
        return new HttpResponseData<>(res.getResult(), res.getErrorCode(), res.getErrorMessage());
    }

    @Override
    public HttpResponseData<Object> batchSendDecoder(TransactionReceipt receipt) {
        ResponseData<Boolean> res = getBac004Service().batchSendDecoder(receipt);
        return new HttpResponseData<>(res.getResult(), res.getErrorCode(), res.getErrorMessage());
    }
}
