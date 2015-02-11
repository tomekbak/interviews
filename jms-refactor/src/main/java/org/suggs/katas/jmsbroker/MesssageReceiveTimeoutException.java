package org.suggs.katas.jmsbroker;

public class MesssageReceiveTimeoutException extends RuntimeException {
	public MesssageReceiveTimeoutException(String reason) {
		super(reason);
	}
}
