package com.webank.weid.http.service;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.webank.weid.http.protocol.response.EndpointInfo;
import com.webank.weid.http.util.EndpointDataUtil;

public class PropertiesTest {

    @Test
    public void testEndpointDataAll() throws Exception {
        List<EndpointInfo> endpointInfoList = EndpointDataUtil.getAllEndpointInfo();
        for (EndpointInfo endpointInfo :  endpointInfoList) {
            System.out.println(endpointInfo.toString());
        }
        EndpointInfo newInfo = new EndpointInfo();
        newInfo.setRequestName("remove-file");
        List<String> result = new ArrayList<>();
        result.add("127.0.3");
        newInfo.setInAddr(result);
        newInfo.setDescription("temp");
        EndpointDataUtil.mergeToCentral(newInfo);
        newInfo = new EndpointInfo();
        newInfo.setRequestName("remove-file");
        result = new ArrayList<>();
        result.add("127.0.3");
        result.add("127.0.4");
        newInfo.setInAddr(result);
        newInfo.setDescription("temp");
        EndpointDataUtil.mergeToCentral(newInfo);
        EndpointDataUtil.saveEndpointsToFile();
        endpointInfoList = EndpointDataUtil.getAllEndpointInfo();
        for (EndpointInfo endpointInfo :  endpointInfoList) {
            System.out.println(endpointInfo.toString());
        }
        Assert.assertTrue(EndpointDataUtil.removeEndpoint(newInfo));
        endpointInfoList = EndpointDataUtil.getAllEndpointInfo();
        for (EndpointInfo endpointInfo :  endpointInfoList) {
            System.out.println(endpointInfo.toString());
        }
    }
}
