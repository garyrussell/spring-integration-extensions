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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.stomp.DefaultStompHandler;
import org.springframework.stomp.StompHandler;
import org.springframework.stomp.StompSubscriptionCallback;
import org.springframework.util.Assert;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public abstract class StompSubscriptionMessageProducer extends MessageProducerSupport {

	private final StompHandler stompHandler;

	private final String destination;

	private final Map<String, Set<String>> subscriptions = Collections.synchronizedMap(new HashMap<String, Set<String>>());

	public StompSubscriptionMessageProducer(String destination) {
		this.destination = destination;
		this.stompHandler = new DefaultStompHandler();
	}

	public StompSubscriptionMessageProducer(String destination, StompHandler stompHandler) {
		this.destination = destination;
		this.stompHandler = stompHandler;
	}

	public String getDestination() {
		return destination;
	}

	@Override
	protected final void onInit() {
		super.onInit();
		this.stompHandler.addDestination(this.destination, new StompSubscriptionCallback() {

			@Override
			public void subscribed(Object session, String id) {
				Assert.isInstanceOf(String.class, session, "Invalid session");
				Set<String> subs = subscriptions.get(session);
				if (subs == null) {
					subs = new HashSet<String>();
					subscriptions.put((String) session, subs);
				}
				subs.add(id);
				if (logger.isDebugEnabled()) {
					logger.debug("new subscription; session=" + session + ", id=" + id);
				}
			}

			@Override
			public void unsubscribed(Object session, String id) {
				Set<String> subs = subscriptions.get(session);
				if (subs != null) {
					if (id != null) {
						subs.remove(id);
						if (logger.isDebugEnabled()) {
							logger.debug("removed subscription; session=" + session + ", id=" + id);
						}
					}
					else {
						subs.clear();
						if (logger.isDebugEnabled()) {
							logger.debug("removed all subscriptions; session=" + session);
						}
					}
				}
			}
		});
		this.onOnInit();
	}

	protected void onOnInit() {
	}

	protected final Map<String, Set<String>> getSubscriptions() {
		return Collections.unmodifiableMap(this.subscriptions);
	}

}
