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

package com.webank.weid.http.service.rpc;

import com.webank.weid.http.service.rpc.proxy.AmopProxyCode;
import com.webank.weid.http.service.rpc.proxy.AmopProxyServer;
import com.webank.weid.http.service.rpc.proxy.AmopRpcConnectionHandler;

public class SendTester {

    public static void main(String[] args) throws Exception {
        AmopProxyServer server = new AmopProxyServer();
        // need boot up server first
        server.run();
        // Test direct send via client
        AmopRpcConnectionHandler.init();
        // Simulate server broadcast
        server.broadcastToAllHosts("heartbeat!");
        // Simulate blind register
        AmopRpcConnectionHandler.registerListeningTopic("webank");
        // Client connect
        AmopRpcConnectionHandler.connect("did:weid:0x111", "12345");
        // Client register
        AmopRpcConnectionHandler.registerListeningTopic("webank");
        AmopRpcConnectionHandler.sendMessage("welcome!" + "```" + "webank");
        // simulate server send message
        server.send("127.0.0.1", "welcome!", AmopProxyCode.SERVER_AMOP_SEND.getCode());
        AmopRpcConnectionHandler.close();
    }

}
