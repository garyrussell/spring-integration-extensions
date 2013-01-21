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
package org.springframework.integration.x.ip.sockjs;

import java.io.IOException;
import java.io.OutputStream;

import org.springframework.integration.x.ip.websocket.WebSocketFrame;
import org.springframework.integration.x.ip.websocket.WebSocketSerializer;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public class SockJSSerializer extends WebSocketSerializer {

	@Override
	public void serialize(Object frame, OutputStream outputStream) throws IOException {
		Object modified;
		if (frame instanceof WebSocketFrame) {
			modified = frame;
		}
		else if (frame instanceof String) {
			String frameString = (String) frame;
			if (frameString.startsWith("HTTP/1.1")) {
				modified = frame;
			}
			else {
				modified = "a" + frameString;
			}
		}
		else {
			throw new IllegalArgumentException("'frame' must be a WebSocketFrame or String, not " + frame.getClass());
		}
		super.serialize(modified, outputStream);
	}


}
