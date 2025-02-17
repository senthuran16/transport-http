/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.transport.http.netty.contractimpl.common.states;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.timeout.IdleStateHandler;
import org.wso2.transport.http.netty.contract.Constants;
import org.wso2.transport.http.netty.contract.HttpResponseFuture;
import org.wso2.transport.http.netty.contractimpl.sender.TargetHandler;
import org.wso2.transport.http.netty.contractimpl.sender.states.SenderState;
import org.wso2.transport.http.netty.message.HttpCarbonMessage;

import java.util.concurrent.TimeUnit;

/**
 * Context class to manipulate current state of the message.
 */
public class SenderReqRespStateManager {

    public SenderState state;

    private final Channel nettyTargetChannel;

    public SenderReqRespStateManager(Channel nettyTargetChannel) {
        this.nettyTargetChannel = nettyTargetChannel;
    }

    public void writeOutboundRequestHeaders(HttpCarbonMessage httpOutboundRequest, HttpContent httpContent) {
        state.writeOutboundRequestHeaders(httpOutboundRequest, httpContent);
    }

    public void writeOutboundRequestEntity(HttpCarbonMessage httpOutboundRequest, HttpContent httpContent) {
        state.writeOutboundRequestEntity(httpOutboundRequest, httpContent);
    }

    public void readInboundResponseHeaders(TargetHandler targetHandler, HttpResponse httpInboundResponse) {
        state.readInboundResponseHeaders(targetHandler, httpInboundResponse);
    }

    public void readInboundResponseEntityBody(ChannelHandlerContext ctx, HttpContent httpContent,
                                              HttpCarbonMessage inboundResponseMsg) throws Exception {
        state.readInboundResponseEntityBody(ctx, httpContent, inboundResponseMsg);
    }

    public void handleAbruptChannelClosure(HttpResponseFuture httpResponseFuture) {
        state.handleAbruptChannelClosure(httpResponseFuture);
    }

    public void handleIdleTimeoutConnectionClosure(HttpResponseFuture httpResponseFuture, String channelID) {
        nettyTargetChannel.pipeline().remove(Constants.IDLE_STATE_HANDLER);
        nettyTargetChannel.close();

        state.handleIdleTimeoutConnectionClosure(httpResponseFuture, channelID);
    }

    public void configIdleTimeoutTrigger(int socketIdleTimeout) {
        ChannelPipeline pipeline = nettyTargetChannel.pipeline();
        IdleStateHandler idleStateHandler = new IdleStateHandler(0, 0, socketIdleTimeout, TimeUnit.MILLISECONDS);
        if (pipeline.get(Constants.TARGET_HANDLER) == null) {
            pipeline.addLast(Constants.IDLE_STATE_HANDLER, idleStateHandler);
        } else {
            pipeline.addBefore(Constants.TARGET_HANDLER, Constants.IDLE_STATE_HANDLER, idleStateHandler);
        }
    }
}
