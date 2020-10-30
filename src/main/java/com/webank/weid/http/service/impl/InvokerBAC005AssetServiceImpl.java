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

import com.webank.payment.protocol.base.*;
import com.webank.payment.protocol.request.SendAssetArgs;
import com.webank.payment.protocol.response.ResponseData;
import com.webank.payment.rpc.BAC005AssetService;
import com.webank.weid.constant.ErrorCode;
import com.webank.weid.http.protocol.request.FunctionArg;
import com.webank.weid.http.protocol.request.ReqInput;
import com.webank.weid.http.protocol.request.TransactionArg;
import com.webank.weid.http.protocol.response.HttpResponseData;
import com.webank.weid.http.service.BaseService;
import com.webank.weid.http.service.InvokerBAC005AssetService;
import com.webank.weid.rpc.WeIdService;
import com.webank.weid.util.WeIdUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Component
public class InvokerBAC005AssetServiceImpl extends BaseService implements InvokerBAC005AssetService {

    private BAC005AssetService bac005Service;
    private WeIdService weIdService;

    @Autowired
    public void setBac005Service(BAC005AssetService bac005Service) {
        this.bac005Service = bac005Service;
    }

    @Autowired
    public void setWeIdService(WeIdService weIdService) {
        this.weIdService = weIdService;
    }

    @Override
    public HttpResponseData<Object> construct(ReqInput inputArg) {
        FunctionArg functionArg = inputArg.getFunctionArg();
        TransactionArg transactionArg = inputArg.getTransactionArg();
        // 获取用户身份信息
        Authentication auth = super.getAuthentication(transactionArg.getInvokerWeId());
        // 调用资产发布
        ResponseData<BaseAsset> res = this.bac005Service.construct(
            functionArg.getShortName(), 
            functionArg.getDescription(), 
            auth
        );
        return new HttpResponseData<>(res.getResult(), res.getErrorCode(), res.getErrorMessage());
    }

    @Override
    public HttpResponseData<Object> issue(ReqInput inputArg) {
        FunctionArg functionArg = inputArg.getFunctionArg();
        TransactionArg transactionArg = inputArg.getTransactionArg();

        com.webank.weid.protocol.response.ResponseData<Boolean> weIdExist = 
            this.weIdService.isWeIdExist(functionArg.getRecipient());
        if (!weIdExist.getResult()) {
            return new HttpResponseData<>(
                weIdExist.getResult(), 
                ErrorCode.WEID_DOES_NOT_EXIST.getCode(), 
                ErrorCode.WEID_DOES_NOT_EXIST.getCodeDesc()
            );
        }
        
        // 获取用户身份信息
        Authentication auth = super.getAuthentication(transactionArg.getInvokerWeId());
        BAC005AssetInfo bac005AssetInfo = new BAC005AssetInfo();
        bac005AssetInfo.setUserAddress(WeIdUtils.convertWeIdToAddress(functionArg.getRecipient()));
        bac005AssetInfo.setAssetId(BigInteger.valueOf(functionArg.getAssetId()));
        bac005AssetInfo.setAssetUri(functionArg.getAssetUri());
        bac005AssetInfo.setData(functionArg.getData());

        ResponseData<Boolean> res = this.bac005Service.issueAsset(
                functionArg.getAssetAddress(),
                bac005AssetInfo,
                auth
        );
        return new HttpResponseData<>(res.getResult(), res.getErrorCode(), res.getErrorMessage());
    }

    @Override
    public HttpResponseData<Object> constructAndIssue(ReqInput inputArg) {
        HttpResponseData<Object> publishRes = this.construct(inputArg);
        if (publishRes.getErrorCode() == ErrorCode.SUCCESS.getCode()) {
            BaseAsset assetInfo = (BaseAsset)publishRes.getRespBody();
            inputArg.getFunctionArg().setAssetAddress(String.valueOf(assetInfo.getAssetAddress()));
            HttpResponseData<Object> issueRes = this.issue(inputArg);
            return new HttpResponseData<>(
                    publishRes.getRespBody(), issueRes.getErrorCode(), issueRes.getErrorMessage());
        }
        return publishRes;
    }

