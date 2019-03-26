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

package com.webank.weid.http.util;

import java.nio.charset.StandardCharsets;

import org.bcos.web3j.crypto.Sign.SignatureData;

import com.webank.weid.util.SignatureUtils;

/**
 * Handles all signature related tasks.
 *
 * @author chaoxinhu
 */
public class SignatureUtil {

    /**
     * Convert an off-chain Base64 signature String to signatureData format
     *
     * @param base64Signature the signature string in Base64
     * @return signatureData structure
     */
    public static SignatureData convertBase64StringToSignatureData(String base64Signature) {
        return SignatureUtils.simpleSignatureDeserialization(
            SignatureUtils.base64Decode(
                base64Signature.getBytes(StandardCharsets.UTF_8))
        );
    }
}
