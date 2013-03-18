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
package org.springframework.integration.x.ip.stomp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.integration.Message;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.x.stomp.StompSubscriptionMessageProducer;
import org.springframework.stomp.DefaultStompHandler;
import org.springframework.stomp.StompHandler;
import org.springframework.stomp.StompMessage;
import org.springframework.stomp.StompMessage.Command;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public class StompSubscriptionMessageProducerTests {

	@Test
	public void test() throws Exception {
		StompHandler handler = new DefaultStompHandler();
		StompSubscriptionMessageProducer producer = new TestStompSubscriptionMessageProducer("foo", handler, 100);
		QueueChannel output = new QueueChannel();
		producer.setOutputChannel(output);
		producer.afterPropertiesSet();

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("destination", "foo");
		headers.put("id", "0");
		handler.handleStompMessage(new StompMessage(Command.SUBSCRIBE, headers, null), "baz");
		headers.put("id", "1");
		handler.handleStompMessage(new StompMessage(Command.SUBSCRIBE,headers, null), "baz");
		headers.put("id", "0");
		handler.handleStompMessage(new StompMessage(Command.SUBSCRIBE,headers, null), "qux");

		Thread.sleep(1000);
		headers.put("id", "0");
		handler.handleStompMessage(new StompMessage(Command.SUBSCRIBE,headers, null), "baz");
		headers.put("id", "1");
		handler.handleStompMessage(new StompMessage(Command.SUBSCRIBE,headers, null), "baz");
		headers.put("id", "0");
		handler.handleStompMessage(new StompMessage(Command.SUBSCRIBE,headers, null), "qux");

		assertTrue(output.getQueueSize() >= 24);

		Message<?> message = output.receive(0);
		assertNotNull(message);
		assertEquals("baz", message.getHeaders().get(IpHeaders.CONNECTION_ID));
		StompMessage stompMessage = (StompMessage) message.getPayload();
		assertEquals(Command.MESSAGE, stompMessage.getCommand());
		assertEquals("foo", stompMessage.getHeaders().get("destination"));
//		assertEquals("0", stompMessage.getHeaders().get("subscription"));
		assertEquals("1", stompMessage.getHeaders().get("message-id"));
		assertEquals("Hello, world!", new String(stompMessage.getPayload()));

		message = output.receive(0);
		assertNotNull(message);
		assertEquals("baz", message.getHeaders().get(IpHeaders.CONNECTION_ID));
		stompMessage = (StompMessage) message.getPayload();
		assertEquals(Command.MESSAGE, stompMessage.getCommand());
		assertEquals("foo", stompMessage.getHeaders().get("destination"));
//		assertEquals("1", stompMessage.getHeaders().get("subscription"));
		assertEquals("1", stompMessage.getHeaders().get("message-id"));
		assertEquals("Hello, world!", new String(stompMessage.getPayload()));

		message = output.receive(0);
		assertNotNull(message);
		assertEquals("qux", message.getHeaders().get(IpHeaders.CONNECTION_ID));
		stompMessage = (StompMessage) message.getPayload();
		assertEquals(Command.MESSAGE, stompMessage.getCommand());
		assertEquals("foo", stompMessage.getHeaders().get("destination"));
//		assertEquals("0", stompMessage.getHeaders().get("subscription"));
		assertEquals("1", stompMessage.getHeaders().get("message-id"));
		assertEquals("Hello, world!", new String(stompMessage.getPayload()));

		message = output.receive(0);
		assertNotNull(message);
		assertEquals("baz", message.getHeaders().get(IpHeaders.CONNECTION_ID));
		stompMessage = (StompMessage) message.getPayload();
		assertEquals(Command.MESSAGE, stompMessage.getCommand());
		assertEquals("foo", stompMessage.getHeaders().get("destination"));
//		assertEquals("0", stompMessage.getHeaders().get("subscription"));
		assertEquals("2", stompMessage.getHeaders().get("message-id"));
		assertEquals("Hello, world!", new String(stompMessage.getPayload()));

		message = output.receive(0);
		assertNotNull(message);
		assertEquals("baz", message.getHeaders().get(IpHeaders.CONNECTION_ID));
		stompMessage = (StompMessage) message.getPayload();
		assertEquals(Command.MESSAGE, stompMessage.getCommand());
		assertEquals("foo", stompMessage.getHeaders().get("destination"));
//		assertEquals("1", stompMessage.getHeaders().get("subscription"));
		assertEquals("2", stompMessage.getHeaders().get("message-id"));
		assertEquals("Hello, world!", new String(stompMessage.getPayload()));

		message = output.receive(0);
		assertNotNull(message);
		assertEquals("qux", message.getHeaders().get(IpHeaders.CONNECTION_ID));
		stompMessage = (StompMessage) message.getPayload();
		assertEquals(Command.MESSAGE, stompMessage.getCommand());
		assertEquals("foo", stompMessage.getHeaders().get("destination"));
//		assertEquals("0", stompMessage.getHeaders().get("subscription"));
		assertEquals("2", stompMessage.getHeaders().get("message-id"));
		assertEquals("Hello, world!", new String(stompMessage.getPayload()));

		output.clear();
	}

}
