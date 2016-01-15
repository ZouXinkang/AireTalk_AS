
package com.pingshow.voip.core;

public interface VoipCoreListener {
		public void show(VoipCore p);
		public void authInfoRequested(VoipCore p,String realm,String username); 
		public void displayStatus(VoipCore p,String message);
		public void displayMessage(VoipCore p,String message);
		public void globalState(VoipCore p,VoipCore.GlobalState state, String message);
		public void callState(VoipCore p, VoipCall call, VoipCall.State cstate,String message);
		public void registrationState(VoipCore p, VoipProxyConfig cfg, VoipCore.RegistrationState cstate, String smessage);
}

