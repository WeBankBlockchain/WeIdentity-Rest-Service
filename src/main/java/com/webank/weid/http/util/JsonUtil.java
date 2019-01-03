package com.webank.weid.http.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.webank.weid.http.exception.BizException;

public class JsonUtil {

    /**
     * json string to object.
     * @param obj
     * @param jsonStr
     * @return
     */
    public static Object jsonStrToObj(Object obj, String jsonStr) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonStr, obj.getClass());
        } catch (Exception e) {
            throw new BizException("[jsonToObj]: json to object exception", e);
        }
    }

    /**
     * object to json string.
     * @param obj
     * @return
     */
    public static String objToJsonStr(Object obj) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new BizException("[objToJson]: object to json exception", e);
        }
    }

}
