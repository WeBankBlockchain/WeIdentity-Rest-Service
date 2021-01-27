package com.webank.weid.http.constant;

public enum SignType {

    RSV(1), VSR(2);

    private int code;

    SignType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static SignType getSignTypeByCode(int code) {
        for (SignType signType : SignType.values()) {
            if (code == signType.getCode()) {
                return signType;
            }
        }
        return null;
    }
}
