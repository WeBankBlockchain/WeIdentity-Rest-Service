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

import java.util.HashMap;
import java.util.Map;

/**
 * Define function names to be picked for calls to WeIdentity Java SDK
 *
 * @author chaoxinhu
 **/

public final class WeIdentityFunctionNames {

    /**
     * Function names to be compared against the passed in encode/send transaction parameters. Case
     * insensitive.
     */
    public static final String FUNCNAME_CREATE_WEID = "createWeId";
    public static final String FUNCNAME_REGISTER_AUTHORITY_ISSUER = "registerAuthorityIssuer";
    public static final String FUNCNAME_REGISTER_CPT = "registerCpt";
    public static final String FUNCNAME_CREATE_CREDENTIAL = "createCredential";
    public static final String FUNCNAME_VERIFY_CREDENTIAL = "verifyCredential";
    public static final String FUNCNAME_GET_WEID_DOCUMENT = "getWeIdDocument";
    public static final String FUNCNAME_GET_WEID_DOCUMENT_JSON = "getWeIdDocumentJson";
    public static final String FUNCNAME_QUERY_AUTHORITY_ISSUER = "queryAuthorityIssuer";
    public static final String FUNCNAME_QUERY_CPT = "queryCpt";
    public static final String FUNCNAME_CREATE_CREDENTIALPOJO = "createCredentialPojo";
    public static final String FUNCNAME_VERIFY_CREDENTIALPOJO = "verifyCredentialPojo";

    public static final String FUNCNAME_CREATE_WEID_WITH_PUBKEY = "createWeIdWithPubKey";
    public static final String FUNCNAME_GET_WEID_DOCUMENT_BY_ORG = "getWeIdDocumentByOrgId";
    public static final String FUNCNAME_VERIFY_LITE_CREDENTIAL = "verifyLiteCredential";
    public static final String FUNCNAME_CREATE_EVIDENCE_FOR_LITE_CREDENTIAL = "createEvidence";
    public static final String FUNCNAME_ECCENCRYPT_CREDENTIAL = "createCredentialAndEncrypt";
    public static final String FUNCNAME_ECCDECRYPT = "eccDecrypt";
    public static final String FUNCNAME_ECCENCRYPT = "eccEncrypt";
    public static final String FUNCNAME_GET_EVIDENCE_BY_HASH = "getEvidence";
    public static final String FUNCNAME_ADD_ISSUER_TO_TYPE = "addWeIdToIssuerType";
    public static final String FUNCNAME_CHECK_ISSUER_BY_TYPE = "checkWeIdByIssuerType";

    /**
     * Function names to be assembled in SDK Function call. Case sensitive. FISCO-BCOS v1.
     */
    public static final String FUNCCALL_SET_ATTRIBUTE = "setAttribute";
    public static final String FUNCCALL_ADD_AUTHORITY_ISSUER = "addAuthorityIssuer";
    public static final String FUNCCALL_REGISTER_CPT = "registerCpt";

    /**
     * The FISCO-BCOS v2 function name and call map.
     */
    public static final Map<String, String> FUNCNAME_CALL_MAP_V2 = new HashMap<String, String>() {{
        put(FUNCNAME_CREATE_WEID, "createWeId");
        put(FUNCNAME_REGISTER_AUTHORITY_ISSUER, "addAuthorityIssuer");
        put(FUNCNAME_REGISTER_CPT, "registerCpt");
    }};
}
