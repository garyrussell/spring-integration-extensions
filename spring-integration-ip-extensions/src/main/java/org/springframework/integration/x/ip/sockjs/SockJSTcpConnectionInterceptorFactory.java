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
package org.springframework.integration.x.ip.sockjs;

import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import org.springframework.core.serializer.Deserializer;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.MessagingException;
import org.springframework.integration.ip.tcp.connection.TcpConnectionInterceptor;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.x.ip.http.AbstractHttpSwitchingDeserializer.BasicState;
import org.springframework.integration.x.ip.serializer.DataFrame;
import org.springframework.integration.x.ip.websocket.WebSocketFrame;
import org.springframework.integration.x.ip.websocket.WebSocketHeaders;
import org.springframework.integration.x.ip.websocket.WebSocketSerializer;
import org.springframework.integration.x.ip.websocket.WebSocketSerializer.WebSocketState;
import org.springframework.integration.x.ip.websocket.WebSocketTcpConnectionInterceptorFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public class SockJSTcpConnectionInterceptorFactory extends WebSocketTcpConnectionInterceptorFactory {

	private final Set<SockJSTcpConnectionInterceptor> openConnections =
			Collections.synchronizedSet(new HashSet<SockJSTcpConnectionInterceptor>());

	private volatile TaskScheduler taskScheduler;

	private volatile long heartbeatInterval = 25000;

	private final Runnable heartBeater = new Runnable() {

		private final WebSocketFrame heartbeat = new WebSocketFrame(WebSocketFrame.TYPE_DATA, "h");

		@Override
		public void run() {
			Iterator<SockJSTcpConnectionInterceptor> iterator = openConnections.iterator();
			while (iterator.hasNext()) {
				SockJSTcpConnectionInterceptor connection = iterator.next();
				try {
					if (logger.isDebugEnabled()) {
						logger.debug("Sending heartbeat to " + connection.getConnectionId());
					}
					connection.send(MessageBuilder.withPayload(this.heartbeat).build());
				}
				catch (Exception e) {
					logger.error("Failed to send heartbeat", e);
					connection.close();
				}
			}
			taskScheduler.schedule(heartBeater, new Date(System.currentTimeMillis() + heartbeatInterval));
		}
	};

	@Override
	public TcpConnectionInterceptor getInterceptor() {
		return new SockJSTcpConnectionInterceptor();
	}

	@Override
	protected void onInit() throws Exception {
		super.onInit();
		this.taskScheduler = this.getTaskScheduler();
		this.taskScheduler.schedule(this.heartBeater, new Date(System.currentTimeMillis() + this.heartbeatInterval));
	}


	protected class SockJSTcpConnectionInterceptor extends WebSocketTcpConnectionInterceptor {

		@Override
		protected boolean doOnMessage(Message<?> message) {
			Assert.isInstanceOf(WebSocketFrame.class, message.getPayload());
			@SuppressWarnings("unchecked")
			Message<WebSocketFrame> webSocketFrameMessage = (Message<WebSocketFrame>) message;
			WebSocketFrame payload = (WebSocketFrame) message.getPayload();
			InputStream inputStream = getInputStream(message);
			WebSocketState state = getState(inputStream);
			if (this.determineSimpleHttp(payload)) {
				this.handleHttp(webSocketFrameMessage, state);
				this.getRequiredDeserializer().removeState(inputStream);
				return false;
			}
			else {
				return super.doOnMessage(message);
			}
		}

		protected boolean determineSimpleHttp(WebSocketFrame payload) {
			return payload.getType() == DataFrame.TYPE_HEADERS &&
					!payload.getPayload().toLowerCase().contains("sec-websocket-key");
		}

		protected void handleHttp(Message<WebSocketFrame> message, BasicState state) {
			if (state.getPath().endsWith("/info")) {
				String info = handleInfo(message);
				Message<String> reply = MessageBuilder.withPayload(info)
						.copyHeaders(message.getHeaders())
						.build();
				try {
					this.send(reply);
				}
				catch (Exception e) {
					this.close();
					throw new MessagingException(reply, "Failed to send info handshake", e);
				}
			}
			else {
				MessageBuilder<?> messageBuilder = MessageBuilder.fromMessage(message);
				if (state.getPath() != null) {
					messageBuilder.setHeader(WebSocketHeaders.PATH, state.getPath());
				}
				if (state.getQueryString() != null) {
					messageBuilder.setHeader(WebSocketHeaders.QUERY_STRING, state.getQueryString());
				}
				this.getListener().onMessage(messageBuilder.build());
			}
		}

		/**
		 * Tell the client we support WebSockets.
		 */
		private String handleInfo(Message<WebSocketFrame> message) {
			String json = "{\"websocket\":true, \"cookie-needed\":false, \"origins\":[\"*:*\"], \"entropy\":" +
								Math.abs(new Random().nextLong()) + "}";
			return "HTTP/1.1 200 OK\r\ncontent-type: application/json; charset=UTF-8\r\n" +
					"Connection: keep-alive\r\n" +
					"Access-Control-Allow-Origin: null\r\n" +
					"Access-Control-Allow-Credentials: true\r\n" +
					"Cache-Control: no-store, no-cache, must-revalidate, max-age=0\r\n" +
					"content-length: " + json.length() + "\r\n\r\n" + json;
		}

		/**
		 * SockJS over WS requires an 'o' (open) frame to be sent by the server.
		 */
		@Override
		protected void doHandshake(WebSocketFrame frame, MessageHeaders messageHeaders) throws Exception {
			super.doHandshake(frame, messageHeaders);
			if (this.isOpen()) {
				this.send(MessageBuilder.withPayload(new WebSocketFrame(WebSocketFrame.TYPE_DATA, "o"))
						.copyHeaders(messageHeaders)
						.build());
				openConnections.add(this);
			}
		}

		@Override
		public void close() {
			openConnections.remove(this);
			super.close();
		}

		@Override
		protected WebSocketSerializer getRequiredDeserializer() {
			Deserializer<?> deserializer = this.getDeserializer();
			Assert.state(deserializer instanceof SockJSSerializer,
					"Deserializer must be a SockJSSerializer");
			return (WebSocketSerializer) deserializer;
		}

	}

}
