package com.pingshow.airecenter.adapter;

import java.util.ArrayList;
import java.util.List;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.pingshow.airecenter.AireApp;
import com.pingshow.airecenter.Log;
import com.pingshow.airecenter.R;
import com.pingshow.airecenter.bean.ChatroomMember;
import com.pingshow.airecenter.holder.ChatroomMemberHolder;
import com.pingshow.airecenter.holder.MultiChatroomMemberHolder;
import com.pingshow.voip.AireVenus;
import com.pingshow.voip.DialerActivity;

public class MultiMemberAdapter extends ListAdapter<List<ChatroomMember>,MultiChatroomMemberHolder> implements OnClickListener{

	private int multiNum;

	public MultiMemberAdapter(List<List<ChatroomMember>> list,int multiNum) {
		super(list);
		this.multiNum = multiNum;
	}

	@Override
	public MultiChatroomMemberHolder initHolder() {
		
		MultiChatroomMemberHolder mcmh = new MultiChatroomMemberHolder();
		List<ChatroomMemberHolder> cmhs = new ArrayList<ChatroomMemberHolder>();
		mcmh.cmhList = cmhs;
		mcmh.contentView = View.inflate(AireApp.context, R.layout.item_conference_member, null);
		mcmh.openVideo = (Button) mcmh.contentView.findViewById(R.id.btn_open_video);
		mcmh.openVideo.setOnClickListener(this);
		LinearLayout  ll_members = (LinearLayout) mcmh.contentView.findViewById(R.id.ll_members);
		for (int i = 0; i < multiNum; i++) {
			ChatroomMemberHolder memberHolder = new ChatroomMemberHolder();
			memberHolder.contentView = View.inflate(AireApp.context, R.layout.item_member, null);
			memberHolder.dispimage = (ImageView) memberHolder.contentView.findViewById(R.id.iv_profile);
			memberHolder.mute_members = (ImageView) memberHolder.contentView.findViewById(R.id.iv_mute);
			memberHolder.dispname = (TextView) memberHolder.contentView.findViewById(R.id.tv_name);
			mcmh.cmhList.add(memberHolder);
			ll_members.addView(memberHolder.contentView);
		}
		return mcmh;
	}

	@Override
	public void onClick(View v) {
		Integer position = (Integer) v.getTag();
		List<ChatroomMember> cms = list.get(position);
		Log.d("点击啦！position： " + position);
		if (DialerActivity.getDialer() != null
				&& AireVenus.getCallType() == AireVenus.CALLTYPE_CHATROOM) {
			DialerActivity.getDialer().notifyOpenVideo(position);
		}
	}

	@Override
	public void handleView(int position, MultiChatroomMemberHolder holder) {
		holder.openVideo.setTag(position);
	}

}
