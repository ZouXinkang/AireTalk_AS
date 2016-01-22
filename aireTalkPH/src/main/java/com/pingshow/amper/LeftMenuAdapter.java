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

public class LeftMenuAdapter extends BaseAdapter{

	final private static List<Map<String, Object>> itemList=new ArrayList<Map<String, Object>>();
	private Context mContext;
	
	final static int [] menu_icon_res={R.drawable.call_log,R.drawable.ratequery,R.drawable.classes,R.drawable.credit,R.drawable.history, R.drawable.help};
	final static int [] menu_title_res={R.string.call_log,R.string.query_rate,R.string.class_selection,R.string.purchase,R.string.trade_detail, R.string.help};
	
	public LeftMenuAdapter(Context context)
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
