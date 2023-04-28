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

public enum HttpReturnCode {

    /**
     * The success.
     */
    SUCCESS(0, "success"),

    /**
     * base error.
     */
    VALUE_FORMAT_ILLEGAL(201000, "Input parameter value format illegal. "),

    /**
     * other uncatched exceptions or error.
     */
    UNKNOWN_ERROR(201001, "Unknown error. "),

    /**
     * input null.
     */
    INPUT_NULL(201002, "Input parameter contains null value or missing keys. "),

    /**
     * input illegal.
     */
    INPUT_ILLEGAL(201003,
        "One or more input arguments format illegal. Please refer to the API documents. "),

    /**
     * function name unknown.
     */
    FUNCTION_NAME_ILLEGAL(201004, "Function name format illegal or missing. "),

    /**
     * transaction hex error.
     */
    TXN_HEX_ERROR(201005, "Transaction hex creation failed. "),

    /**
     * transaction encoder error.
     */
    TXN_ENCODER_ERROR(201006, "Transaction encoder creation failed. "),

    /**
     * SDK call error.
     */
    WEID_SDK_ERROR(201007, "WeIdentity SDK call failed due to unknown error. "),

    /**
     * invoker weId is illegal.
     */
    INVOKER_ILLEGAL(201008, "Cannot find the private key by the invoker index. "),

    /**
     * Nonce value illegal.
     */
    NONCE_ILLEGAL(201009, "Nonce value illegal. "),

    /**
     * function arg illegal.
     */
    FUNCARG_ILLEGAL(201010, "Function arguments format illegal or missing. "),

    /**
     * txn arg illegal.
     */
    TXNARG_ILLEGAL(201011, "Transaction arguments format illegal or missing. "),

    /**
     * version illegal.
     */
    VER_ILLEGAL(201012, "Version format illegal or missing. "),

    /**
     * signed message illegal.
     */
    SIGNED_MSG_ILLEGAL(201013, "Signed message format illegal or missing. "),

    /**
     * data illegal.
     */
    DATA_ILLEGAL(201014, "data segment format illegal or missing. "),

    /**
     * web3j error.
     */
    WEB3J_ERROR(201015, "Failed to load web3j (maybe from illegal certs, Group ID, networking, or secp256k1 settings)"),

    /**
     * contract error.
     */
    CONTRACT_ERROR(201016, "Failed to load contract (maybe from un-deployed site, DB not storing CNS, or CNS hash illegal)"),
    
    /**
     * BlockLimit value illegal.
     */
    BLOCK_LIMIT_ILLEGAL(201017, "BlockLimit value illegal. "),

    /**
     * Convert a public key to a WeIdentity DID error
     */
    CONVERT_PUBKEY_TO_WEID_ERROR(201018, "Convert public key to weId error. "),
    
    /**
     * SignType value illegal.
     */
    SIGN_TYPE_ILLEGAL(201019, "SignType value illegal. "),

    /**
     * WeIdentity deploy style(database) not support send rawTransaction.
     */
    WEIDENTITY_DEPLOY_NOT_SUPPORT(201019, "WeIdentity deploy style(database) not support send rawTransaction. "),

    /**
     * getWeIdListByPubKeyList interface error
     */
    GET_WEID_LIST_BY_PUBKEY_LIST_ERROR(500100, "Get weId list by pubKeyList error. "),
    
    /**
     * RPC related error codes.
     */
    RPC_SEND_FAIL(205001, "Sending RPC request failure"),
    RPC_NETWORK_ERROR(205002, "RPC networking failure"),
    RPC_GET_FAIL(205003, "Get RPC reply from request UUID failure"),
    RPC_FETCH_FAIL(205004, "Fetching RPC remote service info failure"),
    RPC_ENDPOINT_NOT_EXIST(205005, "RPC Endpoint does not exist");

    /**
     * error code.
     */
    private int code;

    /**
     * error message.
     */
    private String codeDesc;

    /**
     * Error Code Constructor.
     *
     * @param code The ErrorCode
     * @param codeDesc The ErrorCode Description
     */
    HttpReturnCode(int code, String codeDesc) {
        this.code = code;
        this.codeDesc = codeDesc;
    }

    /**
     * Get the Error Code.
     *
     * @return the ErrorCode
     */
    public int getCode() {
        return code;
    }

    /**
     * Set the Error Code.
     *
     * @param code the new ErrorCode
     */
    protected void setCode(int code) {
        this.code = code;
    }

    /**
     * Gets the ErrorCode Description.
     *
     * @return the ErrorCode Description
     */
    public String getCodeDesc() {
        return codeDesc;
    }

    /**
     * Sets the ErrorCode Description.
     *
     * @param codeDesc the new ErrorCode Description
     */
    protected void setCodeDesc(String codeDesc) {
        this.codeDesc = codeDesc;
    }
}
