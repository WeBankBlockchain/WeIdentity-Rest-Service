/*
 *       Copyright© (2019) WeBank Co., Ltd.
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

/**
 * The result of EncodedTransaction. A wrapper class with both encoded String, and rawTransaction.
 *
 * @author chaoxinhu
 */

@Data
public class EncodedTransactionWrapper {

    /**
     * The Encoded Transaction in String. This MUST be in Base64 format.
     */
    private String encodedTransaction;

    /**
     * The data segment instance in RawTransaction. Client needs this for future sendTransaction.
     */
    private String data;
}
