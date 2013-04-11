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
package org.springframework.integration.x.stomp;

import java.util.HashMap;
import java.util.Map;

import org.springframework.integration.Message;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.x.ip.wsserver.ProtocolHandler;
import org.springframework.stomp.StompException;
import org.springframework.stomp.StompMessage;
import org.springframework.stomp.StompMessage.Command;
import org.springframework.util.Assert;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public class StompProtocolHandler implements ProtocolHandler {

	private static final byte[] EMPTY_PAYLOAD = new byte[0];

	private DisconnectCallback disconnectCallback;

	@Override
	public boolean canHandle(Message<?> message) {
		return message.getPayload() instanceof StompMessage;
	}

	@Override
	public Object handleProtocolMessage(Message<?> message) {
		Assert.isInstanceOf(StompMessage.class, message.getPayload());
		StompMessage requestMessage = (StompMessage) message.getPayload();
		Command command = requestMessage.getCommand();
		Assert.notNull(command, "command cannot be null");
		if (Command.CONNECT.equals(command) || Command.STOMP.equals(command)) {
			return this.connect(requestMessage, message.getHeaders().get(IpHeaders.CONNECTION_ID));
		}
		else if (Command.DISCONNECT.equals(command)) {
			if (this.disconnectCallback != null) {
				this.disconnectCallback.removeSubscriptions(message);
			}
			// TODO: RECEIPT
			return null;
		}
		return null;
	}

	protected StompMessage connect(StompMessage connectMessage, Object session) {
		// TODO: security
		Map<String, String> headers = new HashMap<String, String>();
		String acceptVersion = connectMessage.getHeaders().get("accept-version");
		if (acceptVersion == null) {
			// version 1.0
		}
		else if (acceptVersion.contains("1.2")) {
			headers.put("version", "1.2");
		}
		else if (acceptVersion.contains("1.1")) {
			headers.put("version", "1.1");
		}
		else {
			throw new StompException("Unsupported version '" + acceptVersion + "'");
		}
		headers.put("heart-beat", "0,0"); // TODO: enable heart-beats
		if (session != null) {
			headers.put("session", session.toString());
		}
		return new StompMessage(Command.CONNECTED, headers, EMPTY_PAYLOAD);
	}

	@Override
	public void registerCallback(DisconnectCallback callback) {
		this.disconnectCallback = callback;
	}

}
