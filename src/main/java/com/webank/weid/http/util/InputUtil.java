package com.webank.weid.http.util;

import org.apache.commons.lang3.StringUtils;

import com.webank.weid.constant.WeIdConstant;

/**
 * Handles all input related tasks.
 *
 * @author chaoxinhu
 */

public class InputUtil {

    /**
     * Remove the double quotes. Use this instead of JsonNode.textValue() to mitigate the possible
     * API body input style mixture (with double quotes or not), e.g. "cptId":10 or "cptId":"10".
     *
     * @param inputValue the input value String
     * @return the String
     */
    public static String removeDoubleQuotes(String inputValue) {
        return inputValue.replace(WeIdConstant.DOUBLE_QUOTE, StringUtils.EMPTY);
    }
}
