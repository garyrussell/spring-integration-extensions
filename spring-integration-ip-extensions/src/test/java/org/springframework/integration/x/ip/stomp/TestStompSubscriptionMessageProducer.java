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

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.x.stomp.StompSubscriptionMessageProducer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stomp.StompHandler;
import org.springframework.stomp.StompMessage;
import org.springframework.stomp.StompMessage.Command;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public class TestStompSubscriptionMessageProducer extends StompSubscriptionMessageProducer {

	private volatile int messageNumber;

	private final long delay;

	final Runnable runner = new Runnable() {
		@Override
		public void run() {
			try {
				int thisMessageNumber = ++messageNumber;
				for (Entry<String, Set<String>> entry : getSubscriptions().entrySet()) {
					String connectionId = entry.getKey();
					for (String id : entry.getValue()) {
						byte[] payload = "Hello, world!".getBytes();
						doSend(thisMessageNumber, connectionId, id, payload);
					}
				}
			}
			finally {
				schedule();
			}
		}

	};

	public TestStompSubscriptionMessageProducer(String destination, long delay) {
		super(destination);
		this.delay = delay;
	}

	public TestStompSubscriptionMessageProducer(String destination, StompHandler handler, long delay) {
		super(destination, handler);
		this.delay = delay;
	}

	@Override
	protected void onOnInit() {
		super.onOnInit();
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.initialize();
		this.setTaskScheduler(taskScheduler);
		schedule();
	}

	private void doSend(int thisMessageNumber, String connectionId, String id, byte[] payload) {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("subscription", id);
		headers.put("message-id", Integer.toString(thisMessageNumber));
		headers.put("destination", getDestination());
		headers.put("content-type", "text/plain");
		StompMessage stompMessage = new StompMessage(Command.MESSAGE, headers, payload);
		sendMessage(MessageBuilder.withPayload(stompMessage)
				.setHeader(IpHeaders.CONNECTION_ID, connectionId)
				.build());
	}

	@Override
	protected void handleProcessTransaction(Object session, Collection<StompMessage> messages) {
		int thisMessageNumber = ++this.messageNumber;
		for (Entry<String, Set<String>> entry : getSubscriptions().entrySet()) {
			String connectionId = entry.getKey();
			if (connectionId.equals(session)) {
				StringBuilder builder = new StringBuilder();
				builder.append("Received transaction with: ");
				for (StompMessage message : messages) {
					builder.append("[").append(new String(message.getPayload())).append("]");
				}
				byte[] payload = builder.toString().getBytes();
				for (String id : entry.getValue()) {
					doSend(thisMessageNumber, connectionId, id, payload);
				}
			}
		}
	}

	private void schedule() {
		this.getTaskScheduler().schedule(runner, new Date(System.currentTimeMillis() + this.delay));
	}

};
