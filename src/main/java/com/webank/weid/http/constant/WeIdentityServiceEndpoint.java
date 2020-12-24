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

package com.webank.weid.http.constant;

/**
 * Define function names to be picked for calls to WeIdentity Java SDK
 *
 * @author chaoxinhu
 **/

public final class WeIdentityServiceEndpoint {

    /**
     * API endpoint.
     */
    public static final String API_ROOT = "weid/api";
    public static final String PAYMENT_ROOT = "payment";
    public static final String ENCODE_TRANSACTION = "encode";
    public static final String SEND_TRANSACTION = "transact";
    public static final String INVOKE_FUNCTION = "invoke";
    public static final String WALLET_AGENT_BAC004_FUNCTION = "bac004/api/invoke";
    public static final String WALLET_AGENT_BAC005_FUNCTION = "bac005/api/invoke";
    public static final String WALLET_AGENT_BAC004_FUNCTION_ENCODE = "bac004/api/encode";
    public static final String WALLET_AGENT_BAC004_FUNCTION_TRANSACT = "bac004/api/transact";
    public static final String WALLET_AGENT_BAC005_FUNCTION_ENCODE = "bac005/api/encode";
    public static final String WALLET_AGENT_BAC005_FUNCTION_TRANSACT = "bac005/api/transact";
    
    /**
     * EP Service endpoint.
     */
    public static final String EPS_ROOT = "endpoint";
    public static final String ADD_FUNCTION = "add";
    public static final String REMOVE_FUNCTION = "remove";
    public static final String FETCH_FUNCTION = "auto-fetch";

    /**
     * Data-Authorization related endpoints.
     */
    public static final String AUTHO_ROOT = "authorize";
    public static final String AUTHO_FETCH_DATA = "fetch-data";
    public static final String AUTHO_REQUEST_NONCE = "request-nonce";

    /**
     * Separator.
     */
    public static final String EPS_SEPARATOR = "|||";

    /**
     * Misc items
     */
    public static final String ALL_INFO = "ALL";
}
