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

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public class StompProcessorSupport implements StompProcessor {

	protected final Log logger = LogFactory.getLog(this.getClass());

	@Override
	public void processSend(Object session, StompMessage message) {
		if (logger.isWarnEnabled()) {
			logger.warn("Processor doesn't handle send for " + message);
		}
	}

	@Override
	public void processTransaction(Object session, Collection<StompMessage> messages) {
		if (logger.isWarnEnabled()) {
			logger.warn("Processor doesn't handle transaction for " + messages);
		}
	}

	@Override
	public void subscribed(Object session, String id) {
		if (logger.isWarnEnabled()) {
			logger.warn("Processor doesn't handle subscriptions");
		}
	}

	@Override
	public void unsubscribed(Object session, String id) {
		if (logger.isWarnEnabled()) {
			logger.warn("Processor doesn't handle subscriptions");
		}
	}

}
