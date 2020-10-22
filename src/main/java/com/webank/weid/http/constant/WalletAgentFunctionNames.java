/*
 *       Copyright© (2019-2020) WeBank Co., Ltd.
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

public final class WalletAgentFunctionNames {

    /**
     * Function names to be compared against the passed in encode/send transaction parameters. Case
     * insensitive.
     */
    public static final String FUNCNAME_WALLETAGENT_CONSTRUCT = "construct";
    public static final String FUNCNAME_WALLETAGENT_ISSUE = "issue";
    public static final String FUNCNAME_WALLETAGENT_CONSTRUCTANDISSUE = "constructAndIssue";
    public static final String FUNCNAME_WALLETAGENT_GETBALANCE = "getBalance";
    public static final String FUNCNAME_WALLETAGENT_GETBATCHBALANCE = "getBatchBalance";
    public static final String FUNCNAME_WALLETAGENT_GETBALANCEBYWEID = "getBalanceByWeId";
    public static final String FUNCNAME_WALLETAGENT_SEND = "send";
    public static final String FUNCNAME_WALLETAGENT_BATCHSEND = "batchSend";
    public static final String FUNCNAME_WALLETAGENT_GETBASEINFO = "getBaseInfo";
    public static final String FUNCNAME_WALLETAGENT_GETBASEINFOBYWEID = "getBaseInfoByWeId";

}
