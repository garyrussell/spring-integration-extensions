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

import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import javax.net.SocketFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.integration.ip.tcp.serializer.ByteArraySingleTerminatorSerializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class StompOverTcpTests {

	@Test
	public void test() throws Exception {
		Socket socket = SocketFactory.getDefault().createSocket("localhost", 9876);
		socket.setSoTimeout(10000);
		OutputStream outputStream = socket.getOutputStream();
		ByteArraySingleTerminatorSerializer serializer = new ByteArraySingleTerminatorSerializer((byte) 0);
		String connect = "CONNECT\naccept-version:1.1\nhost:stomp.github.org\n\n";
		serializer.serialize(connect.getBytes(), outputStream);
		InputStream inputStream = socket.getInputStream();
		String connected = new String(serializer.deserialize(inputStream));
		assertTrue(connected.startsWith("CONNECTED"));
		String subscribe = "SUBSCRIBE\nid:0\ndestination:fooDest\n\n";
		serializer.serialize(subscribe.getBytes(), outputStream);
		String message = new String(serializer.deserialize(inputStream));
		assertTrue(message.startsWith("MESSAGE"));
		assertTrue(message.endsWith("Hello, world!"));
		serializer.serialize(subscribe.getBytes(), outputStream);

		// get next message
		message = new String(serializer.deserialize(inputStream));
		assertTrue(message.startsWith("MESSAGE"));
		assertTrue(message.endsWith("Hello, world!"));

		String unsubscribe = "UNSUBSCRIBE\nid:0\ndestination:fooDest\n\n";
		serializer.serialize(unsubscribe.getBytes(), outputStream);
		socket.close();
	}
}
