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
package org.springframework.integration.x.ip.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.http.HttpHeaders;
import org.springframework.integration.x.ip.serializer.ByteArrayCrLfSerializer;
import org.springframework.integration.x.ip.serializer.DataFrame;
import org.springframework.util.Assert;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public class HttpDeserializer implements Serializer<Object>, Deserializer<DataFrame> {

	protected final Log logger = LogFactory.getLog(this.getClass());

	private volatile int maxMessageSize = 2048;

	private volatile String charset = "UTF-8";

	private final ByteArrayCrLfSerializer crlfDeserializer = new ByteArrayCrLfSerializer();

	public void setMaxMessageSize(int maxMessageSize) {
		this.maxMessageSize = maxMessageSize;
		this.crlfDeserializer.setMaxMessageSize(maxMessageSize);
	}

	protected ByteArrayCrLfSerializer getCrlfDeserializer() {
		return crlfDeserializer;
	}

	protected void checkClosure(int bite) throws IOException {
		if (bite < 0) {
			logger.debug("Socket closed during message assembly");
			throw new IOException("Socket closed during message assembly");
		}
	}

	protected DataFrame createDataFrame(int type, String frameData) {
		return new DataFrame(type, frameData);
	}

	@Override
	public DataFrame deserialize(InputStream inputStream) throws IOException {
		DataFrame dataFrame = this.createDataFrame(DataFrame.TYPE_HEADERS, null);
		HttpHeaders headers = new HttpHeaders();
		byte[] header = new byte[this.maxMessageSize];
		int headerLength;
		do {
			headerLength = this.crlfDeserializer.fillToCrLf(inputStream, header);
			String headerString = new String(header, 0, headerLength, this.charset);
			if (headerString.startsWith("GET")) {
				headers.put("requestLine", Collections.singletonList(headerString));
			}
			else {
				String[] headerParts = headerString.split(":");
				if (headerParts.length == 2) {
					// TODO: split up multi-valued?
					headers.put(headerParts[0], Collections.singletonList(headerParts[1]));
				}
			}
		}
		while (headerLength > 0);
		dataFrame.setHttpHeaders(headers);
		return dataFrame;
		// TODO: When 3.0.M1 is available add mapper
	}

	@Override
	public void serialize(Object object, OutputStream outputStream) throws IOException {
		// TODO: handle other object types
		Assert.isTrue(object instanceof String);
		String string = (String) object;
		outputStream.write(string.getBytes(this.charset));
		outputStream.flush();
	}

}