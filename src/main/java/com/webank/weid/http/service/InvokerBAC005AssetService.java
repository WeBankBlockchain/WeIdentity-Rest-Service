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

import com.webank.weid.http.protocol.request.ReqInput;
import com.webank.weid.http.protocol.response.HttpResponseData;
import org.springframework.stereotype.Service;

@Service
public interface InvokerBAC005AssetService {

    HttpResponseData<Object> construct(ReqInput inputArg);
    HttpResponseData<Object> issue(ReqInput inputArg);
    HttpResponseData<Object> constructAndIssue(ReqInput inputArg);
    HttpResponseData<Object> batchIssue(ReqInput inputArg);
    HttpResponseData<Object> constructAndBatchIssue(ReqInput inputArg);
    HttpResponseData<Object> queryAssetOwner(ReqInput inputArg);
    HttpResponseData<Object> queryAssetNum(ReqInput inputArg);
    HttpResponseData<Object> queryAssetList(ReqInput inputArg);
    HttpResponseData<Object> queryOwnedAssetNum(ReqInput inputArg);
    HttpResponseData<Object> queryOwnedAssetList(ReqInput inputArg);
    HttpResponseData<Object> send(ReqInput inputArg);
    HttpResponseData<Object> batchSend(ReqInput inputArg);
    HttpResponseData<Object> queryBaseInfo(ReqInput inputArg);
    HttpResponseData<Object> queryBaseInfoByWeId(ReqInput inputArg);
}
