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
import com.webank.payment.service.impl.BAC005AssetServiceImpl;
import com.webank.weid.constant.ErrorCode;
import com.webank.weid.http.protocol.request.ReqInput;
import com.webank.weid.http.protocol.request.TransactionArg;
import com.webank.weid.http.protocol.request.payment.*;
import com.webank.weid.http.protocol.response.HttpResponseData;
import com.webank.weid.http.service.BaseService;
import com.webank.weid.http.service.InvokerBAC005AssetService;
import com.webank.weid.rpc.WeIdService;
import com.webank.weid.util.WeIdUtils;

import org.apache.commons.lang3.StringUtils;
import org.fisco.bcos.web3j.protocol.core.methods.response.TransactionReceipt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class InvokerBAC005AssetServiceImpl extends BaseService implements InvokerBAC005AssetService {

    @Autowired
    private WeIdService weIdService ;
    private BAC005AssetService bac005Service;
    
    private BAC005AssetService getBac005AssetService() {
        if (bac005Service == null) {
            bac005Service = new BAC005AssetServiceImpl();
        }
        return bac005Service;
    }

    @Override
    public HttpResponseData<Object> construct(ReqInput<BAC005Info> inputArg) {
        BAC005Info functionArg = inputArg.getFunctionArg();
        TransactionArg transactionArg = inputArg.getTransactionArg();

        return this.constructAsset(
                transactionArg.getInvokerWeId(),
                functionArg.getShortName(),
                functionArg.getDescription()
        );
    }

    private HttpResponseData<Object> constructAsset(String invokerWeId, String shortName, String description) {
        Authentication auth = super.getAuthentication(invokerWeId);
        ResponseData<BaseAsset> res = this.getBac005AssetService().construct(shortName, description, auth);
        return new HttpResponseData<>(res.getResult(), res.getErrorCode(), res.getErrorMessage());
    }

    @Override
    public HttpResponseData<Object> issue(ReqInput<BAC005Info> inputArg) {
        BAC005Info functionArg = inputArg.getFunctionArg();
        TransactionArg transactionArg = inputArg.getTransactionArg();

        return this.issueAsset(
                functionArg.getAssetAddress(),
                transactionArg.getInvokerWeId(),
                functionArg.getRecipient(),
                functionArg.getAssetId(),
                functionArg.getAssetUri(),
                functionArg.getRemark());
    }

    private HttpResponseData<Object> issueAsset(
            String assetAddress,
            String invokerWeId,
            String recipient,
            Integer assetId,
            String assetUri,
            String data) {
        HttpResponseData<Object> checkWeIdExistRsp = super.checkWeIdExist(this.weIdService, recipient);
        if (Objects.nonNull(checkWeIdExistRsp)) return checkWeIdExistRsp;

        Authentication auth = super.getAuthentication(invokerWeId);
        BAC005AssetInfo bac005AssetInfo = new BAC005AssetInfo();
        bac005AssetInfo.setUserAddress(WeIdUtils.convertWeIdToAddress(recipient));
        bac005AssetInfo.setAssetId(BigInteger.valueOf(assetId));
        bac005AssetInfo.setAssetUri(assetUri);
        bac005AssetInfo.setData(data);

        ResponseData<Boolean> res = this.getBac005AssetService().issueAsset(assetAddress, bac005AssetInfo, auth);
        return new HttpResponseData<>(res.getResult(), res.getErrorCode(), res.getErrorMessage());
    }

    @Override
    public HttpResponseData<Object> constructAndIssue(ReqInput<BAC005Info> inputArg) {
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
    public HttpResponseData<Object> batchIssue(ReqInput<BAC005BatchInfo> inputArg) {
        BAC005BatchInfo functionArg = inputArg.getFunctionArg();
        TransactionArg transactionArg = inputArg.getTransactionArg();

        List<BAC005AssetInfo> assetInfoList = new ArrayList<>();
        List<BAC005Info> bac005InfoList = functionArg.getList();
        HttpResponseData<Object> checkWeIdExistRsp = null;
        for (BAC005Info sendInfo : bac005InfoList) {
            checkWeIdExistRsp = super.checkWeIdExist(this.weIdService, sendInfo.getRecipient());
            if (Objects.nonNull(checkWeIdExistRsp)) break;
            BAC005AssetInfo assetInfo = new BAC005AssetInfo();
            assetInfo.setAssetId(BigInteger.valueOf(sendInfo.getAssetId()));
            assetInfo.setAssetUri(sendInfo.getAssetUri());
            assetInfo.setUserAddress(WeIdUtils.convertWeIdToAddress(sendInfo.getRecipient()));
            assetInfo.setData(sendInfo.getRemark());
            assetInfoList.add(assetInfo);
        }

        if (Objects.nonNull(checkWeIdExistRsp)) return checkWeIdExistRsp;

        Authentication auth = super.getAuthentication(transactionArg.getInvokerWeId());
        ResponseData<Boolean> res = this.getBac005AssetService().batchIssueAsset(
                functionArg.getAssetAddress(),
                assetInfoList,
                auth
        );
        return new HttpResponseData<>(res.getResult(), res.getErrorCode(), res.getErrorMessage());
    }

    @Override
    public HttpResponseData<Object> constructAndBatchIssue(ReqInput<BAC005BatchInfo> inputArg) {
        BAC005BatchInfo bac005BatchInfo = inputArg.getFunctionArg();
        HttpResponseData<Object> publishRes = this.constructAsset(
                inputArg.getTransactionArg().getInvokerWeId(),
                bac005BatchInfo.getShortName(),
                bac005BatchInfo.getDescription());
        if (publishRes.getErrorCode() == ErrorCode.SUCCESS.getCode()) {
            BaseAsset respBody = (BaseAsset) publishRes.getRespBody();
            inputArg.getFunctionArg().setAssetAddress(respBody.getAssetAddress());
            HttpResponseData<Object> batchIssueRsp = this.batchIssue(inputArg);
            if (batchIssueRsp.getErrorCode() != ErrorCode.SUCCESS.getCode()) {
                return batchIssueRsp;
            }
        }
        return publishRes;
    }

    @Override
    public HttpResponseData<Object> queryAssetOwner(ReqInput<BAC005Info> inputArg) {
        BAC005Info functionArg = inputArg.getFunctionArg();
        ResponseData<String> assetOwner = this.getBac005AssetService().queryAssetOwner(
                functionArg.getAssetAddress(),
                BigInteger.valueOf(functionArg.getAssetId()));

        String owner =  WeIdUtils.convertAddressToWeId(assetOwner.getResult());
        return new HttpResponseData<>(owner, assetOwner.getErrorCode(), assetOwner.getErrorMessage());
    }

    @Override
    public HttpResponseData<Object> queryAssetNum(ReqInput<BaseAsset> inputArg) {
        BaseAsset functionArg = inputArg.getFunctionArg();
        ResponseData<BAC005AssetNum> assetNum = this.getBac005AssetService().queryAssetNum(functionArg.getAssetAddress());
        return new HttpResponseData<>(assetNum.getResult(), assetNum.getErrorCode(), assetNum.getErrorMessage());
    }

    @Override
    public HttpResponseData<Object> queryAssetList(ReqInput<PageQuery> inputArg) {
        PageQuery functionArg = inputArg.getFunctionArg();
        ResponseData<List<BAC005AssetInfo>> assetList = this.getBac005AssetService().queryAssetList(
                functionArg.getAssetAddress(),
                functionArg.getIndex(),
                functionArg.getNum());
        return new HttpResponseData<>(assetList.getResult(), assetList.getErrorCode(), assetList.getErrorMessage());
    }

    @Override
    public HttpResponseData<Object> queryOwnedAssetNum(ReqInput<BaseQuery> inputArg) {
        BaseQuery functionArg = inputArg.getFunctionArg();

        HttpResponseData<Object> checkWeIdExistRsp = super.checkWeIdExist(this.weIdService, functionArg.getAssetHolder());
        if (Objects.nonNull(checkWeIdExistRsp)) return checkWeIdExistRsp;
        ResponseData<BAC005AssetNum> ownedAssetNum = this.getBac005AssetService().queryOwnedAssetNum(
                functionArg.getAssetAddress(),
                WeIdUtils.convertWeIdToAddress(functionArg.getAssetHolder()));

        return new HttpResponseData<>(
                ownedAssetNum.getResult(),
                ownedAssetNum.getErrorCode(),
                ownedAssetNum.getErrorMessage());
    }

    @Override
    public HttpResponseData<Object> queryOwnedAssetList(ReqInput<PageQuery> inputArg) {
        PageQuery functionArg = inputArg.getFunctionArg();
        ResponseData<List<BAC005AssetInfo>> ownedAssetList = this.getBac005AssetService().queryOwnedAssetList(
                functionArg.getAssetAddress(),
                WeIdUtils.convertWeIdToAddress(functionArg.getAssetHolder()),
                functionArg.getIndex(),
                functionArg.getNum());

        return new HttpResponseData<>(
                ownedAssetList.getResult(),
                ownedAssetList.getErrorCode(),
                ownedAssetList.getErrorMessage());
    }

    @Override
    public HttpResponseData<Object> send(ReqInput<BAC005Info> inputArg) {
        BAC005Info functionArg = inputArg.getFunctionArg();
        TransactionArg transactionArg = inputArg.getTransactionArg();
        HttpResponseData<Object> checkWeIdExistRsp = super.checkWeIdExist(
                this.weIdService, functionArg.getRecipient());
        if (Objects.nonNull(checkWeIdExistRsp)) return checkWeIdExistRsp;

        Authentication auth = super.getAuthentication(transactionArg.getInvokerWeId());
        SendAssetArgs sendAssetArgs = new SendAssetArgs();
        sendAssetArgs.setAmount(BigInteger.valueOf(functionArg.getAssetId()));
        sendAssetArgs.setRecipient(WeIdUtils.convertWeIdToAddress(functionArg.getRecipient()));
        sendAssetArgs.setData(functionArg.getRemark());
        ResponseData<Boolean> res = this.getBac005AssetService().sendAsset(
                functionArg.getAssetAddress(),
                sendAssetArgs,
                auth
        );
        return new HttpResponseData<>(res.getResult(), res.getErrorCode(), res.getErrorMessage());
    }

    @Override
    public HttpResponseData<Object> batchSend(ReqInput<BAC005BatchInfo> inputArg) {
        BAC005BatchInfo functionArg = inputArg.getFunctionArg();
        TransactionArg transactionArg = inputArg.getTransactionArg();
        Authentication auth = super.getAuthentication(transactionArg.getInvokerWeId());
        List<SendAssetArgs> sendAssetArgList = new ArrayList<>();
        List<BAC005Info> objectList = functionArg.getList();
        HttpResponseData<Object> checkWeIdExistRsp = null;
        for (BAC005Info bac005Info : objectList) {
            SendAssetArgs sendAssetArgs = new SendAssetArgs();
            sendAssetArgs.setAmount(BigInteger.valueOf(bac005Info.getAssetId()));
            checkWeIdExistRsp = super.checkWeIdExist(this.weIdService, bac005Info.getRecipient());
            if (Objects.nonNull(checkWeIdExistRsp)) break;
            sendAssetArgs.setRecipient(WeIdUtils.convertWeIdToAddress(bac005Info.getRecipient()));
            sendAssetArgs.setData(bac005Info.getRemark());
            sendAssetArgList.add(sendAssetArgs);
        }

        if (Objects.nonNull(checkWeIdExistRsp)) return checkWeIdExistRsp;

        ResponseData<Boolean> res = this.getBac005AssetService().batchSendAsset(
            functionArg.getAssetAddress(),
            sendAssetArgList,
            functionArg.getRemark(),
            auth
        );
        return new HttpResponseData<>(res.getResult(), res.getErrorCode(), res.getErrorMessage());
    }

    @Override
    public HttpResponseData<Object> queryBaseInfo(ReqInput<AssetAddressList> inputArg) {
        AssetAddressList functionArg = inputArg.getFunctionArg();
        ResponseData<List<BAC005BaseInfo>> res = this.getBac005AssetService().queryBaseInfo(
                functionArg.getAssetAddressList()
        );
        return new HttpResponseData<>(res.getResult(), res.getErrorCode(), res.getErrorMessage());
    }

    @Override
    public HttpResponseData<Object> queryBaseInfoByWeId(ReqInput<PageQuery> inputArg) {
        PageQuery functionArg = inputArg.getFunctionArg();
        ResponseData<List<BAC005BaseInfo>> res = this.getBac005AssetService().queryBaseInfoByWeId(
                WeIdUtils.convertWeIdToAddress(functionArg.getAssetHolder()),
                functionArg.getIndex(),
                functionArg.getNum()
        );
        return new HttpResponseData<>(res.getResult(), res.getErrorCode(), res.getErrorMessage());
    }

    @Override
    public HttpResponseData<String> constructEncoder(ReqInput<BAC005Info> inputArg) {
        BAC005Info functionArg = inputArg.getFunctionArg();
        ResponseData<String> res = getBac005AssetService().constructEncoder(
            functionArg.getShortName(), 
            functionArg.getDescription()
        );
        return new HttpResponseData<>(res.getResult(), res.getErrorCode(), res.getErrorMessage());
    }

    @Override
    public HttpResponseData<Object> constructDeCoder(TransactionReceipt receipt) {
        ResponseData<String> res = getBac005AssetService().constructDecoder(receipt);
        return new HttpResponseData<>(res.getResult(), res.getErrorCode(), res.getErrorMessage());
    }

    @Override
    public HttpResponseData<String> issueEncoder(ReqInput<BAC005Info> inputArg) {
        BAC005Info functionArg = inputArg.getFunctionArg();
        HttpResponseData<Object> checkWeIdExistRsp = super.checkWeIdExist(
            this.weIdService, functionArg.getRecipient());
        if (Objects.nonNull(checkWeIdExistRsp)) {
            return new HttpResponseData<>(
                StringUtils.EMPTY, 
                checkWeIdExistRsp.getErrorCode(), 
                checkWeIdExistRsp.getErrorMessage()
            );
        }
        BAC005AssetInfo bac005AssetInfo = new BAC005AssetInfo();
        bac005AssetInfo.setUserAddress(WeIdUtils.convertWeIdToAddress(functionArg.getRecipient()));
        bac005AssetInfo.setAssetId(BigInteger.valueOf(functionArg.getAssetId()));
        bac005AssetInfo.setAssetUri(functionArg.getAssetUri());
        bac005AssetInfo.setData(functionArg.getRemark());
        ResponseData<String> res = getBac005AssetService().issueAssetEncoder(bac005AssetInfo);
        return new HttpResponseData<>(res.getResult(), res.getErrorCode(), res.getErrorMessage());
    }

    @Override
    public HttpResponseData<Object> issueDeCoder(TransactionReceipt receipt) {
        ResponseData<Boolean> res = getBac005AssetService().issueAssetDecoder(receipt);
        return new HttpResponseData<>(res.getResult(), res.getErrorCode(), res.getErrorMessage());
    }

    @Override
    public HttpResponseData<String> batchIssueEncoder(ReqInput<BAC005BatchInfo> inputArg) {
        BAC005BatchInfo functionArg = inputArg.getFunctionArg();
        List<BAC005AssetInfo> assetInfoList = new ArrayList<>();
        List<BAC005Info> bac005InfoList = functionArg.getList();
        HttpResponseData<Object> checkWeIdExistRsp = null;
        for (BAC005Info sendInfo : bac005InfoList) {
            checkWeIdExistRsp = super.checkWeIdExist(this.weIdService, sendInfo.getRecipient());
            if (Objects.nonNull(checkWeIdExistRsp)) break;
            BAC005AssetInfo assetInfo = new BAC005AssetInfo();
            assetInfo.setAssetId(BigInteger.valueOf(sendInfo.getAssetId()));
            assetInfo.setAssetUri(sendInfo.getAssetUri());
            assetInfo.setUserAddress(WeIdUtils.convertWeIdToAddress(sendInfo.getRecipient()));
            assetInfo.setData(sendInfo.getRemark());
            assetInfoList.add(assetInfo);
        }
        if (Objects.nonNull(checkWeIdExistRsp)) {
            return new HttpResponseData<>(
                StringUtils.EMPTY, 
                checkWeIdExistRsp.getErrorCode(), 
                checkWeIdExistRsp.getErrorMessage()
            );
        }
        
        ResponseData<String> res = getBac005AssetService().batchIssueAssetEncoder(assetInfoList);
        return new HttpResponseData<>(res.getResult(), res.getErrorCode(), res.getErrorMessage());
    }

    @Override
    public HttpResponseData<Object> batchIssueDeCoder(TransactionReceipt receipt) {
        ResponseData<Boolean> res = getBac005AssetService().batchIssueAssetDecoder(receipt);
        return new HttpResponseData<>(res.getResult(), res.getErrorCode(), res.getErrorMessage());
    }

    @Override
    public HttpResponseData<String> sendEncoder(ReqInput<BAC005Info> inputArg) {
        BAC005Info functionArg = inputArg.getFunctionArg();
        SendAssetArgs sendAssetArgs = new SendAssetArgs();
        sendAssetArgs.setAmount(BigInteger.valueOf(functionArg.getAssetId()));
        sendAssetArgs.setRecipient(WeIdUtils.convertWeIdToAddress(functionArg.getRecipient()));
        sendAssetArgs.setData(functionArg.getRemark());
        String from = WeIdUtils.convertWeIdToAddress(inputArg.getTransactionArg().getInvokerWeId());
        ResponseData<String> res = getBac005AssetService().sendAssetEncoder(sendAssetArgs, from);
        return new HttpResponseData<>(res.getResult(), res.getErrorCode(), res.getErrorMessage());
    }

    @Override
    public HttpResponseData<Object> sendDecoder(TransactionReceipt receipt) {
        ResponseData<Boolean> res = getBac005AssetService().sendAssetDecoder(receipt);
        return new HttpResponseData<>(res.getResult(), res.getErrorCode(), res.getErrorMessage());
    }

    @Override
    public HttpResponseData<String> batchSendEncoder(ReqInput<BAC005BatchInfo> inputArg) {
        BAC005BatchInfo functionArg = inputArg.getFunctionArg();
        List<SendAssetArgs> sendAssetArgList = new ArrayList<>();
        List<BAC005Info> objectList = functionArg.getList();
        HttpResponseData<Object> checkWeIdExistRsp = null;
        for (BAC005Info bac005Info : objectList) {
            SendAssetArgs sendAssetArgs = new SendAssetArgs();
            sendAssetArgs.setAmount(BigInteger.valueOf(bac005Info.getAssetId()));
            checkWeIdExistRsp = super.checkWeIdExist(this.weIdService, bac005Info.getRecipient());
            if (Objects.nonNull(checkWeIdExistRsp)) break;
            sendAssetArgs.setRecipient(WeIdUtils.convertWeIdToAddress(bac005Info.getRecipient()));
            sendAssetArgs.setData(bac005Info.getRemark());
            sendAssetArgList.add(sendAssetArgs);
        }

        if (Objects.nonNull(checkWeIdExistRsp)) {
            return new HttpResponseData<>(
                StringUtils.EMPTY, 
                checkWeIdExistRsp.getErrorCode(), 
                checkWeIdExistRsp.getErrorMessage()
            );
        }
        String from = WeIdUtils.convertWeIdToAddress(inputArg.getTransactionArg().getInvokerWeId());
        ResponseData<String> res = getBac005AssetService().batchSendAssetEncoder(
            sendAssetArgList, 
            functionArg.getRemark(), 
            from
        );
        return new HttpResponseData<>(res.getResult(), res.getErrorCode(), res.getErrorMessage());
    }

    @Override
    public HttpResponseData<Object> batchSendDecoder(TransactionReceipt receipt) {
        ResponseData<Boolean> res = getBac005AssetService().batchSendAssetDecoder(receipt);
        return new HttpResponseData<>(res.getResult(), res.getErrorCode(), res.getErrorMessage());
    }
}
