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
package org.springframework.integration.x.ip.wsserver.foo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.support.MessageBuilder;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public class FooService extends IntegrationObjectSupport {

	protected final Log logger = LogFactory.getLog(this.getClass());

	private final Map<String, Set<String>> subscriptions = new HashMap<String, Set<String>>();

	private final static long delay = 5000;

	@Autowired
	private MessageChannel toBrowser;

	@Autowired
	private ApplicationContext applicationContext;

	private final AtomicInteger messageId = new AtomicInteger();

	@Override
	protected void onInit() throws Exception {
		this.getTaskScheduler().scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				Set<String> toRemove = new HashSet<String>();
				for (Entry<String, Set<String>> entry : subscriptions.entrySet()) {
					String connectionId = entry.getKey();
					for (String id : entry.getValue()) {
						Message<?> message = MessageBuilder.withPayload("Hello, world! from " + applicationContext.getId())
								.setHeader(IpHeaders.CONNECTION_ID, connectionId)
								.setHeader("ws_command", "MESSAGE")
								.setHeader("ws_destination", applicationContext.getId())
								.setHeader("ws_subscription", id)
								.setHeader("ws_message-id", String.valueOf(messageId.incrementAndGet()))
								.setHeader("ws_content-type", "text/plain")
								.build();
						try {
							toBrowser.send(message);
						}
						catch (Exception e) {
							logger.error("Removing subscription " + id + " for connection " + connectionId +
									" in " + applicationContext.getId(), e);
							toRemove.add(connectionId);
						}
					}
				}
				for (String connectionId : toRemove) {
					subscriptions.remove(connectionId);
				}
			}
		}, delay);
	}

	public void handleSubscription(Message<?> message) {
		MessageHeaders headers = message.getHeaders();
		String command = (String) headers.get("ws_command");
		String id = (String) headers.get("ws_id");
		String connectionId = (String) headers.get(IpHeaders.CONNECTION_ID);
		Set<String> subs = this.subscriptions.get(headers.get(IpHeaders.CONNECTION_ID));
		if ("SUBSCRIBE".equals(command)) {
			if (subs == null) {
				subs = new HashSet<String>();
				this.subscriptions.put(connectionId, subs);
			}
			logger.info("Added subscription " + id + " for connection " + connectionId +
					" in " + this.applicationContext.getId());
			subs.add(id);
		}
		else if ("UNSUBSCRIBE".equals(command)) {
			Iterator<String> iterator = subs.iterator();
			while (iterator.hasNext()) {
				String subId = iterator.next();
				if (subId.equals(id)) {
					iterator.remove();
					logger.info("Removed subscription " + id + " for connection " + connectionId +
							" in " + this.applicationContext.getId());
					break;
				}
			}
		}
	}
}
