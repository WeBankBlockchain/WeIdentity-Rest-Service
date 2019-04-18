/*
 *       Copyright© (2019) WeBank Co., Ltd.
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

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.stereotype.Component;

import com.webank.weid.http.service.impl.InvokerWeIdServiceImpl;
import com.webank.weid.http.util.PrivateKeyUtil;
import com.webank.weid.protocol.response.CreateWeIdDataResult;

@Component
public class PrivateKeyTest {

    InvokerWeIdService invokerWeIdService = new InvokerWeIdServiceImpl();

    @Test
    public void testKeysAll() {
        CreateWeIdDataResult createWeIdDataResult = invokerWeIdService.createWeId().getResult();
        System.out.println(createWeIdDataResult);
        String weId = createWeIdDataResult.getWeId();
        String privKey = createWeIdDataResult.getUserWeIdPrivateKey().getPrivateKey();

        // test save
        PrivateKeyUtil.savePrivateKey(PrivateKeyUtil.SDK_PRIVKEY_PATH, weId, privKey);

        // test load
        String extractedKey = PrivateKeyUtil
            .getPrivateKeyByWeId(PrivateKeyUtil.SDK_PRIVKEY_PATH, weId);
        System.out.println(extractedKey);
        Assert.assertTrue(StringUtils.equals(extractedKey, privKey));

        // test load sdk
        String extractedKey2 = PrivateKeyUtil
            .getPrivateKeyByWeId(PrivateKeyUtil.SDK_PRIVKEY_PATH, "0xffffffff");
        System.out.println(extractedKey2);
    }
}
