package com.pingshow.airecenter.adapter;

import java.util.List;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.pingshow.airecenter.AireApp;
import com.pingshow.airecenter.MyPreference;
import com.pingshow.airecenter.R;
import com.pingshow.airecenter.bean.ChatroomMember;
import com.pingshow.airecenter.cons.Key;
import com.pingshow.airecenter.holder.ChatroomMemberHolder;

public class MemberAdapter extends ListAdapter<ChatroomMember,ChatroomMemberHolder>{


	private boolean isHost;

	public MemberAdapter(List<ChatroomMember> list) {
		super(list);
		isHost = new MyPreference(AireApp.context).readInt(Key.BCAST_CONF, -1) == 1;
	}

	@Override
	public ChatroomMemberHolder initHolder() {
		ChatroomMemberHolder holder = new ChatroomMemberHolder();
		holder.contentView = View.inflate(AireApp.context, R.layout.gallery_items3,
				null);
		holder.mute_members = (ImageView) holder.contentView
				.findViewById(R.id.mute_members);
		holder.dispimage = (ImageView) holder.contentView
				.findViewById(R.id.gallery_imageview);
		holder.dispname = (TextView) holder.contentView.findViewById(R.id.displayname0);
		
		return holder;
	}
	
}
