/*
 *       Copyright© (2018-2019) WeBank Co., Ltd.
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

package com.webank.weid.http.constant;

/**
 * Define function names to be picked for calls to WeIdentity Java SDK
 *
 * @author chaoxinhu
 **/

public final class WeIdentityFunctionNames {
    /**
     * Function names to be compared against the passed in encode/send transaction parameters.
     */
    public static final String FUNCNAME_CREATE_WEID = "createWeId";
    public static final String FUNCNAME_REGISTER_AUTHORITY_ISSUER = "registerAuthorityIssuer";
    public static final String FUNCNAME_REGISTER_CPT = "registerCpt";
    public static final String FUNCNAME_CREATE_CREDENTIAL = "createCredentialInvoke";
    public static final String FUNCNAME_VERIFY_CREDENTIAL = "verifyCredentialInvoke";
    public static final String FUNCNAME_GET_WEID_DOCUMENT = "getWeIdDocument";
    public static final String FUNCNAME_GET_WEID_DOCUMENT_JSON = "getWeIdDocumentJson";
    public static final String FUNCNAME_QUERY_AUTHORITY_ISSUER = "queryAuthorityIssuer";
    public static final String FUNCNAME_GET_CREDENTIAL_JSON = "getCredentialJsonInvoke";
    public static final String FUNCNAME_QUERY_CPT = "queryCpt";

    /**
     * Function names to be assembled in SDK Function call.
     */
    public static final String FUNCCALL_SET_ATTRIBUTE = "setAttribute";
    public static final String FUNCCALL_ADD_AUTHORITY_ISSUER = "addAuthorityIssuer";
    public static final String FUNCCALL_REGISTER_CPT = "registerCpt";
}
