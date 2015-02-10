package org.suggs.katas.jmsbroker;


public class JmsBrokerServiceFactory {

	public JmsBrokerService createAt(String aBrokerUrl) throws Exception {
		return new ActiveMqBrokerService(aBrokerUrl);
	}

}