    @Override
    public HttpResponseData<Object> batchIssue(ReqInput inputArg) {
        FunctionArg functionArg = inputArg.getFunctionArg();
        TransactionArg transactionArg = inputArg.getTransactionArg();

        com.webank.weid.protocol.response.ResponseData<Boolean> weIdExist =
                this.weIdService.isWeIdExist(functionArg.getRecipient());
        if (!weIdExist.getResult()) {
            return new HttpResponseData<>(
                    weIdExist.getResult(),
                    ErrorCode.WEID_DOES_NOT_EXIST.getCode(),
                    ErrorCode.WEID_DOES_NOT_EXIST.getCodeDesc()
            );
        }

        // 获取用户身份信息
        Authentication auth = super.getAuthentication(transactionArg.getInvokerWeId());
        BAC005AssetInfo bac005AssetInfo = new BAC005AssetInfo();
        bac005AssetInfo.setUserAddress(WeIdUtils.convertWeIdToAddress(functionArg.getRecipient()));
        bac005AssetInfo.setAssetId(BigInteger.valueOf(functionArg.getAssetId()));
        bac005AssetInfo.setAssetUri(functionArg.getAssetUri());
        bac005AssetInfo.setData(functionArg.getData());

        ResponseData<Boolean> res = this.bac005Service.batchIssueAsset(
                functionArg.getAssetAddress(),
                functionArg.getBac005AssetInfoList(),
                auth
        );
        return new HttpResponseData<>(res.getResult(), res.getErrorCode(), res.getErrorMessage());
    }

    @Override
    public HttpResponseData<Object> constructAndBatchIssue(ReqInput inputArg) {
        HttpResponseData<Object> publishRes = this.construct(inputArg);
        if (publishRes.getErrorCode() == ErrorCode.SUCCESS.getCode()) {
            return this.batchIssue(inputArg);
        }
        return publishRes;
    }

    @Override
    public HttpResponseData<Object> queryAssetOwner(ReqInput inputArg) {
        FunctionArg functionArg = inputArg.getFunctionArg();
        ResponseData<String> assetOwner = this.bac005Service.queryAssetOwner(
                functionArg.getAssetAddress(),
                BigInteger.valueOf(functionArg.getAssetId()));

        String owner =  WeIdUtils.convertAddressToWeId(assetOwner.getResult());
        return new HttpResponseData<>(owner, assetOwner.getErrorCode(), assetOwner.getErrorMessage());
    }

    @Override
    public HttpResponseData<Object> queryAssetNum(ReqInput inputArg) {
        FunctionArg functionArg = inputArg.getFunctionArg();
        ResponseData<BAC005AssetNum> assetNum = this.bac005Service.queryAssetNum(functionArg.getAssetAddress());
        return new HttpResponseData<>(assetNum.getResult(), assetNum.getErrorCode(), assetNum.getErrorMessage());
    }

    @Override
    public HttpResponseData<Object> queryAssetList(ReqInput inputArg) {
        FunctionArg functionArg = inputArg.getFunctionArg();
        ResponseData<List<BAC005AssetInfo>> assetList = this.bac005Service.queryAssetList(
                functionArg.getAssetAddress(),
                functionArg.getIndex(),
                functionArg.getNum());
        return new HttpResponseData<>(assetList.getResult(), assetList.getErrorCode(), assetList.getErrorMessage());
    }

    @Override
    public HttpResponseData<Object> queryOwnedAssetNum(ReqInput inputArg) {
        FunctionArg functionArg = inputArg.getFunctionArg();
        ResponseData<BAC005AssetNum> ownedAssetNum = this.bac005Service.queryOwnedAssetNum(
                functionArg.getAssetAddress(),
                WeIdUtils.convertWeIdToAddress(functionArg.getAssetHolder()));

        return new HttpResponseData<>(
                ownedAssetNum.getResult(),
                ownedAssetNum.getErrorCode(),
                ownedAssetNum.getErrorMessage());
    }

