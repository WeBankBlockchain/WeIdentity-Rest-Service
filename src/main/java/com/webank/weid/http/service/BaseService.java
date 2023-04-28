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

import com.webank.weid.blockchain.rpc.RawTransactionService;
import com.webank.weid.blockchain.service.impl.RawTransactionServiceImpl;
import com.webank.weid.constant.ErrorCode;
import com.webank.weid.http.protocol.request.TransactionArg;
import com.webank.weid.http.protocol.response.HttpResponseData;
import com.webank.weid.http.util.PropertiesUtil;
import com.webank.weid.service.rpc.WeIdService;
import com.webank.weid.util.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;


import com.webank.payment.protocol.base.Authentication;
import com.webank.payment.protocol.base.PrivateKey;
import com.webank.weid.exception.WeIdBaseException;
import com.webank.weid.http.constant.HttpReturnCode;
import com.webank.weid.http.util.KeyUtil;
import com.webank.weid.util.DataToolUtils;
import com.webank.weid.util.WeIdUtils;

public abstract class BaseService {

    private static final Logger logger = LoggerFactory.getLogger(BaseService.class);

    /**
     * spring context.
     */
    protected static final ApplicationContext context;

    public static RawTransactionService rawTransactionService;
    static {
        if (PropertyUtils.getProperty("deploy.style").equals("blockchain")) {
            rawTransactionService = new RawTransactionServiceImpl();
        }
    }

    static {
        // initializing spring containers
        context = new ClassPathXmlApplicationContext(new String[]{
            "classpath:SpringApplicationContext.xml"});
        logger.info("initializing spring containers finish...");

    }
    
    private PrivateKey buildPrivateKey(String value) {
        PrivateKey pri = new PrivateKey();
        pri.setValue(value);
        return pri;
    }
    
    protected Authentication getAuthentication(String weId) {
        Authentication authentication = getAuthenticationByWeId(weId);
        String passphrase = PropertiesUtil.getProperty("default.passphrase");
        if (StringUtils.isNotBlank(weId) && weId.equalsIgnoreCase(passphrase)) {
            //将私钥转换成公钥，将公钥转换成weId地址
            weId = WeIdUtils.getWeIdFromPrivateKey(authentication.getPrivateKey().getValue());
        }
        authentication.setUserAddress(WeIdUtils.convertWeIdToAddress(weId));
        return authentication;
    }
    
    private Authentication getAuthenticationByWeId(String weId) {
        String weIdPrivKey = KeyUtil
            .getPrivateKeyByWeId(KeyUtil.SDK_PRIVKEY_PATH, weId);
        if (StringUtils.isEmpty(weIdPrivKey)) {
            throw new WeIdBaseException(HttpReturnCode.INVOKER_ILLEGAL.getCodeDesc());
        }
        Authentication authentication = new Authentication();
        authentication.setPrivateKey(buildPrivateKey(weIdPrivKey));
        return authentication;
    }

    protected HttpResponseData<Object> checkWeIdExist(WeIdService weIdService, String weId) {
        com.webank.weid.blockchain.protocol.response.ResponseData<Boolean> weIdExist = weIdService.isWeIdExist(weId);
        if (!weIdExist.getResult()) {
            return new HttpResponseData<>(
                    weIdExist.getResult(),
                    ErrorCode.WEID_DOES_NOT_EXIST.getCode(),
                    "the weId:[" + weId + "] does not exist on blockchain."
            );
        }
        return null;
    }

    protected static Object getLoopBack(String transactionArgStr) {
        TransactionArg transactionArg = DataToolUtils.deserialize(transactionArgStr, TransactionArg.class);
        return transactionArg.getLoopback();
    }
}
