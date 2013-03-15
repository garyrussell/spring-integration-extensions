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
			int thisMessageNumber = ++messageNumber;
			for (Entry<String, Set<String>> entry : getSubscriptions().entrySet()) {
				String connectionId = entry.getKey();
				for (String id : entry.getValue()) {
					Map<String, String> headers = new HashMap<String, String>();
					headers.put(StompMessage.COMMAND_KEY, "MESSAGE");
					headers.put("subscription", id);
					headers.put("message-id", Integer.toString(thisMessageNumber));
					headers.put("destination", getDestination());
					headers.put("content-type", "text/plain");
					byte[] payload = "Hello, world!".getBytes();
					StompMessage stompMessage = new StompMessage(headers, payload);
					sendMessage(MessageBuilder.withPayload(stompMessage)
							.setHeader(IpHeaders.CONNECTION_ID, connectionId)
							.build());
				}
			}
			schedule();
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

	private void schedule() {
		this.getTaskScheduler().schedule(runner, new Date(System.currentTimeMillis() + this.delay));
	}

};
