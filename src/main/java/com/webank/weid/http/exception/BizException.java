package com.webank.weid.http.exception;

public class BizException extends RuntimeException {

    private Integer errorCode;
    private String errorMessage;

    /**
     * constructor.
     * @param errorMessage exception error message.
     * @param errorCode exception error code.
     */
    public BizException(String errorMessage, Integer errorCode) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    /**
     * constructor.
     * @param errorMessage exception error message
     * @param cause Throwable
     */
    public BizException(String errorMessage,Throwable cause) {
        super(errorMessage, cause);
    }

    /**
     * constructor.
     * @param errorMessage exception error message.
     * @param errorCode exception error code.
     * @param cause Throwable
     */
    public BizException(String errorMessage, Integer errorCode, Throwable cause) {
        super(errorMessage, cause);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public Integer getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

}
