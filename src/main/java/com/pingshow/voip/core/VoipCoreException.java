
package com.pingshow.voip.core;

 
@SuppressWarnings("serial")
public class VoipCoreException extends Exception {

	public VoipCoreException() {
	}

	public VoipCoreException(String detailMessage) {
		super(detailMessage);
	}

	public VoipCoreException(Throwable e) {
		super(e);
	}

	public VoipCoreException(String detailMessage,Throwable e) {
		super(detailMessage,e);
	}
	

}
