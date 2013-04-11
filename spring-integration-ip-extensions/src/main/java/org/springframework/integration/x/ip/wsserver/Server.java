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
package org.springframework.integration.x.ip.wsserver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.ip.tcp.connection.TcpConnectionEvent;
import org.springframework.integration.ip.tcp.connection.TcpConnectionEvent.TcpConnectionEventType;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.x.ip.wsserver.ProtocolHandler.DisconnectCallback;
import org.springframework.integration.x.stomp.StompProtocolHandler;
import org.springframework.util.Assert;

/**
 * Generic Server to handle publish/subscribe scenarios by delegating
 * subscriptions to child contexts. Also provides a Telnet server
 * for management.
 * @author Gary Russell
 * @since 3.0
 *
 */
public class Server implements ApplicationListener<TcpConnectionEvent>,
		InitializingBean, DisconnectCallback {

	protected final Log logger = LogFactory.getLog(this.getClass());

	private static enum Command {
		add, rm, list, subs, quit
	}

	private static CountDownLatch shutdownLatch = new CountDownLatch(1);

	private static final String WELCOME = "Welcome to the Spring Integration WebSocket Server\n" +
			"Available commands:\r\n" +
			" list (list all registered services)\r\n" +
			" add <service> <context> (add a new service)\r\n" +
			" rm <service> (remove a service)\r\n" +
			" subs (list current subscriptions)\r\n" +
			" quit (shut down the server)\r\n";

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private volatile MessageChannel toTelnet;

	private final Map<String, AbstractApplicationContext> services = new HashMap<String, AbstractApplicationContext>();

	private final Map<Object, Map<String, String>> subscriptionToDestination = new ConcurrentHashMap<Object, Map<String,String>>();

	private volatile TaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();

	private volatile ProtocolHandler[] protocolHandlers;

	private final Set<String> telnetClients = new HashSet<String>();

	@Override
	public void afterPropertiesSet() throws Exception {
		this.initDefaultStrategies();
	}

	private void initDefaultStrategies() {
		if (this.protocolHandlers == null) {
			this.protocolHandlers = new ProtocolHandler[] {new StompProtocolHandler()};
		}
		for (ProtocolHandler handler : this.protocolHandlers) {
			handler.registerCallback(this);
		}
	}

	public Object handleWs(Message<?> message) {
		MessageHeaders headers = message.getHeaders();
		Object isProtocol = headers.get("ws_isProtocol");
		Object isSubscription = headers.get("ws_isSubscription");
		if (isProtocol != null && isProtocol == Boolean.TRUE) {
			return handleProtocol(message);
		}
		else if (isSubscription != null && isSubscription == Boolean.TRUE) {
			String connectionId = (String) headers.get(IpHeaders.CONNECTION_ID);
			String command = (String) headers.get("ws_command");
			Assert.notNull(command, "command cannot be null");
			String id = (String) headers.get("ws_id");
			Assert.notNull(id, "id cannot be null");
			String destination = null;
			if (command.equals("SUBSCRIBE")) {
				destination = (String) headers.get("ws_destination");
				Assert.notNull(destination, "destination cannot be null");
				Map<String, String> subs = this.subscriptionToDestination.get(connectionId);
				if (subs == null) {
					subs = new HashMap<String, String>();
					this.subscriptionToDestination.put(connectionId, subs);
				}
				subs.put(id, destination);
			}
			else if (command.equals("UNSUBSCRIBE")) {
				Map<String, String> subs = this.subscriptionToDestination.get(connectionId);
				if (subs != null) {
					destination = subs.remove(id);
				}
			}
			if (destination != null) {
				ApplicationContext context = this.services.get(destination);
				if (context == null) {
					logger.error("No destination '" + destination + "'");
					// TODO send ERROR
				}
				else {
					MessageChannel subscribeChannel = context.getBean("subscriptions", MessageChannel.class);
					if (subscribeChannel != null) {
						subscribeChannel.send(message);
					}
					else {
						logger.error("No 'subscriptions' channel for destination '" + destination + "'");
					}
				}
			}
			return null;
		}
		return null;
	}

	private Object handleProtocol(Message<?> message) {
		for (ProtocolHandler handler : this.protocolHandlers) {
			if (handler.canHandle(message)) {
				return handler.handleProtocolMessage(message);
			}
		}
		return null;
	}

	public String handleTelnet(String request) {
		String trimmedRequest = request.trim();
		if (Command.quit.toString().equals(trimmedRequest)) {
			shutdownLatch.countDown();
			return "bye...";
		}
		else if (trimmedRequest.startsWith(Command.add.toString())) {
			return addService(trimmedRequest);
		}
		else if (trimmedRequest.startsWith(Command.rm.toString())) {
			return removeService(trimmedRequest);
		}
		else if (trimmedRequest.equals(Command.list.toString())) {
			return listServices();
		}
		else if (trimmedRequest.equals(Command.subs.toString())) {
			return listSubs();
		}
		return null;
	}

	private String listSubs() {
		StringBuilder builder = new StringBuilder();
		builder.append("Current subscriptions:\r\n");
		for (Entry<Object, Map<String, String>> entry : this.subscriptionToDestination.entrySet()) {
			builder.append(" ").append(entry.getKey().toString() + "\r\n");
			for (Entry<String, String> sub : entry.getValue().entrySet()) {
				builder.append("  subId:" + sub.getKey() + " dest:" + sub.getValue() + "\r\n");
			}
		}
		return builder.toString();
	}

	private String listServices() {
		StringBuilder builder = new StringBuilder();
		builder.append("Current services:\r\n");
		for (Entry<String, AbstractApplicationContext> entry : this.services.entrySet()) {
			builder.append(" ").append(entry.getKey());
		}
		return builder.toString();
	}

	private String removeService(String trimmedRequest) {
		String[] parts = trimmedRequest.split("\\s");
		if (parts.length != 2) {
			return "Invalid request: " + trimmedRequest;
		}
		String serviceName = parts[1].trim();
		AbstractApplicationContext context = this.services.remove(serviceName);
		if (context != null) {
			context.destroy();
			clearSubscriptionsForService(serviceName);
		}
		return context + " delinked and destroyed as " + serviceName;
	}

	private String addService(String trimmedRequest) {
		String[] parts = trimmedRequest.split("\\s");
		if (parts.length != 3) {
			return "Invalid request: " + trimmedRequest;
		}
		String serviceName = parts[1].trim();
		String context = parts[2].trim();
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(this.applicationContext);
		ctx.setConfigLocation(context);
		ctx.setId(serviceName);
		ctx.refresh();
		this.services.put(serviceName, ctx);
		return context + " linked and refreshed as " + serviceName;
	}

	private void clearSubscriptionsForService(String serviceName) {
		for (Entry<Object, Map<String, String>> entry : this.subscriptionToDestination.entrySet()) {
			Iterator<Entry<String, String>> iterator = entry.getValue().entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<String, String> idToDest = iterator.next();
				if (idToDest.getValue().equals(serviceName)) {
					iterator.remove();
				}
			}
		}
	}

	@Override
	public void onApplicationEvent(final TcpConnectionEvent event) {
		if (event.getType() == TcpConnectionEventType.OPEN) {
			if ("telnet".equals(event.getConnectionFactoryName())) {
				newTelnetClient(event);
			}
			else {
				connectionAlert(event);
			}
		}
		else if (event.getType() == TcpConnectionEventType.CLOSE) {
			if ("telnet".equals(event.getConnectionFactoryName())) {
				this.telnetClients.remove(event.getConnectionId());
			}
			else {
				// TODO: on Close - force unsubscribe all subs
				this.subscriptionToDestination.remove(event.getConnectionId());
				connectionAlert(event);
			}
		}
	}

	private void connectionAlert(final TcpConnectionEvent event) {
		String type = event.getType() == TcpConnectionEventType.OPEN ?"New connection" :
			event.getType() == TcpConnectionEventType.CLOSE ? "Closed connection" :
				event.getType().toString();
		for (String client : this.telnetClients) {
			try {
				toTelnet.send(MessageBuilder.withPayload(type +
						": " +
						event.getConnectionId() +
						" on factory " + event.getConnectionFactoryName())
						.setHeader(IpHeaders.CONNECTION_ID, client)
						.build());
			}
			catch (Exception e) {
				e.printStackTrace();
				telnetClients.remove(client);
			}
		}
	}

	private void newTelnetClient(final TcpConnectionEvent event) {
		taskExecutor.execute(new Runnable() { // TODO fix TcpConnection to register sender before firing event
			public void run() {
				try {
					Thread.sleep(1000);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
				toTelnet.send(MessageBuilder.withPayload(WELCOME)
						.setHeader(IpHeaders.CONNECTION_ID, event.getConnectionId())
						.build());
				telnetClients.add(event.getConnectionId());
			}
		});
	}

	@Override
	public void removeSubscriptions(Message<?> message) {
		String connectionId = (String) message.getHeaders().get(IpHeaders.CONNECTION_ID);
		Map<String, String> subs = this.subscriptionToDestination.get(connectionId);
		subs.remove(connectionId);
	}

	//////////////////////////////////////////////////////////\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\

	public static void main(String[] args) throws Exception {
		AbstractApplicationContext ctx = new ClassPathXmlApplicationContext("/META-INF/spring/wsServer-context.xml");
		shutdownLatch.await();
		ctx.destroy();
		System.exit(0);
	}

}
