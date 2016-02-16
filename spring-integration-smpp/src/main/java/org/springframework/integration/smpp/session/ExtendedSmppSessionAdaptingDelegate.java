/* Copyright 2002-2016 the original author or authors.
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
package org.springframework.integration.smpp.session;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsmpp.InvalidResponseException;
import org.jsmpp.PDUException;
import org.jsmpp.bean.Address;
import org.jsmpp.bean.BindType;
import org.jsmpp.bean.DataCoding;
import org.jsmpp.bean.ESMClass;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.OptionalParameter;
import org.jsmpp.bean.RegisteredDelivery;
import org.jsmpp.bean.ReplaceIfPresentFlag;
import org.jsmpp.bean.SubmitMultiResult;
import org.jsmpp.bean.TypeOfNumber;
import org.jsmpp.extra.NegativeResponseException;
import org.jsmpp.extra.ResponseTimeoutException;
import org.jsmpp.extra.SessionState;
import org.jsmpp.session.BindParameter;
import org.jsmpp.session.ClientSession;
import org.jsmpp.session.DataSmResult;
import org.jsmpp.session.MessageReceiverListener;
import org.jsmpp.session.QuerySmResult;
import org.jsmpp.session.SMPPSession;
import org.jsmpp.session.SessionStateListener;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.Lifecycle;

/**
 * Adapts to the {@link ClientSession} API, while also providing the callbacks for the Spring container
 *
 * @author Josh Long
 * @author Gary Russell
 * @since 1.0
 */
public class ExtendedSmppSessionAdaptingDelegate implements /*Lifecycle,*/ ExtendedSmppSession, InitializingBean {

	/**
	 * callback for custom lifecycle events
	 */
	private final Lifecycle lifecycle;
	private final Log log = LogFactory.getLog(getClass());
	private final DelegatingMessageReceiverListener delegatingMessageReceiverListener = new DelegatingMessageReceiverListener();
	private volatile boolean running;
	private BindType bindType;
	private final SMPPSession session;

	public void setBindType(BindType bindType) {
		this.bindType = bindType;
	}

	public SMPPSession getTargetClientSession() {
		return this.session;
	}

	@Override
	public void start() {

		if( this.running) {
			return;
		}

		lifecycle.start();
		this.running = true;
	}

	@Override
	public void stop() {
		lifecycle.stop();
		this.running = false;
	}

	public boolean isRunning() {
		return this.running;
	}

	@Override
	public BindType getBindType() {
		return this.bindType;
	}

	/**
	 * noops for the {@link Lifecycle} arg in {@link ExtendedSmppSessionAdaptingDelegate#ExtendedSmppSessionAdaptingDelegate(org.jsmpp.session.SMPPSession, org.springframework.context.Lifecycle)}
	 *
	 * @param session the session
	 */
	public ExtendedSmppSessionAdaptingDelegate(SMPPSession session) {
		this(session, new Lifecycle() {
			@Override
			public void start() {
			}

			@Override
			public void stop() {
			}

			@Override
			public boolean isRunning() {
				return true;
			}
		});
	}

	public ExtendedSmppSessionAdaptingDelegate(SMPPSession session, Lifecycle lifecycle) {
		this.lifecycle = lifecycle;
		this.session = session;
		this.session.setMessageReceiverListener(this.delegatingMessageReceiverListener);
	}

    /**
     * Get message receiver listeners.
     * @return message listener that contains multiple listeners
     */
    public DelegatingMessageReceiverListener getDelegateMessageListener() {
        return delegatingMessageReceiverListener;
    }

	@Override
	public void addMessageReceiverListener(MessageReceiverListener messageReceiverListener) {
		this.delegatingMessageReceiverListener.addMessageReceiverListener(messageReceiverListener);
	}

	@Override
	public String submitShortMessage(String serviceType, TypeOfNumber sourceAddrTon, NumberingPlanIndicator sourceAddrNpi,
																	String sourceAddr, TypeOfNumber destAddrTon, NumberingPlanIndicator destAddrNpi,
																	String destinationAddr, ESMClass esmClass, byte protocolId, byte priorityFlag, String scheduleDeliveryTime, String validityPeriod, RegisteredDelivery registeredDelivery, byte replaceIfPresentFlag, DataCoding dataCoding, byte smDefaultMsgId, byte[] shortMessage, OptionalParameter... optionalParameters) throws PDUException, ResponseTimeoutException, InvalidResponseException, NegativeResponseException, IOException {
		return session.submitShortMessage(serviceType, sourceAddrTon, sourceAddrNpi, sourceAddr, destAddrTon, destAddrNpi, destinationAddr, esmClass, protocolId, priorityFlag, scheduleDeliveryTime, validityPeriod, registeredDelivery, replaceIfPresentFlag, dataCoding, smDefaultMsgId, shortMessage, optionalParameters);
	}

