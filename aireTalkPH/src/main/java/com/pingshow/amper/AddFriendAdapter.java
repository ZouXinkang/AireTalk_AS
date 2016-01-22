package com.pingshow.amper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class AddFriendAdapter extends BaseAdapter{

	final private static List<Map<String, Object>> itemList=new ArrayList<Map<String, Object>>();
	private Context mContext;
	
	final static int [] menu_icon_res={R.drawable.san_addfrends,R.drawable.san_mysan,R.drawable.san_point,R.drawable.san_phone_book,R.drawable.san_friends, R.drawable.san_search_friends};
	final static int [] menu_title_res={R.string.sweep_add,R.string.qrcode,R.string.nearby_friends,R.string.phonebook_search,R.string.possible_friends, R.string.search_friends};
	
	public AddFriendAdapter(Context context)
	{
		mContext=context;
		itemList.clear();
		for (int i=0;i<6;i++)
		{
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("icon", mContext.getResources().getDrawable(menu_icon_res[i]));
			map.put("title", mContext.getResources().getString(menu_title_res[i]));
			itemList.add(map);
		}
	}
	
	@Override
	public int getCount() {
		return itemList.size();
	}

	@Override
	public Object getItem(int arg0) {
		return itemList.get(arg0);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	class ViewHolder {
		ImageView icon;
		TextView title;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Map<String, Object> map=null;
		map = itemList.get(position);
		
		ViewHolder holder;
		
		if (convertView == null) {
			holder = new ViewHolder();
			convertView = View.inflate(mContext, R.layout.menu_item_cell, null);
			
			holder.icon = (ImageView) convertView.findViewById(R.id.icon);
			holder.title = (TextView) convertView.findViewById(R.id.title);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		holder.icon.setImageDrawable((Drawable)map.get("icon"));
		holder.title.setText((String)map.get("title"));
		
		return convertView;
	}
}
	
