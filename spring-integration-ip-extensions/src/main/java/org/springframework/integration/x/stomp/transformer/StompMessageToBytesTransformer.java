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
package org.springframework.integration.x.stomp.transformer;

import org.springframework.integration.Message;
import org.springframework.integration.transformer.AbstractTransformer;
import org.springframework.stomp.StompMessage;
import org.springframework.stomp.StompMessageConverter;
import org.springframework.util.Assert;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public class StompMessageToBytesTransformer extends AbstractTransformer {

	private volatile StompMessageConverter converter = new StompMessageConverter();

	public void setConverter(StompMessageConverter converter) {
		Assert.notNull(converter, "'converter' cannot be null");
		this.converter = converter;
	}

	@Override
	protected Object doTransform(Message<?> message) throws Exception {
		Assert.isInstanceOf(StompMessage.class, message.getPayload(), "Invalid Payload");
		// TODO - map from SI message to StompMessage
		return this.converter.fromStompMessage((StompMessage) message.getPayload());
	}

}
