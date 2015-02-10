package org.suggs.katas.jmsbroker;

public interface JmsBrokerService {

    void start() throws Exception;
    
    void stop() throws Exception;

	long getEnqueuedMessageCountAt(String aDestinationName) throws Exception;

}
