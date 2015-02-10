package org.suggs.katas.jmsbroker;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;

public class JmsConnectionFactory {

	public ConnectionFactory createAt(String aBrokerUrl) {
		return new ActiveMQConnectionFactory(aBrokerUrl);
	}

}
