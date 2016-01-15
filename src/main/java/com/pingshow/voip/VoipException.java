
package com.pingshow.voip;

@SuppressWarnings("serial")
public class VoipException extends Exception {

	public VoipException() {
	}

	public VoipException(String detailMessage) {
		super(detailMessage);
	}

	public VoipException(Throwable throwable) {
		super(throwable);
	}

	public VoipException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}
}
