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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.springframework.stomp.DefaultStompHandler;
import org.springframework.stomp.StompHandler;
import org.springframework.stomp.StompMessage;
import org.springframework.stomp.StompSubscriptionCallback;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public class StompHandlerTests {

	@Test
	public void testConnect() {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(StompMessage.COMMAND_KEY, "CONNECT");
		headers.put("accept-version", "1.1");
		headers.put("host", "localhost");
		StompHandler handler = new DefaultStompHandler();
		StompMessage connected = handler.handleStompMessage(new StompMessage(headers, null), "foo");
		assertEquals("CONNECTED", connected.getHeaders().get(StompMessage.COMMAND_KEY));
		assertEquals("1.1", connected.getHeaders().get("version"));
	}

	@Test
	public void testSubscribe() {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(StompMessage.COMMAND_KEY, "SUBSCRIBE");
		headers.put("destination", "foo");
		headers.put("id", "0");
		StompHandler handler = new DefaultStompHandler();
		final AtomicReference<Object> subscribedSession = new AtomicReference<Object>();
		final AtomicReference<String> subscribedId = new AtomicReference<String>();
		handler.addDestination("foo", new StompSubscriptionCallback() {
			@Override
			public void unsubscribed(Object session, String id) {
				fail("Unexpected call");
			}
			@Override
			public void subscribed(Object session, String id) {
				subscribedSession.set(session);
				subscribedId.set(id);
			}
		});
		handler.handleStompMessage(new StompMessage(headers, null), "foo");
		assertEquals("foo", subscribedSession.get());
		assertEquals("0", subscribedId.get());
	}

	@Test
	public void testUnsubscribe() {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(StompMessage.COMMAND_KEY, "UNSUBSCRIBE");
		headers.put("destination", "foo");
		headers.put("id", "0");
		StompHandler handler = new DefaultStompHandler();
		final AtomicReference<Object> unsubscribedSession = new AtomicReference<Object>();
		final AtomicReference<String> unsubscribedId = new AtomicReference<String>();
		handler.addDestination("foo", new StompSubscriptionCallback() {
			@Override
			public void unsubscribed(Object session, String id) {
				unsubscribedSession.set(session);
				unsubscribedId.set(id);
			}
			@Override
			public void subscribed(Object session, String id) {
				fail("Unexpected call");
			}
		});
		handler.handleStompMessage(new StompMessage(headers, null), "foo");
		assertEquals("foo", unsubscribedSession.get());
		assertEquals("0", unsubscribedId.get());
	}

}