	@Override
	public SubmitMultiResult submitMultiple(String serviceType,
			TypeOfNumber sourceAddrTon,
			NumberingPlanIndicator sourceAddrNpi,
			String sourceAddr,
			Address[] destinationAddresses,
			ESMClass esmClass,
			byte protocolId,
			byte priorityFlag,
			String scheduleDeliveryTime,
			String validityPeriod,
			RegisteredDelivery registeredDelivery,
			ReplaceIfPresentFlag replaceIfPresentFlag,
			DataCoding dataCoding,
			byte smDefaultMsgId,
			byte[] shortMessage,
			OptionalParameter... optionalParameters) throws PDUException, ResponseTimeoutException,
			InvalidResponseException, NegativeResponseException, IOException {
		return session.submitMultiple(
				serviceType, sourceAddrTon, sourceAddrNpi, sourceAddr, destinationAddresses, esmClass, protocolId, priorityFlag, scheduleDeliveryTime, validityPeriod, registeredDelivery, replaceIfPresentFlag, dataCoding, smDefaultMsgId, shortMessage, optionalParameters
		);
	}

	@Override
	public QuerySmResult queryShortMessage(String messageId, TypeOfNumber sourceAddrTon, NumberingPlanIndicator sourceAddrNpi, String sourceAddr) throws PDUException, ResponseTimeoutException, InvalidResponseException, NegativeResponseException, IOException {
		return session.queryShortMessage(messageId, sourceAddrTon, sourceAddrNpi, sourceAddr);
	}

	@Override
	public void cancelShortMessage(String serviceType, String messageId, TypeOfNumber sourceAddrTon, NumberingPlanIndicator sourceAddrNpi, String sourceAddr,
																TypeOfNumber destAddrTon, NumberingPlanIndicator destAddrNpi, String destinationAddress) throws PDUException, ResponseTimeoutException, InvalidResponseException, NegativeResponseException, IOException {
		session.cancelShortMessage(serviceType, messageId, sourceAddrTon, sourceAddrNpi, sourceAddr, destAddrTon, destAddrNpi, destinationAddress);
	}

	@Override
	public void replaceShortMessage(String messageId, TypeOfNumber sourceAddrTon, NumberingPlanIndicator sourceAddrNpi, String sourceAddr, String scheduleDeliveryTime, String validityPeriod, RegisteredDelivery registeredDelivery, byte smDefaultMsgId, byte[] shortMessage) throws PDUException, ResponseTimeoutException, InvalidResponseException, NegativeResponseException, IOException {
		session.replaceShortMessage(messageId, sourceAddrTon, sourceAddrNpi, sourceAddr, scheduleDeliveryTime, validityPeriod, registeredDelivery, smDefaultMsgId, shortMessage);
	}

	@Override
	public DataSmResult dataShortMessage(String serviceType, TypeOfNumber sourceAddrTon, NumberingPlanIndicator sourceAddrNpi, String sourceAddr, TypeOfNumber destAddrTon, NumberingPlanIndicator destAddrNpi, String destinationAddr, ESMClass esmClass, RegisteredDelivery registeredDelivery, DataCoding dataCoding, OptionalParameter... optionalParameters) throws PDUException, ResponseTimeoutException, InvalidResponseException, NegativeResponseException, IOException {
		return session.dataShortMessage(serviceType, sourceAddrTon, sourceAddrNpi, sourceAddr, destAddrTon, destAddrNpi, destinationAddr, esmClass, registeredDelivery, dataCoding, optionalParameters);
	}

	@Override
	public String getSessionId() {
		return session.getSessionId();
	}

	@Override
	public void setEnquireLinkTimer(int enquireLinkTimer) {
		session.setEnquireLinkTimer(enquireLinkTimer);
	}

	@Override
	public int getEnquireLinkTimer() {
		return session.getEnquireLinkTimer();
	}

	@Override
	public void setTransactionTimer(long transactionTimer) {
		session.setTransactionTimer(transactionTimer);
	}

	@Override
	public long getTransactionTimer() {
		return session.getTransactionTimer();
	}

	@Override
	public SessionState getSessionState() {
		return session.getSessionState();
	}

	@Override
	public void addSessionStateListener(SessionStateListener l) {
		session.addSessionStateListener(l);
	}

	@Override
	public void removeSessionStateListener(SessionStateListener l) {
		session.removeSessionStateListener(l);
	}

	@Override
	public long getLastActivityTimestamp() {
		return session.getLastActivityTimestamp();
	}

	@Override
	public void close() {
		session.close();
	}

	@Override
	public void unbindAndClose() {
		session.unbindAndClose();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		log.debug( "afterPropertiesSet!");
	}

	@Override
	public void connectAndBind(String host, int port, BindType bindType, String systemId, String password,
			String systemType, TypeOfNumber addrTon, NumberingPlanIndicator addrNpi, String addressRange)
					throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void connectAndBind(String host, int port, BindType bindType, String systemId, String password,
			String systemType, TypeOfNumber addrTon, NumberingPlanIndicator addrNpi, String addressRange, long timeout)
					throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String connectAndBind(String host, int port, BindParameter bindParam) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String connectAndBind(String host, int port, BindParameter bindParam, long timeout) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public MessageReceiverListener getMessageReceiverListener() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setMessageReceiverListener(MessageReceiverListener messageReceiverListener) {
		throw new UnsupportedOperationException();
	}

}