    @Override
    public HttpResponseData<Object> queryOwnedAssetList(ReqInput inputArg) {
        FunctionArg functionArg = inputArg.getFunctionArg();
        ResponseData<List<BAC005AssetInfo>> ownedAssetList = this.bac005Service.queryOwnedAssetList(
                WeIdUtils.convertWeIdToAddress(functionArg.getAssetHolder()),
                functionArg.getAssetHolder(),
                functionArg.getIndex(),
                functionArg.getNum());

        return new HttpResponseData<>(
                ownedAssetList.getResult(),
                ownedAssetList.getErrorCode(),
                ownedAssetList.getErrorMessage());
    }

    @Override
    public HttpResponseData<Object> send(ReqInput inputArg) {
        FunctionArg functionArg = inputArg.getFunctionArg();
        TransactionArg transactionArg = inputArg.getTransactionArg();
        com.webank.weid.protocol.response.ResponseData<Boolean> weIdExist =
                this.weIdService.isWeIdExist(functionArg.getRecipient());
        if (!weIdExist.getResult()) {
            return new HttpResponseData<>(
                weIdExist.getResult(), 
                ErrorCode.WEID_DOES_NOT_EXIST.getCode(), 
                ErrorCode.WEID_DOES_NOT_EXIST.getCodeDesc()
            );
        }
            
        // 获取用户身份信息
        Authentication auth = super.getAuthentication(transactionArg.getInvokerWeId());
        SendAssetArgs sendAssetArgs = new SendAssetArgs();
        sendAssetArgs.setAmount(BigInteger.valueOf(functionArg.getAssetId()));
        sendAssetArgs.setRecipient(WeIdUtils.convertWeIdToAddress(functionArg.getRecipient()));
        sendAssetArgs.setData(functionArg.getData());
        ResponseData<Boolean> res = this.bac005Service.sendAsset(
                functionArg.getAssetAddress(),
                sendAssetArgs,
                auth
        );
        return new HttpResponseData<>(res.getResult(), res.getErrorCode(), res.getErrorMessage());
    }

    @Override
    public HttpResponseData<Object> batchSend(ReqInput inputArg) {
        FunctionArg functionArg = inputArg.getFunctionArg();
        TransactionArg transactionArg = inputArg.getTransactionArg();
        // 获取用户身份信息
        Authentication auth = super.getAuthentication(transactionArg.getInvokerWeId());
        List<SendAssetArgs> sendAssetArgList = new ArrayList<>();
        List<FunctionArg> objectList = functionArg.getObjectList();
        objectList.forEach(input -> {
            SendAssetArgs sendAssetArgs = new SendAssetArgs();
            sendAssetArgs.setAmount(BigInteger.valueOf(input.getAssetId()));
            sendAssetArgs.setRecipient(WeIdUtils.convertWeIdToAddress(input.getRecipient()));
            sendAssetArgs.setData(functionArg.getData());
            sendAssetArgList.add(sendAssetArgs);
        });

        ResponseData<Boolean> res = this.bac005Service.batchSendAsset(
                functionArg.getAssetAddress(),
                sendAssetArgList,
                functionArg.getData(),
                auth
        );
        return new HttpResponseData<>(res.getResult(), res.getErrorCode(), res.getErrorMessage());
    }

    @Override
    public HttpResponseData<Object> queryBaseInfo(ReqInput inputArg) {
        FunctionArg functionArg = inputArg.getFunctionArg();
        ResponseData<List<BAC005BaseInfo>> res = this.bac005Service.queryBaseInfo(
                functionArg.getAssetAddressList()
        );
        return new HttpResponseData<>(res.getResult(), res.getErrorCode(), res.getErrorMessage());
    }

    @Override
    public HttpResponseData<Object> queryBaseInfoByWeId(ReqInput inputArg) {
        FunctionArg functionArg = inputArg.getFunctionArg();
        ResponseData<List<BAC005BaseInfo>> res = this.bac005Service.queryBaseInfoByWeId(
                WeIdUtils.convertWeIdToAddress(functionArg.getAssetHolder()),
                functionArg.getIndex(),
                functionArg.getNum()
        );
        return new HttpResponseData<>(res.getResult(), res.getErrorCode(), res.getErrorMessage());
    }
}
