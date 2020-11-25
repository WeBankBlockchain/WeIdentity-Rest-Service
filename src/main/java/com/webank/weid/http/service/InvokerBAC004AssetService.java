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

import org.fisco.bcos.web3j.protocol.core.methods.response.TransactionReceipt;
import org.springframework.stereotype.Service;

import com.webank.weid.http.protocol.request.ReqInput;
import com.webank.weid.http.protocol.request.payment.AssetAddressList;
import com.webank.weid.http.protocol.request.payment.BAC004BatchSendInfo;
import com.webank.weid.http.protocol.request.payment.BAC004Info;
import com.webank.weid.http.protocol.request.payment.BAC004SendInfo;
import com.webank.weid.http.protocol.request.payment.BaseQuery;
import com.webank.weid.http.protocol.request.payment.PageQuery;
import com.webank.weid.http.protocol.response.HttpResponseData;

@Service
public interface InvokerBAC004AssetService {

    HttpResponseData<Object> construct(ReqInput<BAC004Info> inputArg);
    HttpResponseData<Object> issue(ReqInput<BAC004Info> inputArg);
    HttpResponseData<Object> constructAndIssue(ReqInput<BAC004Info> inputArg);
    HttpResponseData<Object> getBalance(ReqInput<BaseQuery> inputArg);
    HttpResponseData<Object> getBatchBalance(ReqInput<AssetAddressList> inputArg);
    HttpResponseData<Object> getBalanceByWeId(ReqInput<PageQuery> inputArg);
    HttpResponseData<Object> send(ReqInput<BAC004SendInfo> inputArg);
    HttpResponseData<Object> batchSend(ReqInput<BAC004BatchSendInfo> inputArg);
    HttpResponseData<Object> getBaseInfo(ReqInput<AssetAddressList> inputArg);
    HttpResponseData<Object> getBaseInfoByWeId(ReqInput<PageQuery> inputArg);
    
    //
    HttpResponseData<String> issueEncoder(ReqInput<BAC004Info> inputArg);
    HttpResponseData<Object> issueDeCoder(TransactionReceipt receipt);
    HttpResponseData<String> constructEncoder(ReqInput<BAC004Info> inputArg);
    HttpResponseData<Object> constructDeCoder(TransactionReceipt receipt);
    HttpResponseData<String> sendEncoder(ReqInput<BAC004SendInfo> inputArg);
    HttpResponseData<Object> sendDecoder(TransactionReceipt receipt);
    HttpResponseData<String> batchSendEncoder(ReqInput<BAC004BatchSendInfo> inputArg);
    HttpResponseData<Object> batchSendDecoder(TransactionReceipt receipt);
}
