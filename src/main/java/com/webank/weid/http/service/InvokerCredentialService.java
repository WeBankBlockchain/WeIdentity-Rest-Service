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

import org.springframework.stereotype.Service;

import com.webank.weid.http.protocol.request.InputArg;
import com.webank.weid.http.protocol.response.HttpResponseData;

@Service
public interface InvokerCredentialService {

    /**
     * Generate a credential for client to sign. The signature field is null, and both full claim
     * and claimHash will be returned. The returned json String is an key-ordered compact json.
     *
     * @param createCredentialFuncArgs the functionArgs
     * @return the Map contains Credential content and claimHash.
     */
    HttpResponseData<Object> createCredentialInvoke(InputArg createCredentialFuncArgs);

    /**
     * Verify the validity of a credential. Need format conversion (UTC date and @context)
     *
     * @param verifyCredentialFuncArgs the credential json args
     * @return the Boolean response data
     */
    HttpResponseData<Object> verifyCredentialInvoke(InputArg verifyCredentialFuncArgs);
}
