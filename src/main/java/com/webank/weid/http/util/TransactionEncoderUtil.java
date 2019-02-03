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

import java.util.ArrayList;
import java.util.List;

import org.bcos.web3j.crypto.EncryptType;
import org.bcos.web3j.crypto.Sign.SignatureData;
import org.bcos.web3j.crypto.TransactionEncoder;
import org.bcos.web3j.protocol.core.methods.request.RawTransaction;
import org.bcos.web3j.rlp.RlpEncoder;
import org.bcos.web3j.rlp.RlpList;
import org.bcos.web3j.rlp.RlpString;
import org.bcos.web3j.rlp.RlpType;
import org.bcos.web3j.utils.Bytes;
import org.bcos.web3j.utils.Numeric;

/**
 * @author darwindu
 **/
public class TransactionEncoderUtil extends TransactionEncoder {

    public static byte[] encodeEx(RawTransaction rawTransaction, SignatureData signatureData) {
        List<RlpType> values = asRlpValues(rawTransaction, signatureData);
        RlpList rlpList = new RlpList(values);
        return RlpEncoder.encode(rlpList);
    }

    static List<RlpType> asRlpValues(RawTransaction rawTransaction, SignatureData signatureData) {
        List<RlpType> result = new ArrayList();
        result.add(RlpString.create(rawTransaction.getRandomid()));
        result.add(RlpString.create(rawTransaction.getGasPrice()));
        result.add(RlpString.create(rawTransaction.getGasLimit()));
        result.add(RlpString.create(rawTransaction.getBlockLimit()));
        String to = rawTransaction.getTo();
        if (to != null && to.length() > 0) {
            result.add(RlpString.create(Numeric.hexStringToByteArray(to)));
        } else {
            result.add(RlpString.create(""));
        }

        result.add(RlpString.create(rawTransaction.getValue()));
        byte[] data = Numeric.hexStringToByteArray(rawTransaction.getData());
        result.add(RlpString.create(data));
        String contractName = rawTransaction.getContractName();
        if (contractName != null && contractName.length() > 0) {
            result.add(RlpString.create(rawTransaction.getContractName()));
            result.add(RlpString.create(rawTransaction.getVersion()));
            result.add(RlpString.create(rawTransaction.getType()));
        }

        if (signatureData != null) {
            if (EncryptType.encryptType == 1) {
                result.add(RlpString.create(Bytes.trimLeadingZeroes(signatureData.getPub())));
                result.add(RlpString.create(Bytes.trimLeadingZeroes(signatureData.getR())));
                result.add(RlpString.create(Bytes.trimLeadingZeroes(signatureData.getS())));
            } else {
                result.add(RlpString.create(signatureData.getV()));
                result.add(RlpString.create(Bytes.trimLeadingZeroes(signatureData.getR())));
                result.add(RlpString.create(Bytes.trimLeadingZeroes(signatureData.getS())));
            }
        }
        return result;
    }
}
