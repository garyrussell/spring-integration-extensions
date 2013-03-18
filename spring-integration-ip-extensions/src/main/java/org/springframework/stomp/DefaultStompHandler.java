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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.stomp.StompMessage.Command;
import org.springframework.util.Assert;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public class DefaultStompHandler implements StompHandler {

	private static final byte[] EMPTY_PAYLOAD = new byte[0];

	private final Map<String, StompSubscriptionCallback> services = new HashMap<String, StompSubscriptionCallback>();

	private final Map<Object, Map<String, String>> subscriptionToDestination = new HashMap<Object, Map<String,String>>();

	@Override
	public StompMessage handleStompMessage(StompMessage requestMessage, Object session) {
		Command command = requestMessage.getCommand();
		Assert.notNull(command, "Command header cannot be null");
		if (Command.CONNECT.equals(command)) {
			return this.connect(requestMessage, session);
		}
		else if (Command.SUBSCRIBE.equals(command)) {
			return this.subscribe(requestMessage, session);
		}
		else if (Command.UNSUBSCRIBE.equals(command)) {
			return this.unsubscribe(requestMessage, session);
		}
		else if (Command.DISCONNECT.equals(command)) {
			return this.disconnect(session);
		}
		else {
			System.out.println(requestMessage); // TODO
		}
		return null;
	}

	protected StompMessage connect(StompMessage connectMessage, Object session) {
		// TODO: check supported versions
		// TODO: security
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("version", "1.1");
		headers.put("heart-beat", "0,0"); // TODO: enable heart-beats
		if (session != null) {
			headers.put("session", session.toString());
		}
		return new StompMessage(Command.CONNECTED, headers, EMPTY_PAYLOAD);
	}

	protected StompMessage subscribe(StompMessage subscribeMessage, Object session) {
		// TODO: ensure connected
		try {
			StompSubscriptionCallback callback = getService(subscribeMessage);
			String id = getId(subscribeMessage);
			callback.subscribed(session, id);
			Map<String, String> subs = this.subscriptionToDestination.get(session);
			if (subs == null) {
				subs = new HashMap<String, String>();
				this.subscriptionToDestination.put(session, subs);
			}
			subs.put(id, subscribeMessage.getHeaders().get("destination"));
			return null;
		}
		catch (Exception e) {
			e.printStackTrace();
			// TODO: return an ERROR message
			return null;
		}
	}

	protected StompMessage unsubscribe(StompMessage unsubscribeMessage, Object session) {
		// TODO: ensure connected
		try {
			String id = getId(unsubscribeMessage);
			Assert.notNull(id, "No id header found");
			Map<String, String> idToDest = this.subscriptionToDestination.get(session);
			if (idToDest == null) {
				throw new IllegalStateException("No subscription for '" + id + "' found");
			}
			String destination = idToDest.remove(id);
			if (destination == null) {
				throw new IllegalStateException("No subscription for '" + id + "' found");
			}
			StompSubscriptionCallback callback = this.services.get(destination);
			callback.unsubscribed(session, id);
			return null;
		}
		catch (Exception e) {
			e.printStackTrace();
			// TODO: return an ERROR message
			return null;
		}
	}

	protected StompMessage disconnect(Object session) {
		for (Entry<String, StompSubscriptionCallback> entry : this.services.entrySet()) {
			entry.getValue().unsubscribed(session, null);
		}
		this.subscriptionToDestination.remove(session);
		// TODO: RECEIPT
		return null;
	}

	private String getId(StompMessage message) {
		String id = message.getHeaders().get("id");
		Assert.notNull(id, "'id' header must not be null");
		return id;
	}

	private StompSubscriptionCallback getService(StompMessage message) {
		String destination = message.getHeaders().get("destination");
		Assert.notNull(destination, "'destination' header must not be null");
		StompSubscriptionCallback callback = this.services.get(destination);
		Assert.state(callback != null, "No service for destination '" + destination + "'");
		return callback;
	}

	@Override
	public void addDestination(String destination, StompSubscriptionCallback callback) {
		Assert.notNull(destination, "'destination' cannot be null");
		Assert.notNull(callback, "'callback' cannot be null");
		this.services.put(destination, callback);
	}

	@Override
	public void removeDestination(String destination) {
		this.services.remove(destination);
	}

}
