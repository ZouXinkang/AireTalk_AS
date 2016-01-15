
package com.pingshow.voip;

@SuppressWarnings("serial")
public class VoipConfigException extends VoipException {

	public VoipConfigException() {
		super();
	}

	public VoipConfigException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	public VoipConfigException(String detailMessage) {
		super(detailMessage);
	}

	public VoipConfigException(Throwable throwable) {
		super(throwable);
	}

}
