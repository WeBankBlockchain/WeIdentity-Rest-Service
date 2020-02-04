package com.webank.weid.http.service;

import com.webank.weid.http.protocol.response.HttpResponseData;
import com.webank.weid.protocol.base.CredentialPojo;
import org.springframework.stereotype.Service;

@Service
public interface AuthService {

    /**
     * Process data fetching from a service URL based on the CPT101 authorization
     * credential and client-side signing of a nonce from http-server.
     *
     * @return remote data in String format
     */
    HttpResponseData<String> fetchData(CredentialPojo authToken, String signedNonce);

    /**
     * Request a data fetch nonce from http server. TODO to-be-implemented.
     *
     * @return token in String format
     */
    HttpResponseData<String> requestNonce(CredentialPojo authToken);
}
