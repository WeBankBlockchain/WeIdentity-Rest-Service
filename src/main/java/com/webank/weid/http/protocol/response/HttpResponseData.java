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

package com.webank.weid.http.protocol.response;

import lombok.Data;

import com.webank.weid.http.constant.HttpReturnCode;

/**
 * The internal base response result class.
 *
 * @param <T> the generic type
 * @author tonychen and chaoxinhu
 */
@Data
public class HttpResponseData<T> {

    private T respBody;
    private Integer errorCode;
    private String errorMessage;

    /**
     * Instantiates a new response data.
     */
    public HttpResponseData() {
        this.setErrorCode(HttpReturnCode.SUCCESS);
    }

    /**
     * Instantiates a new response data.
     *
     * @param result the result
     * @param errorCode the return code
     */
    public HttpResponseData(T result, HttpReturnCode errorCode) {
        this.respBody = result;
        this.errorCode = errorCode.getCode();
        this.errorMessage = errorCode.getCodeDesc();
    }

    /**
     * Instantiates a new response data by inputing all params.
     *
     * @param result the result
     * @param errorCode the error code
     * @param errorMessage the error msg
     */
    public HttpResponseData(T result, Integer errorCode, String errorMessage) {
        this.respBody = result;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    /**
     * set a ErrorCode type errorCode.
     */
    public void setErrorCode(HttpReturnCode errorCode) {
        this.errorCode = errorCode.getCode();
        this.errorMessage = errorCode.getCodeDesc();
    }

}