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

package com.webank.weid.http.service;

import com.webank.payment.protocol.base.BaseAsset;
import com.webank.weid.http.protocol.request.ReqInput;
import com.webank.weid.http.protocol.request.payment.*;
import com.webank.weid.http.protocol.response.HttpResponseData;

import org.fisco.bcos.web3j.protocol.core.methods.response.TransactionReceipt;
import org.springframework.stereotype.Service;

@Service
public interface InvokerBAC005AssetService {

    HttpResponseData<Object> construct(ReqInput<BAC005Info> inputArg);
    HttpResponseData<Object> issue(ReqInput<BAC005Info> inputArg);
    HttpResponseData<Object> constructAndIssue(ReqInput<BAC005Info> inputArg);
    HttpResponseData<Object> batchIssue(ReqInput<BAC005BatchInfo> inputArg);
    HttpResponseData<Object> constructAndBatchIssue(ReqInput<BAC005BatchInfo> inputArg);
    HttpResponseData<Object> queryAssetOwner(ReqInput<BAC005Info> inputArg);
    HttpResponseData<Object> queryAssetNum(ReqInput<BaseAsset> inputArg);
    HttpResponseData<Object> queryAssetList(ReqInput<PageQuery> inputArg);
    HttpResponseData<Object> queryOwnedAssetNum(ReqInput<BaseQuery> inputArg);
    HttpResponseData<Object> queryOwnedAssetList(ReqInput<PageQuery> inputArg);
    HttpResponseData<Object> send(ReqInput<BAC005Info> inputArg);
    HttpResponseData<Object> batchSend(ReqInput<BAC005BatchInfo> inputArg);
    HttpResponseData<Object> queryBaseInfo(ReqInput<AssetAddressList> inputArg);
    HttpResponseData<Object> queryBaseInfoByWeId(ReqInput<PageQuery> inputArg);

    //
    HttpResponseData<String> constructEncoder(ReqInput<BAC005Info> inputArg);
    HttpResponseData<Object> constructDeCoder(TransactionReceipt receipt);
    HttpResponseData<String> issueEncoder(ReqInput<BAC005Info> inputArg);
    HttpResponseData<Object> issueDeCoder(TransactionReceipt receipt);
    HttpResponseData<String> batchIssueEncoder(ReqInput<BAC005BatchInfo> inputArg);
    HttpResponseData<Object> batchIssueDeCoder(TransactionReceipt receipt);
    HttpResponseData<String> sendEncoder(ReqInput<BAC005Info> inputArg);
    HttpResponseData<Object> sendDecoder(TransactionReceipt receipt);
    HttpResponseData<String> batchSendEncoder(ReqInput<BAC005BatchInfo> inputArg);
    HttpResponseData<Object> batchSendDecoder(TransactionReceipt receipt);
}
