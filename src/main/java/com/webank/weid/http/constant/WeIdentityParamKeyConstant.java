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
 * Define param key names to be allowed to enable calls to WeIdentity Java SDK
 *
 * @author chaoxinhu
 **/
public final class WeIdentityParamKeyConstant {

    /**
     * Universal param key names. Treated as case-INSENSITIVE when being checked.
     */
    public static final String FUNCTION_NAME = "functionName";
    public static final String TRANSACTION_DATA = "data";
    public static final String SIGNED_MESSAGE = "signedMessage";
    public static final String API_VERSION = "v";
    public static final String NONCE = "nonce";
    public static final String FUNCTION_ARG = "functionArg";
    public static final String TRANSACTION_ARG = "transactionArg";
    public static final String CLAIM_HASH = "claimHash";
    public static final String KEY_INDEX = "invokerWeId";
    public static final String BODY = "body";

    public static final String DEFAULT_API_VERSION = "1.0.0";
    public static final String DEFAULT_PRIVATE_KEY_FILE_NAME = "ecdsa_key";
}
