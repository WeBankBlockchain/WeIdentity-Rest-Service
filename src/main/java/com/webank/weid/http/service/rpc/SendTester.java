/*
 *       Copyright© (2019) WeBank Co., Ltd.
 *
 *       This file is part of weidentity-http-service.
 *
 *       weidentity-http-service is free software: you can redistribute it and/or modify
 *       it under the terms of the GNU Lesser General Public License as published by
 *       the Free Software Foundation, either version 3 of the License, or
 *       (at your option) any later version.
 *
 *       weidentity-http-service is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU Lesser General Public License for more details.
 *
 *       You should have received a copy of the GNU Lesser General Public License
 *       along with weidentity-http-service.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.webank.weid.http.service.rpc;

import java.util.ArrayList;
import java.util.List;

import com.webank.weid.http.protocol.request.EndpointRequest;

public class SendTester {

    public static void main(String[] args) throws Exception {
        // Test direct send via client
        String hostport = "127.0.1:6090";
        // need boot up server first
        RpcClient rpcClient = new RpcClient(hostport);
        for (int i = 0; i < 2; i++) {
            String uuid = rpcClient.send("Hello, Server!").getRespBody();
            System.out.println(rpcClient.get(uuid));
        }
        List<String> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String uuid = rpcClient.send(String.valueOf(i)).getRespBody();
            list.add(uuid);
            Thread.sleep(100);
        }
        for (String uuid: list) {
            System.out.println(rpcClient.get(uuid));
        }
        System.out.println("RPC valid: " + rpcClient.isValid());
        rpcClient.close();
        System.out.println("RPC valid: " + rpcClient.isValid());
        rpcClient.reconnect();
        for (int i = 0; i < 2; i++) {
            String uuid = rpcClient.send("Again, Server!").getRespBody();
            System.out.println(rpcClient.get(uuid));
        }
        System.out.println("RPC valid: " + rpcClient.isValid());
        rpcClient.close();

        RpcConnectionHandler rpcConnectionHandler = RpcConnectionHandler.getInstance();
        List<String> toAddrList = new ArrayList<>();
        toAddrList.add(hostport);
        toAddrList.add("127.0.0.1:6092");
        EndpointRequest endpointRequest = new EndpointRequest();
        endpointRequest.setRequestName("abc");
        endpointRequest.setRequestBody("test");
        String uuid;
        String result;
        for (int i = 0; i < 10; i++) {
            uuid = rpcConnectionHandler.randomSend(toAddrList, endpointRequest).getRespBody();
            System.out.println(
                "Round: " + i + ", Send req name: " + endpointRequest.getRequestName() + ", body: "
                    + endpointRequest.getRequestBody() + ". uuid: " + uuid);
            result = rpcConnectionHandler.get(uuid).getRespBody();
            System.out.println("Round: " + i + ", Result: " + result + ". uuid: " + uuid);
        }
    }

}
