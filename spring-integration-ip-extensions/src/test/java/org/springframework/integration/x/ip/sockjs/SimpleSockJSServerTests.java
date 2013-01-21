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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.x.ip.websocket.WebSocketEvent;
import org.springframework.integration.x.ip.websocket.WebSocketFrame;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public class SimpleSockJSServerTests {

	public static void main(String[] args) throws Exception {
		new ClassPathXmlApplicationContext(SimpleSockJSServerTests.class.getSimpleName() + "-context.xml", SimpleSockJSServerTests.class);
		System.out.println("Hit Enter To Terminate...");
		System.in.read();
		System.exit(0);
	}

	public static class DemoService implements ApplicationListener<WebSocketEvent> {

		private static final Log logger = LogFactory.getLog(DemoService.class);

		@Autowired
		private MessageChannel toBrowser;

		public String handleRequest(Message<WebSocketFrame> message) {
			WebSocketFrame dataFrame = message.getPayload();
			if (dataFrame.getType() == WebSocketFrame.TYPE_DATA) {
				// add 'echo:' to string
				return dataFrame.getPayload().replace("[\"", "[\"echo:");
			}
			return null;
		}

		@Override
		public void onApplicationEvent(WebSocketEvent event) {
			logger.info(event);
			if (WebSocketEvent.HANDSHAKE_COMPLETE.equals(event.getType())) {
			}
			else if (WebSocketEvent.WEBSOCKET_CLOSED.equals(event.getType())) {
			}
		}
	}

}
