package org.suggs.katas.jmsbroker;

import org.apache.activemq.broker.Broker;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.region.DestinationStatistics;

public class ActiveMqBrokerService implements JmsBrokerService{

	private BrokerService brokerService;
	private String brokerUrl;

	public ActiveMqBrokerService(String aBrokerUrl) throws Exception {
		brokerUrl = aBrokerUrl;
		brokerService = new BrokerService();
		brokerService.setPersistent(false);
		brokerService.addConnector(aBrokerUrl);
	}

	@Override
	public void start() throws Exception {
		brokerService.start();
	}

	@Override
	public void stop() throws Exception {
		brokerService.stop();
        brokerService.waitUntilStopped();
	}

    @Override
	public long getEnqueuedMessageCountAt(String aDestinationName) throws Exception {
        return getDestinationStatisticsFor(aDestinationName).getMessages().getCount();
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

}
