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
package org.springframework.integration.x.netty;

import static org.junit.Assert.assertEquals;

import java.net.Socket;

import org.junit.Test;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.ip.tcp.serializer.ByteArrayCrLfSerializer;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.x.netty.inbound.HttpChannelInitializer;
import org.springframework.integration.x.netty.inbound.NettyRequestHandlingEndpoint;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public class NettyRequestHandlingEndpointTests {

	@Test
	public void testSimple() throws Exception {
		NettyRequestHandlingEndpoint endpoint = new NettyRequestHandlingEndpoint(9876);
		DirectChannel requestChannel = new DirectChannel();
		endpoint.setRequestChannel(requestChannel);
		final DirectChannel replyChannel = new DirectChannel();
		endpoint.setReplyChannel(replyChannel);
		endpoint.afterPropertiesSet();
		requestChannel.subscribe(new MessageHandler() {
			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				String payload = new String((byte[]) message.getPayload());
				replyChannel.send(MessageBuilder.withPayload(("echo:" + payload).getBytes())
						.copyHeaders(message.getHeaders()).build());
			}
		});
		endpoint.start();
		Thread.sleep(1000);

		Socket socket = new Socket("localhost", 9876);
		socket.setSoTimeout(5000);
		socket.getOutputStream().write("foo\r\n".getBytes());
		byte[] reply = new ByteArrayCrLfSerializer().deserialize(socket.getInputStream());
		assertEquals("echo:foo", new String(reply));

		socket.close();
		endpoint.stop();
	}

	@Test
	public void testHttp() throws Exception {
		NettyRequestHandlingEndpoint endpoint = new NettyRequestHandlingEndpoint(9876);
		HttpChannelInitializer initializer = new  HttpChannelInitializer(endpoint);
		DirectChannel requestChannel = new DirectChannel();
		endpoint.setRequestChannel(requestChannel);
		final DirectChannel replyChannel = new DirectChannel();
		endpoint.setReplyChannel(replyChannel);
		endpoint.afterPropertiesSet();
		requestChannel.subscribe(new MessageHandler() {
			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				String reply = "<html>FOO</html>\r\n";
				replyChannel.send(MessageBuilder.withPayload(reply)
						.copyHeaders(message.getHeaders()).build());
			}
		});
		endpoint.afterPropertiesSet();
		initializer.afterPropertiesSet();
		endpoint.start();
		Thread.sleep(1000);

		Socket socket = new Socket("localhost", 9876);
//		socket.setSoTimeout(5000);
		socket.getOutputStream().write("GET /foo?bar=baz HTTP/1.1\r\nfoo: bar\n\r\n".getBytes());
		byte[] reply = new ByteArrayCrLfSerializer().deserialize(socket.getInputStream());
		assertEquals("HTTP/1.1 200 OK", new String(reply));
		reply = new ByteArrayCrLfSerializer().deserialize(socket.getInputStream());
		assertEquals("foo: bar", new String(reply));
		reply = new ByteArrayCrLfSerializer().deserialize(socket.getInputStream());
		assertEquals("", new String(reply));
		reply = new ByteArrayCrLfSerializer().deserialize(socket.getInputStream());
		assertEquals("<html>FOO</html>", new String(reply));

		socket.close();
		endpoint.stop();
	}

}
