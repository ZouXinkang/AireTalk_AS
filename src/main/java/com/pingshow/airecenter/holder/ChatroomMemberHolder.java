package com.pingshow.airecenter.holder;

import com.pingshow.airecenter.AireApp;
import com.pingshow.airecenter.MyPreference;
import com.pingshow.airecenter.R;
import com.pingshow.airecenter.bean.ChatroomMember;
import com.pingshow.airecenter.cons.Key;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class ChatroomMemberHolder extends BaseHolder<ChatroomMember> {
	public TextView dispname;
	public ImageView dispimage, mute_members;
	private boolean isHost;
	public ChatroomMemberHolder(){
		isHost = new MyPreference(AireApp.context).readInt(Key.BCAST_CONF, -1) == 1;
	}
	@Override
	public void setView(ChatroomMember model) {
		String displayname = model.getDisplayName();

		if (dispname != null && displayname != null) {
			dispname.setText(displayname);
			dispimage.setImageDrawable(model.getPhoto());
		} else {
			dispname.setText(AireApp.context.getResources().getString(
					R.string.unknown_person)
					+ "!");
			dispimage.setImageDrawable(AireApp.context.getResources()
					.getDrawable(R.drawable.bighead));
		}
		if (mute_members != null) {
			mute_members.setImageResource(model.isSpeak() ? R.drawable.mic_on
					: R.drawable.mic_off);
		}
		if (isHost) {// 主叫方
			mute_members.setVisibility(View.VISIBLE);

		} else {// 被叫方
			mute_members.setVisibility(View.GONE);
		}
	}
}
