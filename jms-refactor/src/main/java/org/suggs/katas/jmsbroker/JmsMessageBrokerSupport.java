package org.suggs.katas.jmsbroker;

import static org.slf4j.LoggerFactory.getLogger;
import static org.suggs.katas.jmsbroker.SocketFinder.findNextAvailablePortBetween;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.slf4j.Logger;

public class JmsMessageBrokerSupport {
	public static final String DEFAULT_BROKER_URL_PREFIX = "tcp://localhost:";

	private static final Logger LOG = getLogger(JmsMessageBrokerSupport.class);
	private static final int ONE_SECOND = 1000;
	private static final int DEFAULT_RECEIVE_TIMEOUT = 10 * ONE_SECOND;

	private String brokerUrl;
	private JmsBrokerService brokerService;

	private JmsConnectionSupport jmsConnectionSupport;
	private JmsBrokerServiceFactory jmsBrokerServiceFactory;

	private JmsMessageBrokerSupport(String aBrokerUrl) {
		this(aBrokerUrl, new JmsConnectionSupport(new JmsConnectionFactory()), new JmsBrokerServiceFactory());
	}

	protected JmsMessageBrokerSupport(String aBrokerUrl, JmsConnectionSupport aJmsConnectionSupport, JmsBrokerServiceFactory aJmsBrokerServiceFactory) {
		brokerUrl = aBrokerUrl;
		jmsConnectionSupport = aJmsConnectionSupport;
		jmsBrokerServiceFactory = aJmsBrokerServiceFactory;
	}

	public static JmsMessageBrokerSupport createARunningEmbeddedBrokerOnAvailablePort() throws Exception {
		return createARunningEmbeddedBrokerAt(DEFAULT_BROKER_URL_PREFIX + findNextAvailablePortBetween(41616, 50000));
	}

	public static JmsMessageBrokerSupport createARunningEmbeddedBrokerAt(String aBrokerUrl) throws Exception {
		LOG.debug("Creating a new broker at {}", aBrokerUrl);
		JmsMessageBrokerSupport broker = bindToBrokerAtUrl(aBrokerUrl);
		broker.createEmbeddedBroker();
		broker.startEmbeddedBroker();
		return broker;
	}

	private void createEmbeddedBroker() throws Exception {
		brokerService = jmsBrokerServiceFactory.createAt(brokerUrl);
	}

	public static JmsMessageBrokerSupport bindToBrokerAtUrl(String aBrokerUrl) throws Exception {
		return new JmsMessageBrokerSupport(aBrokerUrl);
	}

	private void startEmbeddedBroker() throws Exception {
		brokerService.start();
	}

	public void stopTheRunningBroker() throws Exception {
		if (brokerService == null) {
			throw new IllegalStateException("Cannot stop the broker from this API: " + "perhaps it was started independently from this utility");
		}
		brokerService.stop();
	}

	public final JmsMessageBrokerSupport andThen() {
		return this;
	}

	public final String getBrokerUrl() {
		return brokerUrl;
	}

	public JmsMessageBrokerSupport sendATextMessageToDestinationAt(String aDestinationName, final String aMessageToSend) {
		jmsConnectionSupport.executeCallbackAgainstRemoteBroker(brokerUrl, aDestinationName, (aSession, aDestination) -> {
			MessageProducer producer = aSession.createProducer(aDestination);
			producer.send(aSession.createTextMessage(aMessageToSend));
			producer.close();
			return "";
		});
		return this;
	}

	public String retrieveASingleMessageFromTheDestination(String aDestinationName) {
		return retrieveASingleMessageFromTheDestination(aDestinationName, DEFAULT_RECEIVE_TIMEOUT);
	}

	public String retrieveASingleMessageFromTheDestination(String aDestinationName, final int aTimeout) {
		return jmsConnectionSupport.executeCallbackAgainstRemoteBroker(brokerUrl, aDestinationName, (aSession, aDestination) -> {
			MessageConsumer consumer = aSession.createConsumer(aDestination);
			Message message = consumer.receive(aTimeout);
			if (message == null) {
				throw new NoMessageReceivedException(String.format("No messages received from the broker within the %d timeout", aTimeout));
			}
			consumer.close();
			return ((TextMessage) message).getText();
		});
	}

	public long getEnqueuedMessageCountAt(String aDestinationName) throws Exception {
		return brokerService.getEnqueuedMessageCountAt(aDestinationName);
	}

	public boolean isEmptyQueueAt(String aDestinationName) throws Exception {
		return brokerService.getEnqueuedMessageCountAt(aDestinationName) == 0;
	}

	public class NoMessageReceivedException extends RuntimeException {
		public NoMessageReceivedException(String reason) {
			super(reason);
		}
	}

	interface JmsCallback {
		String performJmsFunction(Session aSession, Destination aDestination) throws JMSException;
	}

}
