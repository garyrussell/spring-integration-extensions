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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.stomp.StompMessage;
import org.springframework.stomp.StompMessageConverter;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public class BytesToStompConverterTests {

	@Test
	public void testConnect() throws Exception {
		StompMessageConverter converter = new StompMessageConverter();
		String accept = "accept-version:1.1\n";
		String host = "host:stomp.github.org\n";
		String test = "\n\n\nCONNECT\n" + // test skipping of leading \n from previous msg
				accept +
				host +
				"\n";
		StompMessage message = converter.toStompMessage(test.getBytes("UTF-8"));
		assertEquals(3, message.getHeaders().size());
		assertEquals("CONNECT", message.getHeaders().get(StompMessage.COMMAND_KEY));
		assertEquals("1.1", message.getHeaders().get("accept-version"));
		assertEquals("stomp.github.org", message.getHeaders().get("host"));
		assertEquals(0, message.getPayload().length);

		String convertedBack = new String(converter.fromStompMessage(message), "UTF-8");
		assertEquals(test.substring(3).length(), convertedBack.length());
		assertTrue(convertedBack.contains(accept));
		assertTrue(convertedBack.contains(host));
	}

	@Test
	public void testConnectWithEscapes() throws Exception {
		StompMessageConverter converter = new StompMessageConverter();
		String accept = "accept-version:1.1\n";
		String host = "ho\\c\\nst:st\\nomp.gi\\cthu\\b.org\n";
		String test = "\n\n\nCONNECT\n" + // test skipping of leading \n from previous msg
				accept +
				host +
				"\n";
		StompMessage message = converter.toStompMessage(test.getBytes("UTF-8"));
		assertEquals(3, message.getHeaders().size());
		assertEquals("CONNECT", message.getHeaders().get(StompMessage.COMMAND_KEY));
		assertEquals("1.1", message.getHeaders().get("accept-version"));
		assertEquals("st\nomp.gi:thu\\b.org", message.getHeaders().get("ho:\nst"));
		assertEquals(0, message.getPayload().length);

		String convertedBack = new String(converter.fromStompMessage(message), "UTF-8");
		assertEquals(test.substring(3).length(), convertedBack.length());
		assertTrue(convertedBack.contains(accept));
		assertTrue(convertedBack.contains(host));
	}

}
