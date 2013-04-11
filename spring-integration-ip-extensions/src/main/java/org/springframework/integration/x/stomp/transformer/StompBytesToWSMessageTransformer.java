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
package org.springframework.integration.x.stomp.transformer;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.integration.Message;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.transformer.AbstractTransformer;
import org.springframework.stomp.StompMessage;
import org.springframework.stomp.StompMessage.Command;
import org.springframework.stomp.StompMessageConverter;
import org.springframework.util.Assert;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public class StompBytesToWSMessageTransformer extends AbstractTransformer {

	private volatile StompMessageConverter converter = new StompMessageConverter();

	public void setConverter(StompMessageConverter converter) {
		Assert.notNull(converter, "'converter' cannot be null");
		this.converter = converter;
	}

	@Override
	protected Object doTransform(Message<?> message) throws Exception {
		StompMessage stompMessage = this.converter.toStompMessage(message.getPayload());
		Map<String, ?> mappedHeaders = mapHeaders(stompMessage);
		Object isProtocol = mappedHeaders.get("ws_isProtocol");
		if (isProtocol != null && isProtocol == Boolean.TRUE) {
			return MessageBuilder.withPayload(stompMessage)
					.copyHeaders(mappedHeaders)
					.copyHeadersIfAbsent(message.getHeaders())
					.build();
		}
		else {
			return MessageBuilder.withPayload(stompMessage.getPayload())
					.copyHeaders(mappedHeaders)
					.copyHeadersIfAbsent(message.getHeaders())
					.build();
		}
	}

	private Map<String, ?> mapHeaders(StompMessage stompMessage) {
		Map<String, String> headers = stompMessage.getHeaders();
		Map<String, Object> siHeaders = new HashMap<String, Object>();
		Command command = stompMessage.getCommand();
		if (command == Command.SUBSCRIBE || command == Command.UNSUBSCRIBE) {
			siHeaders.put("ws_isSubscription", Boolean.TRUE);
		}
		else if (command == Command.CONNECT || command == Command.STOMP || command == Command.DISCONNECT) {
			siHeaders.put("ws_isProtocol", Boolean.TRUE);
		}
		siHeaders.put("ws_command", command.toString());
		for (Entry<String, String> entry : headers.entrySet()) {
			siHeaders.put("ws_" + entry.getKey(), entry.getValue());
		}
		return siHeaders;
	}

}
