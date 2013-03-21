/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.integration.x.netty.inbound;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.util.Map.Entry;

import org.springframework.integration.Message;
import org.springframework.integration.mapping.InboundMessageMapper;
import org.springframework.integration.mapping.OutboundMessageMapper;
import org.springframework.integration.support.MessageBuilder;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public class NettyHttpMessageMapper implements InboundMessageMapper<HttpMessage>, OutboundMessageMapper<HttpMessage> {

	@Override
	public Message<?> toMessage(HttpMessage message) throws Exception {
		MessageBuilder<String> builder = MessageBuilder.withPayload(""); // TODO - handle POST
		for (Entry<String, String> entry : message.headers().entries()) {
			builder.setHeader("http_" + entry.getKey(), entry.getValue());
		}
		return builder.build();
	}

	@Override
	public HttpMessage fromMessage(Message<?> message) throws Exception {
		ByteBuf content = Unpooled.wrappedBuffer(((String) message.getPayload()).getBytes());
		FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);
		for (Entry<String, Object> entry : message.getHeaders().entrySet()) {
			if (entry.getKey().startsWith("http_")) {
				response.headers().add(entry.getKey().substring(5), entry.getValue());
			}
		}
		return response;
	}

}
