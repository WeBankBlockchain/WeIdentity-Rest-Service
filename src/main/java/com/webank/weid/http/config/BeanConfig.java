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

package com.webank.weid.http.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.webank.weid.service.rpc.AuthorityIssuerService;
import com.webank.weid.service.rpc.CptService;
import com.webank.weid.service.rpc.CredentialService;
import com.webank.weid.service.rpc.WeIdService;
import com.webank.weid.service.impl.AuthorityIssuerServiceImpl;
import com.webank.weid.service.impl.CptServiceImpl;
import com.webank.weid.service.impl.CredentialServiceImpl;
import com.webank.weid.service.impl.WeIdServiceImpl;

/**
 * implement http redirect https.
 *
 * @author darwindu
 */
@Configuration
public class BeanConfig {

    @Bean("authorityIssuerService")
    public AuthorityIssuerService getAuthorityIssuerService() {
        return new AuthorityIssuerServiceImpl();
    }

    @Bean("cptService")
    public CptService getCptService() {
        return new CptServiceImpl();
    }

    @Bean("credentialService")
    public CredentialService getCredentialService() {
        return new CredentialServiceImpl();
    }

    @Bean("weIdService")
    public WeIdService getWeIdService() {
        return new WeIdServiceImpl();
    }

}
