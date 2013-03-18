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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stomp.StompMessage.Command;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public class DefaultStompHandler implements StompHandler {

	private static final byte[] EMPTY_PAYLOAD = new byte[0];

	private final Map<String, StompProcessor> services = new ConcurrentHashMap<String, StompProcessor>();

	private final Map<Object, Map<String, String>> subscriptionToDestination = new ConcurrentHashMap<Object, Map<String,String>>();

	private final Map<Object, Map<String, Collection<StompMessage>>> transactions = new ConcurrentHashMap<Object, Map<String,Collection<StompMessage>>>();

	private volatile TransactionTemplate transactionTemplate;

	public void setTransactionTemplate(TransactionTemplate transactionTemplate) {
		this.transactionTemplate = transactionTemplate;
	}

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
		else if (Command.BEGIN.equals(command)) {
			return this.begin(requestMessage, session);
		}
		else if (Command.SEND.equals(command)) {
			return this.send(requestMessage, session);
		}
		else if (Command.COMMIT.equals(command)) {
			return this.commit(requestMessage, session);
		}
		else if (Command.ABORT.equals(command)) {
			return this.abort(requestMessage, session);
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
			StompProcessor callback = getService(subscribeMessage);
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
			StompProcessor callback = this.services.get(destination);
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
		for (Entry<String, StompProcessor> entry : this.services.entrySet()) {
			entry.getValue().unsubscribed(session, null);
		}
		this.subscriptionToDestination.remove(session);
		this.transactions.remove(session);
		// TODO: RECEIPT
		return null;
	}

	protected StompMessage begin(StompMessage beginMessage, Object session) {
		// TODO: ensure connected
		try {
			Map<String, Collection<StompMessage>> openTx = this.transactions.get(session);
			if (openTx == null) {
				openTx = new HashMap<String, Collection<StompMessage>>();
				this.transactions.put(session, openTx);
			}
			String tx = beginMessage.getHeaders().get("transaction");
			Assert.notNull(tx, "transaction header required");
			Collection<StompMessage> messages = openTx.get(tx);
			Assert.isNull(messages, "transaction already exists");
			messages = new ArrayList<StompMessage>();
			openTx.put(tx, messages);
			return null;
		}
		catch (Exception e) {
			e.printStackTrace();
			// TODO: return an ERROR message
			return null;
		}
	}

	protected StompMessage send(StompMessage message, Object session) {
		// TODO: ensure connected
		try {
			String tx = message.getHeaders().get("transaction");
			if (tx != null) {
				Map<String, Collection<StompMessage>> openTx = this.transactions.get(session);
				if (openTx == null) {
					throw new IllegalStateException("No transaction '" + tx + "'");
				}
				Collection<StompMessage> messages = openTx.get(tx);
				if (messages == null) {
					throw new IllegalStateException("No transaction '" + tx + "'");
				}
				messages.add(message);
			}
			else {
				getService(message).processSend(session, message);
			}
			return null;
		}
		catch (Exception e) {
			e.printStackTrace();
			// TODO: return an ERROR message
			return null;
		}
	}

	protected StompMessage commit(StompMessage commitMessage, final Object session) {
		// TODO: ensure connected
		try {
			String tx = commitMessage.getHeaders().get("transaction");
			Map<String, Collection<StompMessage>> openTx = this.transactions.get(session);
			if (openTx == null) {
				throw new IllegalStateException("No transaction '" + tx + "'");
			}
			final Collection<StompMessage> messages = openTx.remove(tx);
			if (messages == null) {
				throw new IllegalStateException("No transaction '" + tx + "'");
			}
			if (this.transactionTemplate != null) {
				transactionTemplate.execute(new TransactionCallback<StompMessage>() {
					@Override
					public StompMessage doInTransaction(TransactionStatus status) {
						return doCommit(messages, session);
					}
				});
			}
			else {
				return doCommit(messages, session);
			}
			return null;
		}
		catch (Exception e) {
			e.printStackTrace();
			// TODO: return an ERROR message
			return null;
		}
	}

	protected StompMessage abort(StompMessage commitMessage, Object session) {
		// TODO: ensure connected
		try {
			String tx = commitMessage.getHeaders().get("transaction");
			Map<String, Collection<StompMessage>> openTx = this.transactions.get(session);
			if (openTx == null) {
				throw new IllegalStateException("No transaction '" + tx + "'");
			}
			final Collection<StompMessage> messages = openTx.remove(tx);
			if (messages == null) {
				throw new IllegalStateException("No transaction '" + tx + "'");
			}
			return null;
		}
		catch (Exception e) {
			e.printStackTrace();
			// TODO: return an ERROR message
			return null;
		}
	}

	private StompMessage doCommit(Collection<StompMessage> messages, Object session) {
		Set<String> destinations = new HashSet<String>();
		for (StompMessage message : messages) {
			destinations.add(message.getHeaders().get("destination"));
		}
		if (destinations.size() == 1) {
			StompProcessor stompProcessor = this.services.get(destinations.iterator().next());
			Assert.notNull(stompProcessor, "No processor for commit");
			stompProcessor.processTransaction(session, messages);
		}
		else {
			// TODO: handle tx across different destinations
			// Iterate over list and process individually (so they are in order)
			// or break up into several collections and process each collection
			throw new UnsupportedOperationException("Currently no support for tx across destinations");
		}
		return null;
	}

	private String getId(StompMessage message) {
		String id = message.getHeaders().get("id");
		Assert.notNull(id, "'id' header must not be null");
		return id;
	}

	private StompProcessor getService(StompMessage message) {
		String destination = message.getHeaders().get("destination");
		Assert.notNull(destination, "'destination' header must not be null");
		StompProcessor callback = this.services.get(destination);
		Assert.state(callback != null, "No service for destination '" + destination + "'");
		return callback;
	}

	@Override
	public void addDestination(String destination, StompProcessor callback) {
		Assert.notNull(destination, "'destination' cannot be null");
		Assert.notNull(callback, "'callback' cannot be null");
		this.services.put(destination, callback);
	}

	@Override
	public void removeDestination(String destination) {
		this.services.remove(destination);
	}

}
