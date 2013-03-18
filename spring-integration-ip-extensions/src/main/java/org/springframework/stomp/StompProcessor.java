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

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public interface StompProcessor {

	void processSend(Object session, StompMessage message);

	void processTransaction(Object session, Collection<StompMessage> messages);

	/**
	 * Called when a connection subscribes to a service.
	 * @param session
	 */
	void subscribed(Object session, String id);

	/**
	 * Called when a connection unsubscribes from a service.
	 * @param session the session (e.g. connectionId)
	 * @param id The subscription id - if null unsubscribe all
	 */
	void unsubscribed(Object session, String id);
}
