/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.carbon.transport.http.netty.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.messaging.CarbonMessage;
import org.wso2.carbon.messaging.StatusCarbonMessage;
import org.wso2.carbon.messaging.exceptions.ClientConnectorException;
import org.wso2.carbon.transport.http.netty.common.Constants;
import org.wso2.carbon.transport.http.netty.contract.HTTPConnectorFactory;
import org.wso2.carbon.transport.http.netty.contract.websocket.WebSocketBinaryMessage;
import org.wso2.carbon.transport.http.netty.contract.websocket.WebSocketClientConnector;
import org.wso2.carbon.transport.http.netty.contract.websocket.WebSocketCloseMessage;
import org.wso2.carbon.transport.http.netty.contract.websocket.WebSocketConnectorListener;
import org.wso2.carbon.transport.http.netty.contract.websocket.WebSocketControlMessage;
import org.wso2.carbon.transport.http.netty.contract.websocket.WebSocketInitMessage;
import org.wso2.carbon.transport.http.netty.contract.websocket.WebSocketMessage;
import org.wso2.carbon.transport.http.netty.contract.websocket.WebSocketTextMessage;
import org.wso2.carbon.transport.http.netty.contractimpl.HTTPConnectorFactoryImpl;
import org.wso2.carbon.transport.http.netty.contractimpl.websocket.WebSocketMessageImpl;
import org.wso2.carbon.transport.http.netty.internal.websocket.WebSocketUtil;
import org.wso2.carbon.transport.http.netty.message.HTTPMessageUtil;
import org.wso2.carbon.transport.http.netty.util.client.websocket.WebSocketTestConstants;

import java.io.IOException;
import java.net.ProtocolException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.websocket.Session;

/**
 * Server Connector Listener to check WebSocket pass-through scenarios.
 */
public class WebSocketPassthroughServerConnectorListener implements WebSocketConnectorListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketPassthroughServerConnectorListener.class);

    private final HTTPConnectorFactory connectorFactory = new HTTPConnectorFactoryImpl();
    private final Map<String, Session> sessionsMap = new ConcurrentHashMap<>();

    @Override
    public void onMessage(WebSocketInitMessage initMessage) {
        try {
            CarbonMessage carbonMessage = new StatusCarbonMessage(org.wso2.carbon.messaging.Constants.STATUS_OPEN,
                                                                  0, null);
            CarbonMessage serverCarbonMessage = HTTPMessageUtil.convertWebSocketInitMessage(initMessage);
            carbonMessage.setProperty(Constants.REMOTE_ADDRESS, "ws://localhost:8490/websocket");
            carbonMessage.setProperty(Constants.TO, "myService");
            carbonMessage.setProperty(Constants.SRC_HANDLER, serverCarbonMessage.getProperty(Constants.SRC_HANDLER));
            WebSocketClientConnector clientConnector = connectorFactory.getWSClientConnector(carbonMessage);

            WebSocketConnectorListener connectorListener = new WebSocketPassthroughClientConnectorListener();
            Map<String, String> customHeaders = new HashMap<>();
            Session clientSession = clientConnector.connect(connectorListener, customHeaders);
            Session serverSession = initMessage.handshake();
            sessionsMap.put(serverSession.getId(), clientSession);
        } catch (ProtocolException | ClientConnectorException e) {
            logger.error("Error occurred during connection: " + e.getMessage());
        }

    }

    @Override
    public void onMessage(WebSocketTextMessage textMessage) {
        try {
            Session clientSession = sessionsMap.get(textMessage.getChannelSession().getId());
            clientSession.getBasicRemote().sendText(textMessage.getText());
        } catch (IOException e) {
            logger.error("IO error when sending message: " + e.getMessage());
        }
    }

    @Override
    public void onMessage(WebSocketBinaryMessage binaryMessage) {
        try {
            Session clientSession = sessionsMap.get(binaryMessage.getChannelSession().getId());
            clientSession.getBasicRemote().sendBinary(binaryMessage.getByteBuffer());
        } catch (IOException e) {
            logger.error("IO error when sending message: " + e.getMessage());
        }
    }

    @Override
    public void onMessage(WebSocketControlMessage controlMessage) {
        // Do nothing.
    }

    @Override
    public void onMessage(WebSocketCloseMessage closeMessage) {
        try {
            Session clientSession = sessionsMap.get(closeMessage.getChannelSession().getId());
            clientSession.close();
        } catch (IOException e) {
            logger.error("IO error when sending message: " + e.getMessage());
        }
    }

    @Override
    public void onError(Throwable throwable) {

    }
}
