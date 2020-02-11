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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.webank.weid.constant.CredentialConstant;
import com.webank.weid.constant.ParamKeyConstant;
import com.webank.weid.exception.DataTypeCastException;
import com.webank.weid.util.DateUtils;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.lang3.StringUtils;

/**
 * Handles all Json related tasks.
 *
 * @author chaoxinhu
 */

public class JsonUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final ObjectWriter OBJECT_WRITER;
    private static final ObjectReader OBJECT_READER;

    static {
        // sort by letter
        OBJECT_MAPPER.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
        // when map is serialization, sort by key
        OBJECT_MAPPER.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        // ignore mismatched fields
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // use field for serialize and deSerialize
        OBJECT_MAPPER.setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE);
        OBJECT_MAPPER.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
        OBJECT_MAPPER.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        OBJECT_WRITER = OBJECT_MAPPER.writer().withDefaultPrettyPrinter();
        OBJECT_READER = OBJECT_MAPPER.reader();
    }

    /**
     * Json String to Object.
     *
     * @param obj Object
     * @param jsonStr Json String
     * @return Object
     */
    public static Object jsonStrToObj(Object obj, String jsonStr) {

        try {
            return OBJECT_READER.readValue(
                OBJECT_MAPPER.getFactory().createParser(jsonStr),
                obj.getClass());
        } catch (IOException e) {
            throw new DataTypeCastException(e);
        }
    }

    /**
     * Object to Json String.
     *
     * @param obj Object
     * @return String
     */
    public static String objToJsonStr(Object obj) {

        try {
            return OBJECT_WRITER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new DataTypeCastException(e);
        }
    }

    /**
     * Convert a Map to compact Json output, with keys ordered. Use Jackson JsonNode toString() to ensure key order and compact output.
     *
     * @param map input map
     * @return JsonString
     */
    public static String mapToCompactJson(Map<String, Object> map) throws Exception {
        ObjectWriter objectWriter = OBJECT_MAPPER.writer();
        return objectWriter.writeValueAsString(map);
    }

    /**
     * Convert a POJO to Map.
     *
     * @param object POJO
     * @return Map
     */
    public static Map<String, Object> objToMap(Object object) throws Exception {
        JsonNode jsonNode = OBJECT_MAPPER.readTree(objToJsonStr(object));
        return (HashMap<String, Object>) OBJECT_MAPPER.convertValue(jsonNode, HashMap.class);
    }

    /**
     * Remove the double quotes. Use this instead of plain JsonNode.textValue() to mitigate possible API body input style mixture (with double quotes
     * or not), e.g. "cptId":10 or "cptId":"10".
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
            Map<String, Object> map = (Map<String, Object>) jsonStrToObj(
                new HashMap<String, Object>(), looseJson);
            return new TreeMap<>(map);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Format a credential pojo object into a Json format Map by tuning the context and date.
     *
     * @param originalMap the original credential (in map)
     * @return the new map
     * @throws Exception date conversion related exceptions
     */
    public static Map<String, Object> reformatCredentialPojoToJson(Map<String, Object> originalMap)
        throws Exception {
        Map<String, Object> credMap = new HashMap<>(originalMap);

        // Convert context into @context
        Object context = credMap
            .get(CredentialConstant.CREDENTIAL_CONTEXT_PORTABLE_JSON_FIELD);
        credMap.remove(CredentialConstant.CREDENTIAL_CONTEXT_PORTABLE_JSON_FIELD);
        if (!StringUtils.isEmpty(context.toString())) {
            credMap.put(ParamKeyConstant.CONTEXT, context);
        } else if (StringUtils.isEmpty(credMap.get(ParamKeyConstant.CONTEXT).toString())) {
            credMap
                .put(ParamKeyConstant.CONTEXT,
                    CredentialConstant.DEFAULT_CREDENTIAL_CONTEXT);
        }
        // Convert dates
        String issuanceDate = credMap.get(ParamKeyConstant.ISSUANCE_DATE).toString();
        String expirationDate = credMap.get(ParamKeyConstant.EXPIRATION_DATE).toString();
        credMap.put(ParamKeyConstant.ISSUANCE_DATE,
            DateUtils.convertUtcDateToTimeStamp(issuanceDate));
        credMap.put(ParamKeyConstant.EXPIRATION_DATE,
            DateUtils.convertUtcDateToTimeStamp(expirationDate));
        return credMap;
    }


}
