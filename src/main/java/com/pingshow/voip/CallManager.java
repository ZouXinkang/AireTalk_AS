
package com.pingshow.voip;

import com.pingshow.airecenter.Log;
import com.pingshow.voip.core.Version;
import com.pingshow.voip.core.VoipAddress;
import com.pingshow.voip.core.VoipCall;
import com.pingshow.voip.core.VoipCallParams;
import com.pingshow.voip.core.VoipCore;
import com.pingshow.voip.core.VoipCoreException;

public class CallManager {

	private static CallManager instance;
	
	private CallManager() {}
	public static final synchronized CallManager getInstance() {
		if (instance == null) instance = new CallManager();
		return instance;
	}
	
	void inviteAddress(VoipAddress lAddress, boolean videoEnabled) throws VoipCoreException {
		VoipCore lc = AireVenus.getLc();
		
		VoipCallParams params = lc.createDefaultCallParameters();
		updateWithProfileSettings(lc, params);

		if (videoEnabled && params.getVideoEnabled()) {
			if (Version.isVideoCapable())
				lc.enableVideo(true, true);
			params.setVideoEnabled(true);
		} else {
			params.setVideoEnabled(false);
		}

		lc.inviteAddressWithParams(lAddress, params);
	}

	
	boolean reinviteWithVideo() {
		VoipCore lc =  AireVenus.getLc();
		VoipCall lCall = lc.getCurrentCall();
		if (lCall == null) {
			Log.e("Trying to reinviteWithVideo while not in call: doing nothing");
			return false;
		}
		VoipCallParams params = lCall.getCurrentParamsCopy();

		if (params.getVideoEnabled()) return false;

		updateWithProfileSettings(lc, params);

		if (!params.getVideoEnabled()) {
			return false;
		}

		lc.updateCall(lCall, params);
		return true;
	}
	void reinvite() {
		VoipCore lc = AireVenus.getLc();
		VoipCall lCall = lc.getCurrentCall();
		if (lCall == null) {
			Log.e("Trying to reinvite while not in call: doing nothing");
			return;
		}
		VoipCallParams params = lCall.getCurrentParamsCopy();
		updateWithProfileSettings(lc, params);
		lc.updateCall(lCall, params);
	}
	public void updateCall() {
		VoipCore lc = AireVenus.getLc();
		VoipCall lCall = lc.getCurrentCall();
		if (lCall == null) {
			Log.e("Trying to updateCall while not in call: doing nothing");
			return;
		}
		VoipCallParams params = lCall.getCurrentParamsCopy();
		updateWithProfileSettings(lc, params);
		lc.updateCall(lCall, null);
	}
	
	public void updateWithProfileSettings(VoipCore lc, VoipCallParams callParams) {
		if (callParams != null) { // in call
			// Update video parm if
			callParams.setVideoEnabled(true);
		}
	}
}

