package org.suggs.katas.jmsbroker;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

public class JmsMessageSupport {

	private Session session;
	private Destination destination;

	public JmsMessageSupport(Session aSession, Destination aDestination) {
		this.session = aSession;
		this.destination = aDestination;
	}

	public void send(String aMessage) throws JMSException {
		MessageProducer producer = session.createProducer(destination);
		producer.send(session.createTextMessage(aMessage));
		producer.close();
	}

	public String receive(int aTimeout) throws JMSException {
		MessageConsumer consumer = session.createConsumer(destination);
		Message message = consumer.receive(aTimeout);
		if (message == null) {
			throw new MesssageReceiveTimeoutException(String.format("No messages received from the broker within the %d timeout", aTimeout));
		}
		consumer.close();
		return ((TextMessage) message).getText();
	}

}
