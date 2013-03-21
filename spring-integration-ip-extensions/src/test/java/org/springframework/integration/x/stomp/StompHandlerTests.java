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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.stomp.DefaultStompHandler;
import org.springframework.stomp.StompHandler;
import org.springframework.stomp.StompMessage;
import org.springframework.stomp.StompMessage.Command;
import org.springframework.stomp.StompProcessorSupport;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public class StompHandlerTests {

	@Test
	public void testConnect() {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("accept-version", "1.1");
		headers.put("host", "localhost");
		StompHandler handler = new DefaultStompHandler();
		StompMessage connected = handler.handleStompMessage(new StompMessage(Command.CONNECT, headers, null), "foo");
		assertEquals(Command.CONNECTED, connected.getCommand());
		assertEquals("1.1", connected.getHeaders().get("version"));
	}

	@Test
	public void testStomp12() {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("accept-version", "1.2");
		headers.put("host", "localhost");
		StompHandler handler = new DefaultStompHandler();
		StompMessage connected = handler.handleStompMessage(new StompMessage(Command.STOMP, headers, null), "foo");
		assertEquals(Command.CONNECTED, connected.getCommand());
		assertEquals("1.2", connected.getHeaders().get("version"));
	}

	@Test
	public void testSubscribeAndUnsubscribe() {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("destination", "foo");
		headers.put("id", "0");
		StompHandler handler = new DefaultStompHandler();
		final AtomicReference<Object> subscribedSession = new AtomicReference<Object>();
		final AtomicReference<String> subscribedId = new AtomicReference<String>();
		handler.addDestination("foo", new StompProcessorSupport() {
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
		handler.handleStompMessage(new StompMessage(Command.SUBSCRIBE, headers, null), "foo");
		assertEquals("foo", subscribedSession.get());
		assertEquals("0", subscribedId.get());
		testUnsubscribe(handler);
	}

	private void testUnsubscribe(StompHandler handler) {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("destination", "foo");
		headers.put("id", "0");
		final AtomicReference<Object> unsubscribedSession = new AtomicReference<Object>();
		final AtomicReference<String> unsubscribedId = new AtomicReference<String>();
		handler.addDestination("foo", new StompProcessorSupport() {
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
		handler.handleStompMessage(new StompMessage(Command.UNSUBSCRIBE, headers, null), "foo");
		assertEquals("foo", unsubscribedSession.get());
		assertEquals("0", unsubscribedId.get());
	}

	@Test
	public void testSend() {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("destination", "foo");
		StompHandler handler = new DefaultStompHandler();
		final AtomicReference<byte[]> data = new AtomicReference<byte[]>();
		handler.addDestination("foo", new StompProcessorSupport() {
			@Override
			public void processSend(Object session, StompMessage message) {
				data.set(message.getPayload());
			}
		});
		handler.handleStompMessage(new StompMessage(Command.SEND, headers, "baz".getBytes()), "bar");
		assertEquals("baz", new String(data.get()));
	}

	@Test
	public void testSendInTx() {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("destination", "foo");
		headers.put("transaction", "tx1");
		StompHandler handler = new DefaultStompHandler();
		final AtomicReference<Collection<StompMessage>> data = new AtomicReference<Collection<StompMessage>>();
		handler.addDestination("foo", new StompProcessorSupport() {
			@Override
			public void processTransaction(Object session, Collection<StompMessage> messages) {
				data.set(messages);
			}
		});
		handler.handleStompMessage(new StompMessage(Command.BEGIN, headers, null), "bar");
		handler.handleStompMessage(new StompMessage(Command.SEND, headers, "baz".getBytes()), "bar");
		handler.handleStompMessage(new StompMessage(Command.SEND, headers, "qux".getBytes()), "bar");
		handler.handleStompMessage(new StompMessage(Command.COMMIT, headers, null), "bar");
		assertEquals(2, data.get().size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testTxAbort() {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("destination", "foo");
		headers.put("transaction", "tx1");
		StompHandler handler = new DefaultStompHandler();
		final AtomicReference<Collection<StompMessage>> data = new AtomicReference<Collection<StompMessage>>();
		handler.addDestination("foo", new StompProcessorSupport() {
			@Override
			public void processTransaction(Object session, Collection<StompMessage> messages) {
				data.set(messages);
			}
		});
		handler.handleStompMessage(new StompMessage(Command.BEGIN, headers, null), "bar");
		handler.handleStompMessage(new StompMessage(Command.SEND, headers, "baz".getBytes()), "bar");
		handler.handleStompMessage(new StompMessage(Command.SEND, headers, "qux".getBytes()), "bar");
		DirectFieldAccessor accessor = new DirectFieldAccessor(handler);
		assertEquals(2, ((Map<String, Map<String, Collection<?>>>) accessor.getPropertyValue("transactions")).get("bar").get("tx1").size());
		handler.handleStompMessage(new StompMessage(Command.ABORT, headers, null), "bar");
		assertNull(data.get());
		assertNull(((Map<String, Map<String, Collection<?>>>) accessor.getPropertyValue("transactions")).get("bar").get("tx1"));
	}


}
