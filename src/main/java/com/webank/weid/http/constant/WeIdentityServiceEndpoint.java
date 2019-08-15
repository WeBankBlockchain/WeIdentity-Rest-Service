/*
 *       Copyright© (2019) WeBank Co., Ltd.
 *
 *       This file is part of weidentity-http-service.
 *
 *       weidentity-http-service is free software: you can redistribute it and/or modify
 *       it under the terms of the GNU Lesser General Public License as published by
 *       the Free Software Foundation, either version 3 of the License, or
 *       (at your option) any later version.
 *
 *       weidentity-http-service is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU Lesser General Public License for more details.
 *
 *       You should have received a copy of the GNU Lesser General Public License
 *       along with weidentity-http-service.  If not, see <https://www.gnu.org/licenses/>.
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
    public static final String ENCODE_TRANSACTION = "encode";
    public static final String SEND_TRANSACTION = "transact";
    public static final String INVOKE_FUNCTION = "invoke";

    /**
     * EP Service endpoint.
     */
    public static final String EPS_ROOT = "endpoint";
    public static final String ADD_FUNCTION = "add";
    public static final String REMOVE_FUNCTION = "remove";
    public static final String FETCH_FUNCTION = "auto-fetch";

    /**
     * Separator.
     */
    public static final String EPS_SEPARATOR = "|||";

    /**
     * Misc items
     */
    public static final String ALL_INFO = "ALL";
}
