/*
 *       Copyright© (2018) WeBank Co., Ltd.
 *
 *       This file is part of weidentity-java-sdk.
 *
 *       weidentity-java-sdk is free software: you can redistribute it and/or modify
 *       it under the terms of the GNU Lesser General Public License as published by
 *       the Free Software Foundation, either version 3 of the License, or
 *       (at your option) any later version.
 *
 *       weidentity-java-sdk is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU Lesser General Public License for more details.
 *
 *       You should have received a copy of the GNU Lesser General Public License
 *       along with weidentity-java-sdk.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.webank.weid.http.service;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Random;

import org.springframework.stereotype.Service;

import com.webank.weid.protocol.response.ResponseData;
import com.webank.weid.service.BaseService;

/**
 * @author darwindu
 **/
@Service
public class InvokerWeb3jService {


    /**
     * get nonce
     * @return
     */
    public ResponseData<BigInteger> getNonce() {

        ResponseData<BigInteger> responseData = new ResponseData<BigInteger>();
        Random r = new SecureRandom();
        BigInteger nonce = new BigInteger(250, r);
        responseData.setResult(nonce);
        return responseData;
    }

    /**
     * get block limit
     * @return
     */
    public ResponseData<BigInteger> getBlockLimit() {
        return BaseService.getBlockLimit();
    }
}
