package com.webank.weid.http.constant;

public enum HttpReturnCode {

    /**
     * The success.
     */
    SUCCESS(0, "success"),

    /**
     * BASE error.
     */
    BASE_ERROR(1000, "base error occurred."),

    /**
     * other uncatched exceptions or error.
     */
    UNKNOWN_ERROR(1001, "unknown error, please check the error log."),

    /**
     * input null.
     */
    INPUT_NULL(1002, "input is null."),

    /**
     * input illegal.
     */
    INPUT_ILLEGAL(1003, "input parameter format illegal."),

    /**
     * function name unknown.
     */
    UNKNOWN_FUNCTION_NAME(1004, "function name unknown."),

    /**
     * transaction hex error
     */
    TXN_HEX_ERROR(1005, "transaction hex error."),

    /**
     * transaction hex error
     */
    WEID_SDK_ERROR(1006, "WeIdentity SDK function call error.")

    ;

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
