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

package com.webank.weid.http.util;

import com.webank.weid.protocol.base.WeIdAuthentication;
import com.webank.weid.protocol.base.WeIdPrivateKey;
import com.webank.weid.util.DataToolUtils;
import com.webank.weid.util.WeIdUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.webank.weid.http.constant.WeIdentityParamKeyConstant;

/**
 * the util of the private key.
 *
 * @author afeexian
 */
public class KeyUtil {

    private static final Logger logger = LoggerFactory.getLogger(KeyUtil.class);

    /**
     * SDK private key storage path.
     */
    public static final String SDK_PRIVKEY_PATH =
        PropertiesUtil.getProperty("weid.keys.dir");

    /**
     * SDK private key storage path.
     */
    public static final String ADMIN_PRIVKEY_PATH =
            PropertiesUtil.getProperty("admin.privKeyPath");

    /**
     * slash.
     */
    private static final String SLASH_CHARACTER = "/";

    /**
     * this method stores weId private key information by file and stores private key information by itself in actual scene.
     *
     * @param path save path
     * @param weId the weId
     * @param privateKey the private key
     * @return returns saved results
     */
    public static boolean savePrivateKey(String path, String weId, String privateKey) {

        try {
            if (null == weId) {
                logger.error("weId is null");
                return false;
            }

            // get the third paragraph of weId.
            String fileName = weId.substring(weId.lastIndexOf(":") + 1);

            // check whether the path exists or not, then create the path and return.
            String checkPath = checkDir(path);
            String filePath = checkPath + fileName;

            logger.info("save private key into file, weId={}, filePath={}", weId, filePath);

            // save the private key information as the file name for the third paragraph of weId.
            saveFile(filePath, privateKey);
            return true;
        } catch (Exception e) {
            logger.error("savePrivateKey error", e);
        }
        return false;
    }

    /**
     * get the private key by weId.
     *
     * @param path the path
     * @param weId the weId
     * @return returns the private key
     */
    public static String getPrivateKeyByWeId(String path, String weId) {

        if (null == weId) {
            logger.error("weId is null");
            return StringUtils.EMPTY;
        }

        // get the third paragraph of weId.
        String fileName = weId.substring(weId.lastIndexOf(":") + 1);

        // check the default passphrase
        String passphrase = PropertiesUtil.getProperty("default.passphrase");
        if (fileName.equalsIgnoreCase(passphrase)) {
            fileName = WeIdentityParamKeyConstant.DEFAULT_PRIVATE_KEY_FILE_NAME;
            path = ADMIN_PRIVKEY_PATH;
        }

        // check whether the path exists or not, then create the path and return.
        String checkPath = checkDir(path);
        String filePath = checkPath + fileName;

        logger.info("get private key from file, weId={}, filePath={}", weId, filePath);

        // get private key information from a file according to the third paragraph of weId.
        String privateKey = getDataByPath(filePath);
        return privateKey;
    }

    /**
     * check the path is exists, create and return the path if it does not exist.
     *
     * @param path the path
     * @return returns the path
     */
    public static String checkDir(String path) {

        String checkPath = path;

        // stitching the last slash.
        if (!checkPath.endsWith(SLASH_CHARACTER)) {
            checkPath = checkPath + SLASH_CHARACTER;
        }

        // check the path, create the path when it does not exist.
        File checkDir = new File(checkPath);
        if (!checkDir.exists()) {
            boolean success = checkDir.mkdirs();
            if (!success) {
                logger.error("checkDir.mkdirs");
            }
        }
        return checkPath;
    }

    /**
     * read data from the file.
     *
     * @param path the file path
     * @return returns the data
     */
    public static String getDataByPath(String path) {

        logger.info("get data form [{}]", path);
        FileInputStream fis = null;
        String str = null;
        try {
            fis = new FileInputStream(path);
            byte[] buff = new byte[fis.available()];
            int size = fis.read(buff);
            if (size > 0) {
                str = new String(buff, StandardCharsets.UTF_8);
            }
        } catch (FileNotFoundException e) {
            logger.error("the file path is not exists", e);
        } catch (IOException e) {
            logger.error("getDataByPath error", e);
        } finally {
            IOUtils.closeQuietly(fis);
        }
        return str;
    }

    /**
     * save data in a specified file.
     *
     * @param filePath save file path
     * @param dataStr save data
     * @return return the file path
     */
    public static String saveFile(String filePath, String dataStr) {

        logger.info("save data in to [{}]", filePath);
        OutputStreamWriter ow = null;
        FileOutputStream fos = null;
        try {
            File file = new File(filePath);
            fos = new FileOutputStream(file);
            ow = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
            ow.write(dataStr);
            return file.getAbsolutePath();
        } catch (IOException e) {
            logger.error("writer file exception", e);
        } finally {
            IOUtils.closeQuietly(ow);
            IOUtils.closeQuietly(fos);
        }
        return StringUtils.EMPTY;
    }

    /**
     * Build a default WeIdAuthentication from passed-in private key.
     *
     * @param privateKey the private key in String
     * @return weIdAuthentication
     */
    public static WeIdAuthentication buildWeIdAuthenticationFromPrivKey(String privateKey) {
        if (StringUtils.isBlank(privateKey) || !isPrivateKeyLengthValid(privateKey)) {
            logger.error("Private key format or size illegal.");
            return null;
        }
        String keyWeId = WeIdUtils.getWeIdFromPrivateKey(privateKey);
        WeIdAuthentication weIdAuthentication = new WeIdAuthentication();
        weIdAuthentication.setWeId(keyWeId);
        weIdAuthentication.setAuthenticationMethodId(keyWeId + "#keys-" + DataToolUtils.publicKeyStrFromPrivate(new BigInteger(privateKey)));
        WeIdPrivateKey weIdPrivateKey = new WeIdPrivateKey();
        weIdPrivateKey.setPrivateKey(privateKey);
        weIdAuthentication.setWeIdPrivateKey(weIdPrivateKey);
        return weIdAuthentication;
    }

    public static boolean isPrivateKeyLengthValid(String privateKey) {
        if (StringUtils.isBlank(privateKey)) {
            return false;
        }
        WeIdPrivateKey weIdPrivateKey = new WeIdPrivateKey();
        weIdPrivateKey.setPrivateKey(privateKey);
        if (WeIdUtils.isPrivateKeyValid(weIdPrivateKey)) {
            BigInteger privKeyBig = new BigInteger(privateKey, 10);
            BigInteger MAX_PRIVKEY_VALUE = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16);
            return (privKeyBig.compareTo(MAX_PRIVKEY_VALUE) <= 0);
        }
        return false;
    }

    public static boolean isPubkeyBytesValid(byte[] publicKey) {
        return publicKey.length == 64 && publicKey[0] != 0;
    }

    public static boolean isSecretKeyValid(byte[] secretKey) {
        return secretKey.length == 32 && secretKey[0] != 0;
    }

    public static boolean isKeyPairValid(byte[] privateKey, byte[] publicKey) {
        return isSecretKeyValid(privateKey)
            && isPubkeyBytesValid(publicKey);
    }
}
