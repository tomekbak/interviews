package org.suggs.katas.jmsbroker;

import static org.slf4j.LoggerFactory.getLogger;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Session;

import org.slf4j.Logger;
import org.suggs.katas.jmsbroker.JmsMessageBrokerSupport.JmsCallback;

public class JmsConnectionSupport {
	private static final Logger LOG = getLogger(JmsConnectionSupport.class);
	
	private JmsConnectionFactory jmsConnectionFactory;

	public JmsConnectionSupport(JmsConnectionFactory aJmsConnectionFactory) {
		this.jmsConnectionFactory = aJmsConnectionFactory;
	}

	public String executeCallbackAgainstRemoteBroker(String aBrokerUrl, String aDestinationName, JmsCallback aCallback) {
		Connection connection = null;
		String returnValue = "";
		try {
			ConnectionFactory connectionFactory = jmsConnectionFactory.createAt(aBrokerUrl);
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

}
