package com.webank.weid.http.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.webank.weid.rpc.AuthorityIssuerService;
import com.webank.weid.rpc.CptService;
import com.webank.weid.rpc.CredentialService;
import com.webank.weid.rpc.WeIdService;
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
