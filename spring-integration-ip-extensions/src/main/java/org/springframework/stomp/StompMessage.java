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
package org.springframework.stomp;

import java.util.Collections;
import java.util.Map;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public class StompMessage {

	public enum Command {
		CONNECT,
		DISCONNECT,
		SUBSCRIBE,
		UNSUBSCRIBE,

		CONNECTED,
		MESSAGE
	}

	private final Command command;

	private final Map<String, String> headers;

	private final byte[] payload;

	public StompMessage(Command command, Map<String, String> headers, byte[] payload) {
		this.command = command;
		this.headers = headers;
		this.payload = payload;
	}

	public Command getCommand() {
		return command;
	}

	public Map<String, String> getHeaders() {
		return Collections.unmodifiableMap(this.headers);
	}

	public byte[] getPayload() {
		return payload;
	}

	@Override
	public String toString() {
		return "StompMessage [headers=" + headers + ", payload=" + new String(payload) + "]";
	}


}

