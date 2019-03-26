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

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import com.webank.weid.util.JsonUtil;

/**
 * Handles all input related tasks.
 *
 * @author chaoxinhu
 */

public class InputUtil {

    /**
     * Remove the double quotes. Use this instead of plain JsonNode.textValue() to mitigate possible
     * API body input style mixture (with double quotes or not), e.g. "cptId":10 or "cptId":"10".
     *
     * @param inputValue the input value String
     * @return the String
     */
    public static String removeDoubleQuotes(String inputValue) {
        return inputValue.replace("\"", StringUtils.EMPTY);
    }

    /**
     * Convert Json String into a sorted map object (keys ordered).
     *
     * @param looseJson the input json String
     * @return the String
     */
    public static Map<String, Object> convertJsonToSortedMap(String looseJson) {
        try {
            Map<String, Object> map = (Map<String, Object>) JsonUtil
                .jsonStrToObj(new HashMap<String, Object>(), looseJson);
            return new TreeMap<>(map);
        } catch (Exception e) {
            return null;
        }
    }
}
