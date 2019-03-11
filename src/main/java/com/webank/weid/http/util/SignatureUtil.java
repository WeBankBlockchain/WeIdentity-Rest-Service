/*
 *       Copyright© (2018) WeBank Co., Ltd.
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

import org.bcos.web3j.abi.datatypes.generated.Bytes32;
import org.bcos.web3j.abi.datatypes.generated.Uint8;
import org.bcos.web3j.crypto.Sign.SignatureData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.webank.weid.protocol.response.RsvSignature;
import com.webank.weid.util.DataTypetUtils;
import com.webank.weid.util.SignatureUtils;

/**
 * Handles all signature related tasks.
 *
 * @author chaoxinhu
 */
public class SignatureUtil {

    private static Logger logger = LoggerFactory.getLogger(SignatureUtil.class);

    /**
     * Convert SignatureData to blockchain-ready RSV format
     *
     * @param signatureData the signature data
     * @return rsvSignature the rsv signature structure
     */
    public static RsvSignature convertSignatureDataToRsv(
        SignatureData signatureData) {

        Uint8 v = DataTypetUtils.intToUnt8(Integer.valueOf(signatureData.getV()));
        Bytes32 r = DataTypetUtils.bytesArrayToBytes32(signatureData.getR());
        Bytes32 s = DataTypetUtils.bytesArrayToBytes32(signatureData.getS());

        RsvSignature rsvSignature = new RsvSignature();
        rsvSignature.setV(v);
        rsvSignature.setR(r);
        rsvSignature.setS(s);
        return rsvSignature;
    }

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
