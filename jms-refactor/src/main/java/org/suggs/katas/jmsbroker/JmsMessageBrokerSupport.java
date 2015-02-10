package org.suggs.katas.jmsbroker;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.Broker;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.region.DestinationStatistics;
import org.slf4j.Logger;

import javax.jms.*;
import java.lang.IllegalStateException;

import static org.slf4j.LoggerFactory.getLogger;
import static org.suggs.katas.jmsbroker.SocketFinder.findNextAvailablePortBetween;

public class JmsMessageBrokerSupport {
    private static final Logger LOG = getLogger(JmsMessageBrokerSupport.class);
    private static final int ONE_SECOND = 1000;
    private static final int DEFAULT_RECEIVE_TIMEOUT = 10 * ONE_SECOND;
    public static final String DEFAULT_BROKER_URL_PREFIX = "tcp://localhost:";

    private String brokerUrl;
    private BrokerService brokerService;

    private JmsMessageBrokerSupport(String aBrokerUrl) {
        brokerUrl = aBrokerUrl;
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
        brokerService = new BrokerService();
        brokerService.setPersistent(false);
        brokerService.addConnector(brokerUrl);
    }

    public static JmsMessageBrokerSupport bindToBrokerAtUrl(String aBrokerUrl) throws Exception {
        return new JmsMessageBrokerSupport(aBrokerUrl);
    }

    private void startEmbeddedBroker() throws Exception {
        brokerService.start();
    }

    public void stopTheRunningBroker() throws Exception {
        if (brokerService == null) {
            throw new IllegalStateException("Cannot stop the broker from this API: " +
                    "perhaps it was started independently from this utility");
        }
        brokerService.stop();
        brokerService.waitUntilStopped();
    }

    public final JmsMessageBrokerSupport andThen() {
        return this;
    }

    public final String getBrokerUrl() {
        return brokerUrl;
    }

    public JmsMessageBrokerSupport sendATextMessageToDestinationAt(String aDestinationName, final String aMessageToSend) {
        executeCallbackAgainstRemoteBroker(brokerUrl, aDestinationName, (aSession, aDestination) -> {
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
        return executeCallbackAgainstRemoteBroker(brokerUrl, aDestinationName, (aSession, aDestination) -> {
            MessageConsumer consumer = aSession.createConsumer(aDestination);
            Message message = consumer.receive(aTimeout);
            if (message == null) {
                throw new NoMessageReceivedException(String.format("No messages received from the broker within the %d timeout", aTimeout));
            }
            consumer.close();
            return ((TextMessage) message).getText();
        });
    }

    private String executeCallbackAgainstRemoteBroker(String aBrokerUrl, String aDestinationName, JmsCallback aCallback) {
        Connection connection = null;
        String returnValue = "";
        try {
            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(aBrokerUrl);
            connection = connectionFactory.createConnection();
            connection.start();
            returnValue = executeCallbackAgainstConnection(connection, aDestinationName, aCallback);
        } catch (JMSException jmse) {
            LOG.error("failed to create connection to {}", aBrokerUrl);
            throw new IllegalStateException(jmse);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (JMSException jmse) {
                    LOG.warn("Failed to close connection to broker at []", aBrokerUrl);
                    throw new IllegalStateException(jmse);
                }
            }
        }
        return returnValue;
    }

    interface JmsCallback {
        String performJmsFunction(Session aSession, Destination aDestination) throws JMSException;
    }

    private String executeCallbackAgainstConnection(Connection aConnection, String aDestinationName, JmsCallback aCallback) {
        Session session = null;
        try {
            session = aConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue(aDestinationName);
            return aCallback.performJmsFunction(session, queue);
        } catch (JMSException jmse) {
            LOG.error("Failed to create session on connection {}", aConnection);
            throw new IllegalStateException(jmse);
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (JMSException jmse) {
                    LOG.warn("Failed to close session {}", session);
                    throw new IllegalStateException(jmse);
                }
            }
        }
    }

    public long getEnqueuedMessageCountAt(String aDestinationName) throws Exception {
        return getDestinationStatisticsFor(aDestinationName).getMessages().getCount();
    }

    public boolean isEmptyQueueAt(String aDestinationName) throws Exception {
        return getEnqueuedMessageCountAt(aDestinationName) == 0;
    }

    private DestinationStatistics getDestinationStatisticsFor(String aDestinationName) throws Exception {
        Broker regionBroker = brokerService.getRegionBroker();
        for (org.apache.activemq.broker.region.Destination destination : regionBroker.getDestinationMap().values()) {
            if (destination.getName().equals(aDestinationName)) {
                return destination.getDestinationStatistics();
            }
        }
        throw new IllegalStateException(String.format("Destination %s does not exist on broker at %s", aDestinationName, brokerUrl));
    }

    public class NoMessageReceivedException extends RuntimeException {
        public NoMessageReceivedException(String reason) {
            super(reason);
        }
    }
}
