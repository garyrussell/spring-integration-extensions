/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.integration.x.netty.inbound;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.x.netty.inbound.NettyRequestHandlingEndpoint.InboundNettyAdapterCallback;


/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public class HttpChannelInitializer extends ChannelInitializer<Channel> implements InitializingBean {

	private final InboundNettyAdapterCallback adapter;

	public HttpChannelInitializer(NettyRequestHandlingEndpoint endpoint) {
		adapter = endpoint.registerChannelInitializer(this);
	}

	@Override
	protected void initChannel(Channel ch) throws Exception {
		ChannelPipeline pipeline = ch.pipeline();
		pipeline.addLast("decoder", new HttpRequestDecoder());
		pipeline.addLast("encoder", new HttpResponseEncoder());
		pipeline.addLast("handler", new ChannelInboundMessageHandlerAdapter<HttpMessage>() {
			@Override
			public void messageReceived(ChannelHandlerContext ctx, HttpMessage msg) throws Exception {
				if (adapter.isExpectReply()) {
					Object reply = adapter.sendAndReceive(msg);
					if (reply != null) {
						ctx.write(reply);
					}
				}
				else {
					adapter.send(msg);
				}
			}

		});
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		NettyHttpMessageMapper nettyHttpMessageMapper = new NettyHttpMessageMapper();
		this.adapter.setRequestMapper(nettyHttpMessageMapper);
		this.adapter.setReplyMapper(nettyHttpMessageMapper);
	}

}
