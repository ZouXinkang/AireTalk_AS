
package com.pingshow.voip.core;

import java.util.Vector;

import com.pingshow.voip.core.VoipCallParams;

@SuppressWarnings("unchecked")
public interface VoipCall {
	static class State {
		static private Vector values = new Vector();
		private final int mValue;
		private final String mStringValue;
		public final static State Idle = new State(0,"Idle");
		public final static State IncomingReceived = new State(1,"IncomingReceived");
		public final static State OutgoingInit = new State(2,"OutgoingInit");
		public final static State OutgoingProgress = new State(3,"OutgoingProgress");
		public final static State OutgoingRinging = new State(4,"OutgoingRinging");
		public final static State OutgoingEarlyMedia = new State(5,"OutgoingEarlyMedia");
		public final static State Connected = new State(6,"Connected");
		public final static State StreamsRunning = new State(7,"StreamsRunning");
		public final static State Pausing = new State(8,"Pausing");
		public final static State Paused = new State(9,"Paused");
		public final static State Resuming = new State(10,"Resuming");
		public final static State Refered = new State(11,"Refered");
		public final static State Error = new State(12,"Error");
		public final static State CallEnd = new State(13,"CallEnd");
		public final static State PausedByRemote = new State(14,"PausedByRemote");
		public static final State CallUpdatedByRemote = new State(15, "CallUpdatedByRemote");
		public static final State CallIncomingEarlyMedia = new State(16,"CallIncomingEarlyMedia");
		public static final State CallUpdated = new State(17, "CallUpdated");
		public static final State CallReleased = new State(18,"CallReleased");

		private State(int value,String stringValue) {
			mValue = value;
			values.addElement(this);
			mStringValue=stringValue;
		}
		public static State fromInt(int value) {
			for (int i=0; i<values.size();i++) {
				State state = (State) values.elementAt(i);
				if (state.mValue == value) return state;
			}
			throw new RuntimeException("state not found ["+value+"]");
		}
		public String toString() {
			return mStringValue;
		}
	}
	public State getState();
	public VoipAddress getRemoteAddress();
	public VoipCallParams getCurrentParamsCopy();
	public void enableCamera(boolean enabled);
	
	public void enableEchoLimiter(boolean enable);
	public int isVideoEmpty();
}
