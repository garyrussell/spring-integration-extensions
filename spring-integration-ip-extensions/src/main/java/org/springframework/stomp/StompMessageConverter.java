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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
public class StompMessageConverter {

	public static final byte LF = 0x0a;

	private static final byte COLON = ':';

	/**
	 * @param bytes a complete STOMP message (without the trailing 0x00).
	 */
	public StompMessage toStompMessage(Object stomp) {
		Assert.state(stomp instanceof String || stomp instanceof byte[], "'stomp' must be String or byte[]");
		byte[] stompBytes = null;
		if (stomp instanceof String) {
			try {
				stompBytes = ((String) stomp).getBytes("UTF-8");
			}
			catch (UnsupportedEncodingException e) {
				throw new StompException(e);
			}
		}
		else {
			stompBytes = (byte[]) stomp;
		}
		int totalLength = stompBytes.length;
		if (stompBytes[totalLength-1] == 0) {
			totalLength--;
		}
		int payloadIndex = findPayloadStart(stompBytes);
		try {
			String headerString = new String(stompBytes, 0, payloadIndex, "UTF-8");
			Parser parser = new Parser(headerString);
			Map<String, String> headers = new HashMap<String, String>();
			// TODO: validate command and whether a payload is allowed
			Command command = Command.valueOf(parser.nextToken(LF).trim());
			Assert.notNull(command, "No command found");
			while (parser.hasNext()) {
				String header = parser.nextToken(COLON);
				if (header != null) {
					if (parser.hasNext()) {
						String value = parser.nextToken(LF);
						if (!headers.keySet().contains(header)) { // ::Repeated Header Entries
							headers.put(header, value);
						}
					}
					else {
						throw new StompException("Parse exception for " + headerString);
					}
				}
			}
			byte[] payload = new byte[totalLength - payloadIndex];
			System.arraycopy(stompBytes, payloadIndex, payload, 0, totalLength - payloadIndex);
			return new StompMessage(command, headers, payload);
		}
		catch (UnsupportedEncodingException e) {
			throw new StompException(e);
		}
	}

	public byte[] fromStompMessage(StompMessage message) {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		Map<String, String> headers = message.getHeaders();
		Command command = message.getCommand();
		try {
			outputStream.write(command.toString().getBytes("UTF-8"));
			outputStream.write(LF);
			for (Entry<String, String> entry : headers.entrySet()) {
				String key = entry.getKey();
				key = key.replaceAll("\\\\", "\\\\").replaceAll(":", "\\\\c").replaceAll("\n", "\\\\n");
				outputStream.write(key.getBytes("UTF-8"));
				outputStream.write(COLON);
				String value = entry.getValue();
				value = value.replaceAll("\\\\", "\\\\").replaceAll(":", "\\\\c").replaceAll("\n", "\\\\n");
				outputStream.write(value.getBytes("UTF-8"));
				outputStream.write(LF);
			}
			outputStream.write(LF);
			outputStream.write(message.getPayload());
			return outputStream.toByteArray();
		}
		catch (IOException e) {
			throw new StompException(e);
		}
	}

	private int findPayloadStart(byte[] bytes) {
		int i;
		// ignore any leading \n from the previous message
		for (i = 0; i < bytes.length; i++) {
			if (bytes[i] != '\n') {
				break;
			}
			bytes[i] = ' ';
		}
		for (; i < bytes.length - 1; i++) {
			if (bytes[i] == LF && bytes[i+1] == LF) {
				break;
			}
		}
		if (i >= bytes.length) {
			throw new StompException("No end of headers found");
		}
		return i + 2;
	}

	private class Parser {

		private final String content;

		private int offset;

		public Parser(String content) {
			this.content = content;
		}

		public boolean hasNext() {
			return this.offset < this.content.length();
		}

		public String nextToken(byte delimiter) {
			if (this.offset >= this.content.length()) {
				return null;
			}
			int delimAt = this.content.indexOf(delimiter, this.offset);
			if (delimAt == -1) {
				if (this.offset == this.content.length() - 1 && delimiter == COLON &&
						this.content.charAt(this.offset) == LF) {
					this.offset++;
					return null;
				}
				else {
					throw new StompException("No delimiter found at offset " + offset + " in " + this.content);
				}
			}
			int escapeAt = this.content.indexOf('\\', this.offset);
			String token = this.content.substring(this.offset, delimAt + 1);
			this.offset += token.length();
			if (escapeAt >= 0 && escapeAt < delimAt) {
				char escaped = this.content.charAt(escapeAt + 1);
				if (escaped == 'n' || escaped == 'c' || escaped == '\\') {
					token = token.replaceAll("\\\\n", "\n")
							.replaceAll("\\\\c", ":")
							.replaceAll("\\\\\\\\", "\\\\");
				}
				else {
					throw new StompException("Invalid escape sequence \\" + escaped);
				}
			}
			return token.substring(0, token.length() - 1);
		}
	}
}
