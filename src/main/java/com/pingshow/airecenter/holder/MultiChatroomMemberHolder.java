package com.pingshow.airecenter.holder;

import java.util.List;

import com.pingshow.airecenter.bean.ChatroomMember;

import android.view.View;
import android.widget.Button;

public class MultiChatroomMemberHolder extends BaseHolder<List<ChatroomMember>>{
	public Button openVideo;
	public List<ChatroomMemberHolder> cmhList ;
	
	@Override
	public void setView(List<ChatroomMember> model) {
		if(cmhList != null){
			for (int i = 0 ; i < cmhList.size(); i++) {
				if(model == null){
					break;
				}
				if(i < model.size()){
					cmhList.get(i).contentView.setVisibility(View.VISIBLE);
					cmhList.get(i).setView(model.get(i));
				}else{
					cmhList.get(i).contentView.setVisibility(View.GONE);
				}
			}
		}
	}
}
