package com.webank.weid.http.service.rpc.proxy;

public enum AmopProxyCode {

    SUCCESS(0, "server succeeded"),

    FAILED(1, "server failed due to generic error"),

    TIMEOUT(2, "server timeout"),

    INVALID_STATUS(3, "current status invalid"),

    CLIENT_HANDSHAKE(500, "client handshake"),

    CLIENT_AUTH(1000, "client creates auth to server"),

    SERVER_TOKEN(1001, "server provides client a token for auth"),

    CLIENT_SIGNED_TOKEN(1002, "client provides signed token for auth"),

    CLIENT_CLOSE(1003, "client asks to de-register all auth and topic"),

    CLIENT_REGISTER_TOPIC(2000, "client registers amop topic request"),

    CLIENT_AMOP_SEND(3000, "client sends amop message to server (wrt topic)"),

    SERVER_SEND(4000, "server sends raw message to client"),

    SERVER_AMOP_SEND(4001, "server sends AMOP message to client (recv from topic)");

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
    AmopProxyCode(int code, String codeDesc) {
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

    /**
     * get ErrorType By errcode.
     *
     * @param errorCode the ErrorCode
     * @return errorCode
     */
    public static AmopProxyCode getByCode(int errorCode) {
        for (AmopProxyCode type : AmopProxyCode.values()) {
            if (type.getCode() == errorCode) {
                return type;
            }
        }
        return AmopProxyCode.FAILED;
    }

}
